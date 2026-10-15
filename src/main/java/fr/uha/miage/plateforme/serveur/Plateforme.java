package fr.uha.miage.plateforme.serveur;

import fr.uha.miage.plateforme.protocole.CodeErreur;
import fr.uha.miage.plateforme.protocole.Message;
import fr.uha.miage.plateforme.protocole.Verbes;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Cœur du serveur, indépendant du réseau : présence, invitations et parties.
 * Les transitions d'état des joueurs sont protégées par un verrou unique ; on n'y fait jamais d'entrée-sortie
 * (les messages sont seulement placés dans les files d'envoi des joueurs).
 */
public final class Plateforme implements AutoCloseable {

    private static final System.Logger LOG = System.getLogger(Plateforme.class.getName());
    private static final Pattern PSEUDO = Pattern.compile("[A-Za-z0-9_-]{1,20}");

    public record Parametres(Duration delaiInvitation, Duration intervalleHeartbeat, Duration timeoutHeartbeat,
                             Duration intervalleVerification) {

        public static Parametres parDefaut() {
            return new Parametres(Duration.ofSeconds(30), Duration.ofSeconds(5), Duration.ofSeconds(20),
                    Duration.ofSeconds(5));
        }
    }

    private final CatalogueJeux catalogue;
    private final Parametres parametres;
    private final ReentrantLock verrou = new ReentrantLock();
    private final Map<String, Joueur> joueurs = new HashMap<>();
    private final Map<String, Joueur> parJeton = new ConcurrentHashMap<>();
    private final ScheduledThreadPoolExecutor planificateur;
    private final SecureRandom aleatoire = new SecureRandom();

    public Plateforme(CatalogueJeux catalogue, Parametres parametres) {
        this.catalogue = catalogue;
        this.parametres = parametres;
        this.planificateur = new ScheduledThreadPoolExecutor(1, tache -> {
            Thread fil = new Thread(tache, "plateforme-planificateur");
            fil.setDaemon(true);
            return fil;
        });
        this.planificateur.setRemoveOnCancelPolicy(true);
        long periode = parametres.intervalleVerification().toMillis();
        this.planificateur.scheduleWithFixedDelay(this::verifierPresences, periode, periode, TimeUnit.MILLISECONDS);
    }

    public Parametres parametres() {
        return parametres;
    }

    public CatalogueJeux catalogue() {
        return catalogue;
    }

    // ---------------------------------------------------------------- présence

    /**
     * Enregistre un joueur. Le message d'accueil est placé dans sa file avant toute notification de présence,
     * afin qu'il soit toujours le premier message reçu.
     */
    public Joueur connecter(String pseudo, Set<String> jeuxClient, CanalJoueur canal,
                            Function<Joueur, Message> accueil) throws ErreurPlateforme {
        if (!PSEUDO.matcher(pseudo).matches()) {
            throw new ErreurPlateforme(CodeErreur.PSEUDO_INVALIDE,
                    "Pseudo invalide : utilisez 1 à 20 caractères parmi lettres, chiffres, « _ » et « - ».");
        }
        Set<String> jeuxCommuns = new TreeSet<>(jeuxClient);
        jeuxCommuns.retainAll(catalogue.ids());
        verrou.lock();
        try {
            String cle = cle(pseudo);
            if (joueurs.containsKey(cle)) {
                throw new ErreurPlateforme(CodeErreur.PSEUDO_PRIS, "Le pseudo « " + pseudo + " » est déjà utilisé.");
            }
            Joueur joueur = new Joueur(pseudo, jeuxCommuns, nouveauJeton(), canal);
            canal.envoyer(accueil.apply(joueur));
            joueurs.put(cle, joueur);
            parJeton.put(joueur.jeton(), joueur);
            diffuserPresence(joueur);
            return joueur;
        } finally {
            verrou.unlock();
        }
    }

    /** Retire un joueur de la plateforme ; sans effet si c'est déjà fait. */
    public void deconnecter(Joueur joueur, String raison) {
        verrou.lock();
        try {
            if (!joueur.marquerDeconnecte()) {
                return;
            }
            joueurs.remove(cle(joueur.pseudo()));
            parJeton.remove(joueur.jeton());
            Message depart = Message.of(Verbes.DEPART, joueur.pseudo(), raison);
            joueurs.values().forEach(autre -> autre.envoyer(depart));
        } finally {
            verrou.unlock();
        }
        joueur.canal().fermer();
    }

    public void definirDisponibilite(Joueur joueur, boolean disponible) throws ErreurPlateforme {
        verrou.lock();
        try {
            verifierConnecte(joueur);
            if (joueur.etat() == EtatJoueur.EN_PARTIE) {
                throw new ErreurPlateforme(CodeErreur.ETAT_INCOMPATIBLE,
                        "Vous ne pouvez pas changer de disponibilité pendant une partie.");
            }
            EtatJoueur nouvelEtat = disponible ? EtatJoueur.DISPONIBLE : EtatJoueur.CONNECTE;
            if (nouvelEtat != joueur.etat()) {
                joueur.definirEtat(nouvelEtat);
                diffuserPresence(joueur);
            }
        } finally {
            verrou.unlock();
        }
    }

    public List<InfoJoueur> listerJoueurs() {
        verrou.lock();
        try {
            return joueurs.values().stream()
                    .map(Joueur::info)
                    .sorted(Comparator.comparing(InfoJoueur::pseudo, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } finally {
            verrou.unlock();
        }
    }

    /** Appelé par le service UDP : accessible sans le verrou principal. */
    public Optional<Joueur> enregistrerSigneUdp(String jeton) {
        Joueur joueur = parJeton.get(jeton);
        if (joueur == null) {
            return Optional.empty();
        }
        joueur.signalerSigneUdp();
        return Optional.of(joueur);
    }

    void verifierPresences() {
        long limite = parametres.timeoutHeartbeat().toNanos();
        for (Joueur joueur : new ArrayList<>(parJeton.values())) {
            if (joueur.estSilencieuxDepuis(limite)) {
                LOG.log(System.Logger.Level.INFO, "{0} ne donne plus signe de vie (UDP) : déconnexion", joueur.pseudo());
                deconnecter(joueur, "SILENCE");
            }
        }
    }

    @Override
    public void close() {
        planificateur.shutdownNow();
    }

    // ---------------------------------------------------------------- utilitaires

    static String cle(String pseudo) {
        return pseudo.toLowerCase(Locale.ROOT);
    }

    private String nouveauJeton() {
        byte[] octets = new byte[16];
        aleatoire.nextBytes(octets);
        return HexFormat.of().formatHex(octets);
    }

    private void verifierConnecte(Joueur joueur) throws ErreurPlateforme {
        if (!joueur.estConnecte()) {
            throw new ErreurPlateforme(CodeErreur.NON_CONNECTE, "Vous n'êtes plus connecté à la plateforme.");
        }
    }

    private void diffuserPresence(Joueur joueur) {
        Message message = Message.of(Verbes.PRESENCE, joueur.pseudo(), joueur.etat(), String.join(",", joueur.jeux().stream().sorted().toList()));
        joueurs.values().forEach(destinataire -> destinataire.envoyer(message));
    }
}
