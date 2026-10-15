package fr.uha.miage.plateforme.serveur;

import fr.uha.miage.plateforme.jeu.Jeu;
import fr.uha.miage.plateforme.jeu.Partie;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/** Une partie arbitrée par la plateforme ; son verrou sérialise les actions des participants. */
final class PartieEnCours {

    private final String id;
    private final Jeu jeu;
    private final Partie partie;
    private final List<Joueur> participants;
    private final ReentrantLock verrou = new ReentrantLock();
    private final AtomicBoolean terminee = new AtomicBoolean();

    PartieEnCours(String id, Jeu jeu, Partie partie, List<Joueur> participants) {
        this.id = id;
        this.jeu = jeu;
        this.partie = partie;
        this.participants = List.copyOf(participants);
    }

    String id() {
        return id;
    }

    Jeu jeu() {
        return jeu;
    }

    Partie partie() {
        return partie;
    }

    List<Joueur> participants() {
        return participants;
    }

    ReentrantLock verrou() {
        return verrou;
    }

    boolean estTerminee() {
        return terminee.get();
    }

    /** Vrai pour le seul appelant qui clôt la partie. */
    boolean marquerTerminee() {
        return terminee.compareAndSet(false, true);
    }
}
