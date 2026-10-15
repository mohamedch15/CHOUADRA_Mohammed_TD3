package fr.uha.miage.plateforme.jeux.morpion;

import fr.uha.miage.plateforme.jeu.ActionInvalideException;
import fr.uha.miage.plateforme.jeu.Donnees;
import fr.uha.miage.plateforme.jeu.PartieBase;
import fr.uha.miage.plateforme.jeu.ResultatPartie;
import fr.uha.miage.plateforme.jeu.Vue;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Actions alternées : le premier joueur (X) commence, les cases se désignent par colonne A-C et ligne 1-3. */
final class MorpionPartie extends PartieBase {

    private static final Pattern CASE = Pattern.compile("([A-C])([1-3])");
    private static final int[][] LIGNES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}};

    private final char[] grille = new char[9];
    private char tour = 'X';
    private String dernierCoup = "";

    MorpionPartie(List<String> joueurs) {
        super(joueurs);
        Arrays.fill(grille, '.');
    }

    private char symbole(String joueur) {
        return joueurs().get(0).equals(joueur) ? 'X' : 'O';
    }

    @Override
    protected void appliquer(String joueur, String action) throws ActionInvalideException {
        Matcher m = CASE.matcher(action.trim().toUpperCase(Locale.ROOT));
        if (!m.matches()) {
            throw new ActionInvalideException("Case inconnue : « " + action + " ». Exemples valides : A1, B2, C3.");
        }
        char symbole = symbole(joueur);
        if (symbole != tour) {
            throw new ActionInvalideException("Ce n'est pas votre tour.");
        }
        int index = (m.group(2).charAt(0) - '1') * 3 + (m.group(1).charAt(0) - 'A');
        if (grille[index] != '.') {
            throw new ActionInvalideException("La case " + m.group(0) + " est déjà occupée.");
        }
        grille[index] = symbole;
        dernierCoup = m.group(0);
        if (aGagne(symbole)) {
            terminer(ResultatPartie.victoire(joueurs(), joueur, joueur + " a aligné trois " + symbole + "."));
        } else if (new String(grille).indexOf('.') < 0) {
            terminer(ResultatPartie.nul(joueurs(), "Grille pleine : match nul."));
        } else {
            tour = tour == 'X' ? 'O' : 'X';
        }
    }

    private boolean aGagne(char symbole) {
        for (int[] ligne : LIGNES) {
            if (grille[ligne[0]] == symbole && grille[ligne[1]] == symbole && grille[ligne[2]] == symbole) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Vue vue(String joueur) {
        if (!joueurs().contains(joueur)) {
            throw new IllegalArgumentException("Joueur inconnu : " + joueur);
        }
        char monSymbole = symbole(joueur);
        String donnees = Donnees.encoder(
                "grille", new String(grille),
                "symbole", String.valueOf(monSymbole),
                "tour", estTerminee() ? "" : String.valueOf(tour),
                "joueurX", joueurs().get(0),
                "joueurO", joueurs().get(1),
                "dernierCoup", dernierCoup);
        return new Vue(!estTerminee() && monSymbole == tour, donnees);
    }
}
