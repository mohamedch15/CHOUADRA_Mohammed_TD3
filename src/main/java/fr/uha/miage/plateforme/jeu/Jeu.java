package fr.uha.miage.plateforme.jeu;

import java.util.List;

/** Côté serveur : décrit un type de jeu et fabrique ses parties. */
public interface Jeu {

    /** Identifiant stable, en minuscules, chiffres et tirets (ex. « morpion »). */
    String id();

    String nom();

    int nombreJoueurs();

    /** Crée une partie pour les pseudos donnés, dans l'ordre d'invitation (l'invitant en premier). */
    Partie creerPartie(List<String> joueurs);
}
