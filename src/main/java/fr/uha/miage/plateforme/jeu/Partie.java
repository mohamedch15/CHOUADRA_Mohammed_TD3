package fr.uha.miage.plateforme.jeu;

import java.util.List;
import java.util.Optional;

/**
 * Côté serveur : état de référence d'une partie. Une instance n'est pas thread-safe,
 * la plateforme sérialise tous les accès à une même partie.
 */
public interface Partie {

    List<String> joueurs();

    /** Applique l'action d'un joueur ; lève ActionInvalideException sans modifier l'état si elle est refusée. */
    void jouer(String joueur, String action) throws ActionInvalideException;

    /** Ce que le joueur a le droit de voir ; reste valable après la fin de la partie. */
    Vue vue(String joueur);

    /** Vide tant que la partie est en cours. */
    Optional<ResultatPartie> resultat();

    /** Termine la partie par forfait du joueur et retourne le résultat. */
    ResultatPartie abandonner(String joueur);
}
