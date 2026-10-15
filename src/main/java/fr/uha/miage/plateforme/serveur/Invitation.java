package fr.uha.miage.plateforme.serveur;

import fr.uha.miage.plateforme.jeu.Jeu;
import java.util.concurrent.ScheduledFuture;

/** Invitation en attente d'une réponse. Modifiée uniquement sous le verrou de la Plateforme. */
final class Invitation {

    enum Etat {
        EN_ATTENTE("en attente"),
        ACCEPTEE("acceptée"),
        REFUSEE("refusée"),
        ANNULEE("annulée"),
        EXPIREE("expirée"),
        INVALIDEE("devenue invalide");

        private final String libelle;

        Etat(String libelle) {
            this.libelle = libelle;
        }

        String libelle() {
            return libelle;
        }
    }

    private final String id;
    private final Joueur invitant;
    private final Joueur invite;
    private final Jeu jeu;
    private ScheduledFuture<?> expiration;

    Invitation(String id, Joueur invitant, Joueur invite, Jeu jeu) {
        this.id = id;
        this.invitant = invitant;
        this.invite = invite;
        this.jeu = jeu;
    }

    String id() {
        return id;
    }

    Joueur invitant() {
        return invitant;
    }

    Joueur invite() {
        return invite;
    }

    Jeu jeu() {
        return jeu;
    }

    void programmerExpiration(ScheduledFuture<?> expiration) {
        this.expiration = expiration;
    }

    void annulerExpiration() {
        if (expiration != null) {
            expiration.cancel(false);
        }
    }
}
