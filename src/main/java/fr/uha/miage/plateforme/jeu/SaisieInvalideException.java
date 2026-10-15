package fr.uha.miage.plateforme.jeu;

/** Saisie du joueur incompréhensible pour la présentation du jeu ; le message est affiché tel quel. */
public class SaisieInvalideException extends Exception {

    public SaisieInvalideException(String message) {
        super(message);
    }
}
