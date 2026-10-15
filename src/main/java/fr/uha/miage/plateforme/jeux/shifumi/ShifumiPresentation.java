package fr.uha.miage.plateforme.jeux.shifumi;

import fr.uha.miage.plateforme.jeu.Donnees;
import fr.uha.miage.plateforme.jeu.PresentationJeu;
import fr.uha.miage.plateforme.jeu.SaisieInvalideException;
import fr.uha.miage.plateforme.jeu.Vue;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShifumiPresentation implements PresentationJeu {

    @Override
    public String idJeu() {
        return ShifumiJeu.ID;
    }

    @Override
    public String nomJeu() {
        return "Shi-fu-mi";
    }

    @Override
    public String regles() {
        return "Chaque manche, les deux joueurs choisissent en secret pierre, feuille ou ciseaux.\n"
                + "La pierre bat les ciseaux, les ciseaux battent la feuille, la feuille bat la pierre.\n"
                + "Égalité : la manche est rejouée. Le premier à gagner 2 manches remporte la partie.";
    }

    @Override
    public String aide() {
        return "Tapez pierre, feuille ou ciseaux (ou p, f, c).";
    }

    @Override
    public String actionDepuisSaisie(String saisie) throws SaisieInvalideException {
        return switch (saisie.trim().toLowerCase(Locale.ROOT)) {
            case "pierre", "p" -> Coup.PIERRE.name();
            case "feuille", "f", "papier" -> Coup.FEUILLE.name();
            case "ciseaux", "c" -> Coup.CISEAUX.name();
            default -> throw new SaisieInvalideException("Je ne comprends pas « " + saisie + " ». " + aide());
        };
    }

    @Override
    public String afficher(String moi, List<String> joueurs, Vue vue) {
        Map<String, String> d = Donnees.decoder(vue.donnees());
        String adversaire = d.get("adversaire");
        StringBuilder texte = new StringBuilder();
        if (!d.get("derniereManche").isEmpty()) {
            texte.append("Manche précédente : vous avez joué ").append(d.get("monDernierCoup"))
                    .append(", ").append(adversaire).append(" a joué ").append(d.get("dernierCoupAdversaire"))
                    .append(" -> ").append(switch (d.get("derniereManche")) {
                        case "GAGNE" -> "manche gagnée !";
                        case "PERDU" -> "manche perdue.";
                        default -> "égalité, on rejoue.";
                    }).append('\n');
        }
        texte.append("Score : vous ").append(d.get("mesPoints")).append(" - ").append(d.get("pointsAdversaire"))
                .append(' ').append(adversaire).append(" (premier à ").append(d.get("objectif")).append(")");
        if (vue.actionAttendue()) {
            texte.append("\nManche ").append(d.get("manche")).append(" : à vous de choisir.");
            if (d.get("adversaireADecide").equals("oui")) {
                texte.append(' ').append(adversaire).append(" a déjà choisi.");
            }
        } else if (!d.get("monChoix").isEmpty()) {
            texte.append("\nVous avez choisi ").append(d.get("monChoix")).append(" : en attente de ")
                    .append(adversaire).append('.');
        }
        return texte.toString();
    }
}
