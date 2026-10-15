package fr.uha.miage.plateforme.jeux.shifumi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uha.miage.plateforme.jeu.Partie;
import fr.uha.miage.plateforme.jeu.SaisieInvalideException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShifumiPresentationTest {

    private final ShifumiPresentation presentation = new ShifumiPresentation();

    @Test
    void convertitLesSaisiesEnActions() throws Exception {
        assertEquals("PIERRE", presentation.actionDepuisSaisie(" Pierre "));
        assertEquals("PIERRE", presentation.actionDepuisSaisie("p"));
        assertEquals("FEUILLE", presentation.actionDepuisSaisie("F"));
        assertEquals("CISEAUX", presentation.actionDepuisSaisie("ciseaux"));
        assertThrows(SaisieInvalideException.class, () -> presentation.actionDepuisSaisie("lezard"));
    }

    @Test
    void afficheLEtatSansRevelerLeChoixAdverse() throws Exception {
        Partie partie = new ShifumiJeu().creerPartie(List.of("alice", "bob"));
        partie.jouer("alice", "PIERRE");

        String pourBob = presentation.afficher("bob", partie.joueurs(), partie.vue("bob"));
        assertTrue(pourBob.contains("alice a déjà choisi"));
        assertFalse(pourBob.contains("PIERRE"));

        String pourAlice = presentation.afficher("alice", partie.joueurs(), partie.vue("alice"));
        assertTrue(pourAlice.contains("Vous avez choisi PIERRE"));
    }

    @Test
    void afficheLaMancheResolue() throws Exception {
        Partie partie = new ShifumiJeu().creerPartie(List.of("alice", "bob"));
        partie.jouer("alice", "PIERRE");
        partie.jouer("bob", "CISEAUX");

        String texte = presentation.afficher("alice", partie.joueurs(), partie.vue("alice"));
        assertTrue(texte.contains("manche gagnée"));
        assertTrue(texte.contains("vous 1 - 0 bob"));
    }
}
