package fr.uha.miage.plateforme.jeux;

import fr.uha.miage.plateforme.jeu.ModuleJeu;
import fr.uha.miage.plateforme.jeux.morpion.MorpionJeu;
import fr.uha.miage.plateforme.jeux.morpion.MorpionPresentation;
import fr.uha.miage.plateforme.jeux.shifumi.ShifumiJeu;
import fr.uha.miage.plateforme.jeux.shifumi.ShifumiPresentation;
import java.util.List;

/** Point unique d'enregistrement des jeux proposés par la plateforme. */
public final class JeuxStandard {

    private static final List<ModuleJeu> MODULES = List.of(
            new ModuleJeu(new ShifumiJeu(), new ShifumiPresentation()),
            new ModuleJeu(new MorpionJeu(), new MorpionPresentation()));

    private JeuxStandard() {
    }

    public static List<ModuleJeu> modules() {
        return MODULES;
    }
}
