package fr.uha.miage.plateforme.protocole;

/** Verbes du protocole applicatif et constantes associées. */
public final class Verbes {

    public static final int VERSION_PROTOCOLE = 1;

    // Client -> serveur (TCP)
    public static final String CONNEXION = "CONNEXION";
    public static final String DISPONIBLE = "DISPONIBLE";
    public static final String INDISPONIBLE = "INDISPONIBLE";
    public static final String JOUEURS = "JOUEURS";
    public static final String INVITER = "INVITER";
    public static final String ACCEPTER = "ACCEPTER";
    public static final String REFUSER = "REFUSER";
    public static final String ANNULER = "ANNULER";
    public static final String ACTION = "ACTION";
    public static final String ABANDON = "ABANDON";
    public static final String QUITTER = "QUITTER";

    // Serveur -> client (TCP) : réponses
    public static final String BIENVENUE = "BIENVENUE";
    public static final String OK = "OK";
    public static final String ERREUR = "ERREUR";
    public static final String AU_REVOIR = "AU_REVOIR";

    // Serveur -> client (TCP) : événements
    public static final String PRESENCE = "PRESENCE";
    public static final String DEPART = "DEPART";
    public static final String INVITATION = "INVITATION";
    public static final String INVITATION_RESULTAT = "INVITATION_RESULTAT";
    public static final String PARTIE_DEBUT = "PARTIE_DEBUT";
    public static final String VUE = "VUE";
    public static final String FIN_PARTIE = "FIN_PARTIE";

    // UDP
    public static final String DECOUVERTE = "DECOUVERTE";
    public static final String SERVEUR = "SERVEUR";
    public static final String HEARTBEAT = "HEARTBEAT";
    public static final String HEARTBEAT_ACK = "HEARTBEAT_ACK";

    public static final String CROISEE = "CROISEE";

    private Verbes() {
    }
}
