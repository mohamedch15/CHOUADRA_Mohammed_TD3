package fr.uha.miage.plateforme.serveur;

import fr.uha.miage.plateforme.protocole.Message;

/** Moyen d'atteindre un joueur ; indépendant du réseau pour pouvoir tester la plateforme seule. */
public interface CanalJoueur {

    /** Ne bloque jamais ; retourne false si le message n'a pas pu être mis en file (canal fermé ou saturé). */
    boolean envoyer(Message message);

    void fermer();
}
