package fr.uha.miage.plateforme.serveur;

import fr.uha.miage.plateforme.jeu.Jeu;
import fr.uha.miage.plateforme.jeu.Partie;
import fr.uha.miage.plateforme.jeu.Vue;
import fr.uha.miage.plateforme.protocole.CodeErreur;
import fr.uha.miage.plateforme.protocole.Message;
import fr.uha.miage.plateforme.protocole.Verbes;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
    private final Map<String, Invitation> invitations = new HashMap<>();
    private final Map<String, String> invitationsTerminees = historiqueBorne();
    private final Map<String, PartieEnCours> parties = new ConcurrentHashMap<>();
    private final AtomicLong compteurInvitations = new AtomicLong();
    private final AtomicLong compteurParties = new AtomicLong();
    private final ScheduledThreadPoolExecutor planificateur;
    private final SecureRandom aleatoire = new SecureRandom();

    /** Issue d'une demande d'invitation : soit une invitation en attente, soit une partie déjà lancée. */
    public record ResultatInvitation(String idInvitation, boolean partieDemarree) {
    }

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
            annulerInvitationsDe(joueur, joueur.pseudo() + " a quitté la plateforme.", joueur);
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
                if (!disponible) {
                    annulerInvitationsDe(joueur, joueur.pseudo() + " n'est plus disponible.", joueur);
                }
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

    // ---------------------------------------------------------------- invitations

    /**
     * Politique : un joueur disponible a au plus une invitation sortante en attente et peut en recevoir plusieurs.
     * Dès qu'une partie démarre, toutes les autres invitations de ses deux participants sont invalidées.
     * Deux invitations croisées pour le même jeu valent accord mutuel et lancent la partie.
     */
    public ResultatInvitation inviter(Joueur de, String pseudoCible, String idJeu) throws ErreurPlateforme {
        verrou.lock();
        try {
            verifierConnecte(de);
            exigerDisponible(de);
            Joueur cible = joueurs.get(cle(pseudoCible));
            if (cible == null) {
                throw new ErreurPlateforme(CodeErreur.JOUEUR_INCONNU, "Aucun joueur nommé « " + pseudoCible + " ».");
            }
            if (cible == de) {
                throw new ErreurPlateforme(CodeErreur.AUTO_INVITATION, "Vous ne pouvez pas vous inviter vous-même.");
            }
            Jeu jeu = catalogue.trouver(idJeu).orElseThrow(() -> new ErreurPlateforme(CodeErreur.JEU_INCONNU,
                    "Jeu inconnu : « " + idJeu + " »."));
            if (jeu.nombreJoueurs() != 2) {
                throw new ErreurPlateforme(CodeErreur.JEU_NON_JOUABLE,
                        jeu.nom() + " se joue à " + jeu.nombreJoueurs() + " : les invitations à deux ne conviennent pas.");
            }
            if (!de.jeux().contains(idJeu)) {
                throw new ErreurPlateforme(CodeErreur.JEU_NON_SUPPORTE,
                        "Votre client ne propose pas le jeu « " + idJeu + " ».");
            }
            if (!cible.jeux().contains(idJeu)) {
                throw new ErreurPlateforme(CodeErreur.JEU_NON_SUPPORTE,
                        cible.pseudo() + " ne peut pas jouer à « " + idJeu + " » (jeu absent de son client).");
            }
            if (cible.etat() != EtatJoueur.DISPONIBLE) {
                throw new ErreurPlateforme(CodeErreur.JOUEUR_INDISPONIBLE, cible.pseudo() + " n'est pas disponible.");
            }
            Invitation croisee = cible.invitationSortante();
            if (croisee != null && croisee.invite() == de && croisee.jeu().id().equals(idJeu)) {
                demarrer(croisee);
                return new ResultatInvitation(croisee.id(), true);
            }
            if (de.invitationSortante() != null) {
                throw new ErreurPlateforme(CodeErreur.INVITATION_EN_COURS,
                        "Vous avez déjà une invitation en attente : annulez-la d'abord.");
            }
            Invitation invitation = new Invitation("i" + compteurInvitations.incrementAndGet(), de, cible, jeu);
            invitations.put(invitation.id(), invitation);
            de.definirInvitationSortante(invitation);
            cible.invitationsEntrantes().add(invitation);
            long delai = parametres.delaiInvitation().toMillis();
            invitation.programmerExpiration(
                    planificateur.schedule(() -> expirer(invitation.id()), delai, TimeUnit.MILLISECONDS));
            cible.envoyer(Message.of(Verbes.INVITATION, invitation.id(), de.pseudo(), jeu.id(),
                    parametres.delaiInvitation().toSeconds()));
            return new ResultatInvitation(invitation.id(), false);
        } finally {
            verrou.unlock();
        }
    }

    public void accepter(Joueur joueur, String idInvitation) throws ErreurPlateforme {
        verrou.lock();
        try {
            verifierConnecte(joueur);
            Invitation invitation = invitationEnAttente(idInvitation);
            if (invitation.invite() != joueur) {
                throw new ErreurPlateforme(CodeErreur.NON_AUTORISE, "Cette invitation ne vous est pas destinée.");
            }
            if (joueur.etat() != EtatJoueur.DISPONIBLE) {
                throw new ErreurPlateforme(CodeErreur.ETAT_INCOMPATIBLE,
                        "Vous devez être disponible pour accepter une invitation.");
            }
            demarrer(invitation);
        } finally {
            verrou.unlock();
        }
    }

    public void refuser(Joueur joueur, String idInvitation) throws ErreurPlateforme {
        verrou.lock();
        try {
            verifierConnecte(joueur);
            Invitation invitation = invitationEnAttente(idInvitation);
            if (invitation.invite() != joueur) {
                throw new ErreurPlateforme(CodeErreur.NON_AUTORISE, "Cette invitation ne vous est pas destinée.");
            }
            resoudre(invitation, Invitation.Etat.REFUSEE, joueur.pseudo() + " a refusé votre invitation.", joueur);
        } finally {
            verrou.unlock();
        }
    }

    public void annuler(Joueur joueur, String idInvitation) throws ErreurPlateforme {
        verrou.lock();
        try {
            verifierConnecte(joueur);
            Invitation invitation = invitationEnAttente(idInvitation);
            if (invitation.invitant() != joueur) {
                throw new ErreurPlateforme(CodeErreur.NON_AUTORISE, "Vous n'êtes pas l'auteur de cette invitation.");
            }
            resoudre(invitation, Invitation.Etat.ANNULEE, joueur.pseudo() + " a annulé son invitation.", joueur);
        } finally {
            verrou.unlock();
        }
    }

    private void expirer(String idInvitation) {
        verrou.lock();
        try {
            Invitation invitation = invitations.get(idInvitation);
            if (invitation != null) {
                resoudre(invitation, Invitation.Etat.EXPIREE, "L'invitation de " + invitation.invitant().pseudo()
                        + " à " + invitation.invite().pseudo() + " a expiré sans réponse.", null);
            }
        } finally {
            verrou.unlock();
        }
    }

    private Invitation invitationEnAttente(String idInvitation) throws ErreurPlateforme {
        Invitation invitation = invitations.get(idInvitation);
        if (invitation != null) {
            return invitation;
        }
        String issue = invitationsTerminees.get(idInvitation);
        throw new ErreurPlateforme(CodeErreur.INVITATION_INVALIDE, issue == null
                ? "Invitation inconnue : « " + idInvitation + " »."
                : "L'invitation " + idInvitation + " n'est plus valide : elle est " + issue + ".");
    }

    private void annulerInvitationsDe(Joueur joueur, String raison, Joueur acteur) {
        for (Invitation invitation : joueur.invitationsConcernees()) {
            resoudre(invitation, Invitation.Etat.INVALIDEE, raison, acteur);
        }
    }

    /** Clôt une invitation et prévient ses participants, sauf l'acteur qui en est à l'origine. */
    private void resoudre(Invitation invitation, Invitation.Etat etat, String raison, Joueur acteur) {
        invitation.annulerExpiration();
        invitations.remove(invitation.id());
        invitationsTerminees.put(invitation.id(), etat.libelle());
        invitation.invitant().definirInvitationSortante(null);
        invitation.invite().invitationsEntrantes().remove(invitation);
        Message message = Message.of(Verbes.INVITATION_RESULTAT, invitation.id(), etat, raison);
        for (Joueur participant : List.of(invitation.invitant(), invitation.invite())) {
            if (participant != acteur) {
                participant.envoyer(message);
            }
        }
    }

    private void exigerDisponible(Joueur joueur) throws ErreurPlateforme {
        if (joueur.etat() == EtatJoueur.EN_PARTIE) {
            throw new ErreurPlateforme(CodeErreur.ETAT_INCOMPATIBLE, "Vous êtes déjà en partie.");
        }
        if (joueur.etat() != EtatJoueur.DISPONIBLE) {
            throw new ErreurPlateforme(CodeErreur.ETAT_INCOMPATIBLE,
                    "Déclarez-vous disponible (commande DISPONIBLE) avant d'inviter.");
        }
    }

    // ---------------------------------------------------------------- démarrage des parties

    /** Atomique sous le verrou : les deux joueurs passent EN_PARTIE et toutes leurs autres invitations tombent. */
    private void demarrer(Invitation invitation) throws ErreurPlateforme {
        Joueur invitant = invitation.invitant();
        Joueur invite = invitation.invite();
        if (invitant.etat() != EtatJoueur.DISPONIBLE || !invitant.estConnecte()) {
            resoudre(invitation, Invitation.Etat.INVALIDEE, invitant.pseudo() + " n'est plus disponible.", null);
            throw new ErreurPlateforme(CodeErreur.INVITATION_INVALIDE,
                    "L'invitation n'est plus valide : " + invitant.pseudo() + " n'est plus disponible.");
        }
        Partie partie;
        try {
            partie = invitation.jeu().creerPartie(List.of(invitant.pseudo(), invite.pseudo()));
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "Création de partie impossible pour " + invitation.jeu().id(), e);
            resoudre(invitation, Invitation.Etat.INVALIDEE, "Le jeu n'a pas pu créer la partie.", null);
            throw new ErreurPlateforme(CodeErreur.JEU_NON_JOUABLE, "Le jeu n'a pas pu créer la partie.");
        }
        resoudre(invitation, Invitation.Etat.ACCEPTEE, invite.pseudo() + " a accepté votre invitation.", invite);
        for (Joueur participant : List.of(invitant, invite)) {
            annulerInvitationsDe(participant, participant.pseudo() + " est engagé dans une autre partie.", null);
        }
        PartieEnCours enCours = new PartieEnCours("p" + compteurParties.incrementAndGet(), invitation.jeu(), partie,
                List.of(invitant, invite));
        parties.put(enCours.id(), enCours);
        enCours.verrou().lock();
        try {
            for (Joueur participant : enCours.participants()) {
                participant.definirPartie(enCours);
                participant.definirEtat(EtatJoueur.EN_PARTIE);
                participant.envoyer(Message.of(Verbes.PARTIE_DEBUT, enCours.id(), enCours.jeu().id(),
                        String.join(",", partie.joueurs())));
                participant.envoyer(messageVue(enCours, participant));
            }
        } finally {
            enCours.verrou().unlock();
        }
        diffuserPresence(invitant);
        diffuserPresence(invite);
    }

    private Message messageVue(PartieEnCours enCours, Joueur joueur) {
        Vue vue = enCours.partie().vue(joueur.pseudo());
        return Message.of(Verbes.VUE, enCours.id(), vue.actionAttendue(), vue.donnees());
    }

    public int nombrePartiesEnCours() {
        return parties.size();
    }

    // ---------------------------------------------------------------- utilitaires

    private static Map<String, String> historiqueBorne() {
        return new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > 200;
            }
        };
    }

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
