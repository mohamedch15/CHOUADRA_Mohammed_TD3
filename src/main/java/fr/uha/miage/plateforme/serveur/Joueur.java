package fr.uha.miage.plateforme.serveur;

import fr.uha.miage.plateforme.protocole.Message;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

/** Un joueur connecté. Les champs modifiables ne changent que sous le verrou de la Plateforme. */
public final class Joueur {

    private final String pseudo;
    private final Set<String> jeux;
    private final String jeton;
    private final CanalJoueur canal;
    private final AtomicLong dernierSigneNanos = new AtomicLong(System.nanoTime());
    private volatile boolean udpActif;
    private volatile boolean connecte = true;
    private volatile EtatJoueur etat = EtatJoueur.CONNECTE;
    private Invitation invitationSortante;
    private final Set<Invitation> invitationsEntrantes = new LinkedHashSet<>();
    private PartieEnCours partie;

    Joueur(String pseudo, Set<String> jeux, String jeton, CanalJoueur canal) {
        this.pseudo = pseudo;
        this.jeux = Set.copyOf(new TreeSet<>(jeux));
        this.jeton = jeton;
        this.canal = canal;
    }

    public String pseudo() {
        return pseudo;
    }

    public Set<String> jeux() {
        return jeux;
    }

    public String jeton() {
        return jeton;
    }

    public EtatJoueur etat() {
        return etat;
    }

    public boolean estConnecte() {
        return connecte;
    }

    public boolean envoyer(Message message) {
        return connecte && canal.envoyer(message);
    }

    CanalJoueur canal() {
        return canal;
    }

    void definirEtat(EtatJoueur nouvelEtat) {
        this.etat = nouvelEtat;
    }

    boolean marquerDeconnecte() {
        boolean etaitConnecte = connecte;
        connecte = false;
        return etaitConnecte;
    }

    /** Toute activité (message TCP ou signe UDP) repousse l'échéance de détection de silence. */
    public void signalerActivite() {
        dernierSigneNanos.set(System.nanoTime());
    }

    void signalerSigneUdp() {
        udpActif = true;
        signalerActivite();
    }

    boolean estSilencieuxDepuis(long dureeNanos) {
        return udpActif && System.nanoTime() - dernierSigneNanos.get() > dureeNanos;
    }

    Invitation invitationSortante() {
        return invitationSortante;
    }

    void definirInvitationSortante(Invitation invitation) {
        this.invitationSortante = invitation;
    }

    Set<Invitation> invitationsEntrantes() {
        return invitationsEntrantes;
    }

    List<Invitation> invitationsConcernees() {
        List<Invitation> toutes = new ArrayList<>(invitationsEntrantes);
        if (invitationSortante != null) {
            toutes.add(invitationSortante);
        }
        return toutes;
    }

    PartieEnCours partie() {
        return partie;
    }

    void definirPartie(PartieEnCours partie) {
        this.partie = partie;
    }

    InfoJoueur info() {
        return new InfoJoueur(pseudo, etat, jeux);
    }
}
