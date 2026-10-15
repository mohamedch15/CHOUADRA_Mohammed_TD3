package fr.uha.miage.plateforme.jeux.shifumi;

import fr.uha.miage.plateforme.jeu.ActionInvalideException;
import fr.uha.miage.plateforme.jeu.Donnees;
import fr.uha.miage.plateforme.jeu.PartieBase;
import fr.uha.miage.plateforme.jeu.ResultatPartie;
import fr.uha.miage.plateforme.jeu.Vue;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Choix simultanés et cachés : un coup n'est révélé que lorsque les deux joueurs ont joué. */
final class ShifumiPartie extends PartieBase {

    private final int manchesGagnantes;
    private final Map<String, Coup> choixEnCours = new HashMap<>();
    private final Map<String, Integer> points = new HashMap<>();
    private int manche = 1;
    private Map<String, Coup> dernierCoups = Map.of();
    private String dernierGagnant = "";

    ShifumiPartie(List<String> joueurs, int manchesGagnantes) {
        super(joueurs);
        this.manchesGagnantes = manchesGagnantes;
        joueurs.forEach(joueur -> points.put(joueur, 0));
    }

    @Override
    protected void appliquer(String joueur, String action) throws ActionInvalideException {
        Coup coup = lireCoup(action);
        if (choixEnCours.containsKey(joueur)) {
            throw new ActionInvalideException("Vous avez déjà choisi pour cette manche : attendez votre adversaire.");
        }
        choixEnCours.put(joueur, coup);
        if (choixEnCours.size() == joueurs().size()) {
            resoudreManche();
        }
    }

    private static Coup lireCoup(String action) throws ActionInvalideException {
        try {
            return Coup.valueOf(action.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ActionInvalideException("Coup inconnu : « " + action + " ». Choisissez PIERRE, FEUILLE ou CISEAUX.");
        }
    }

    private void resoudreManche() {
        String premier = joueurs().get(0);
        String second = joueurs().get(1);
        Coup coupPremier = choixEnCours.get(premier);
        Coup coupSecond = choixEnCours.get(second);
        dernierCoups = Map.copyOf(choixEnCours);
        choixEnCours.clear();
        manche++;
        if (coupPremier == coupSecond) {
            dernierGagnant = "";
            return;
        }
        dernierGagnant = coupPremier.bat(coupSecond) ? premier : second;
        int total = points.merge(dernierGagnant, 1, Integer::sum);
        if (total >= manchesGagnantes) {
            String perdant = dernierGagnant.equals(premier) ? second : premier;
            terminer(ResultatPartie.victoire(joueurs(), dernierGagnant,
                    dernierGagnant + " remporte la partie " + total + " manche(s) à " + points.get(perdant) + "."));
        }
    }

    private String adversaire(String joueur) {
        if (!joueurs().contains(joueur)) {
            throw new IllegalArgumentException("Joueur inconnu : " + joueur);
        }
        return joueurs().get(0).equals(joueur) ? joueurs().get(1) : joueurs().get(0);
    }

    @Override
    public Vue vue(String joueur) {
        String adversaire = adversaire(joueur);
        String derniereManche = dernierCoups.isEmpty() ? ""
                : dernierGagnant.isEmpty() ? "NUL"
                : dernierGagnant.equals(joueur) ? "GAGNE" : "PERDU";
        Coup monDernier = dernierCoups.get(joueur);
        Coup dernierAdversaire = dernierCoups.get(adversaire);
        Coup monChoix = choixEnCours.get(joueur);
        String donnees = Donnees.encoder(
                "manche", String.valueOf(manche),
                "objectif", String.valueOf(manchesGagnantes),
                "mesPoints", String.valueOf(points.get(joueur)),
                "pointsAdversaire", String.valueOf(points.get(adversaire)),
                "adversaire", adversaire,
                "monChoix", monChoix == null ? "" : monChoix.name(),
                "adversaireADecide", choixEnCours.containsKey(adversaire) ? "oui" : "non",
                "derniereManche", derniereManche,
                "monDernierCoup", monDernier == null ? "" : monDernier.name(),
                "dernierCoupAdversaire", dernierAdversaire == null ? "" : dernierAdversaire.name());
        return new Vue(!estTerminee() && monChoix == null, donnees);
    }
}
