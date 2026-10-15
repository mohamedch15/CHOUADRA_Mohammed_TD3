package fr.uha.miage.plateforme.jeu;

/** Action refusée par les règles ; le message est affiché tel quel au joueur. */
public class ActionInvalideException extends Exception {

    public ActionInvalideException(String message) {
        super(message);
    }
}
