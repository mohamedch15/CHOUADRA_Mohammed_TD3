package fr.uha.miage.plateforme.jeux.morpion;

import fr.uha.miage.plateforme.jeu.Donnees;
import fr.uha.miage.plateforme.jeu.PresentationJeu;
import fr.uha.miage.plateforme.jeu.SaisieInvalideException;
import fr.uha.miage.plateforme.jeu.Vue;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MorpionPresentation implements PresentationJeu {

    private static final Pattern COLONNE_LIGNE = Pattern.compile("([A-C])\\s*([1-3])");
    private static final Pattern LIGNE_COLONNE = Pattern.compile("([1-3])\\s*([A-C])");

    @Override
    public String idJeu() {
        return MorpionJeu.ID;
    }

    @Override
    public String nomJeu() {
        return "Morpion";
    }

    @Override
    public String regles() {
        return "Grille 3x3, les joueurs placent à tour de rôle leur symbole (X commence : c'est l'invitant).\n"
                + "Le premier qui aligne trois symboles (ligne, colonne ou diagonale) gagne ; grille pleine : match nul.";
    }

    @Override
    public String aide() {
        return "Désignez une case par sa colonne (A-C) et sa ligne (1-3), par exemple : b2.";
    }

    @Override
    public String actionDepuisSaisie(String saisie) throws SaisieInvalideException {
        String texte = saisie.trim().toUpperCase(Locale.ROOT);
        Matcher cl = COLONNE_LIGNE.matcher(texte);
        if (cl.matches()) {
            return cl.group(1) + cl.group(2);
        }
        Matcher lc = LIGNE_COLONNE.matcher(texte);
        if (lc.matches()) {
            return lc.group(2) + lc.group(1);
        }
        throw new SaisieInvalideException("Case incorrecte : « " + saisie + " ». " + aide());
    }

    @Override
    public String afficher(String moi, List<String> joueurs, Vue vue) {
        Map<String, String> d = Donnees.decoder(vue.donnees());
        String g = d.get("grille");
        StringBuilder texte = new StringBuilder("    A   B   C\n");
        for (int ligne = 0; ligne < 3; ligne++) {
            texte.append(' ').append(ligne + 1).append("  ");
            for (int colonne = 0; colonne < 3; colonne++) {
                char c = g.charAt(ligne * 3 + colonne);
                texte.append(c == '.' ? ' ' : c);
                if (colonne < 2) {
                    texte.append(" | ");
                }
            }
            texte.append('\n');
            if (ligne < 2) {
                texte.append("    ---+---+---\n");
            }
        }
        texte.append("Vous jouez ").append(d.get("symbole"))
                .append(" (X : ").append(d.get("joueurX")).append(", O : ").append(d.get("joueurO")).append(')');
        if (vue.actionAttendue()) {
            texte.append("\nÀ vous de jouer !");
        } else if (!d.get("tour").isEmpty()) {
            String joueurDuTour = d.get("tour").equals("X") ? d.get("joueurX") : d.get("joueurO");
            texte.append("\nEn attente du coup de ").append(joueurDuTour).append('.');
        }
        return texte.toString();
    }
}
