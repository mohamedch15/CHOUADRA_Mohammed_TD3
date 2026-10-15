package fr.uha.miage.plateforme.jeux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uha.miage.plateforme.client.CataloguePresentations;
import fr.uha.miage.plateforme.jeu.ModuleJeu;
import fr.uha.miage.plateforme.serveur.CatalogueJeux;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JeuxStandardTest {

    @Test
    void lesCataloguesServeurEtClientSontCoherents() {
        CatalogueJeux serveur = CatalogueJeux.depuis(JeuxStandard.modules());
        CataloguePresentations client = CataloguePresentations.depuis(JeuxStandard.modules());

        assertEquals(Set.of("shifumi", "morpion"), serveur.ids());
        assertEquals(serveur.ids(), client.ids());
    }

    @Test
    void chaqueJeuSeJoueADeuxEtSaPresentationLuiCorrespond() {
        for (ModuleJeu module : JeuxStandard.modules()) {
            assertEquals(2, module.jeu().nombreJoueurs());
            assertEquals(module.jeu().id(), module.presentation().idJeu());
            assertTrue(module.presentation().regles().length() > 20);
        }
    }
}
