package fr.uha.miage.plateforme.jeu;

import java.util.List;

/** Côté client : traduit les saisies en actions et les vues en texte. Ne connaît ni le réseau ni le serveur. */
public interface PresentationJeu {

    /** Identifiant du jeu présenté, identique à Jeu.id(). */
    String idJeu();

    String nomJeu();

    String regles();

    /** Rappel des saisies possibles, affiché quand c'est au joueur de jouer. */
    String aide();

    /** Convertit ce que tape le joueur en action transmise au serveur. */
    String actionDepuisSaisie(String saisie) throws SaisieInvalideException;

    String afficher(String moi, List<String> joueurs, Vue vue);
}
