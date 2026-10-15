package fr.uha.miage.plateforme.serveur;

public enum EtatJoueur {
    /** Présent sur la plateforme mais ne souhaite pas jouer. */
    CONNECTE,
    /** Prêt à recevoir et à envoyer des invitations. */
    DISPONIBLE,
    /** Engagé dans une partie. */
    EN_PARTIE
}
