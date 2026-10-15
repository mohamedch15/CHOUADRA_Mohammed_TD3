package fr.uha.miage.plateforme.jeux.morpion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uha.miage.plateforme.jeu.ActionInvalideException;
import fr.uha.miage.plateforme.jeu.Donnees;
import fr.uha.miage.plateforme.jeu.Issue;
import fr.uha.miage.plateforme.jeu.Partie;
import fr.uha.miage.plateforme.jeu.SaisieInvalideException;
import java.util.List;
import org.junit.jupiter.api.Test;

class MorpionTest {

    private final Partie partie = new MorpionJeu().creerPartie(List.of("alice", "bob"));

    private void jouer(String... coups) throws ActionInvalideException {
        for (int i = 0; i < coups.length; i++) {
            partie.jouer(i % 2 == 0 ? "alice" : "bob", coups[i]);
        }
    }

    private String grille() {
        return Donnees.decoder(partie.vue("alice").donnees()).get("grille");
    }

    @Test
    void lInvitantCommenceEtLesJoueursAlternent() throws Exception {
        assertTrue(partie.vue("alice").actionAttendue());
        assertFalse(partie.vue("bob").actionAttendue());
        assertThrows(ActionInvalideException.class, () -> partie.jouer("bob", "A1"));

        partie.jouer("alice", "B2");
        assertEquals("....X....", grille());
        assertTrue(partie.vue("bob").actionAttendue());
        assertFalse(partie.vue("alice").actionAttendue());
        assertThrows(ActionInvalideException.class, () -> partie.jouer("alice", "A1"), "alice ne peut pas rejouer");
    }

    @Test
    void refuseLesCasesOccupeesOuInconnues() throws Exception {
        partie.jouer("alice", "A1");
        assertThrows(ActionInvalideException.class, () -> partie.jouer("bob", "A1"));
        assertThrows(ActionInvalideException.class, () -> partie.jouer("bob", "D4"));
        assertThrows(ActionInvalideException.class, () -> partie.jouer("bob", ""));
        assertThrows(ActionInvalideException.class, () -> partie.jouer("bob", "A1; DROP"));
        assertEquals("X........", grille(), "les actions refusées ne modifient pas la grille");
        assertTrue(partie.vue("bob").actionAttendue(), "le tour de bob n'est pas consommé");
    }

    @Test
    void detecteUneLigneGagnante() throws Exception {
        jouer("A1", "A2", "B1", "B2", "C1");
        var resultat = partie.resultat().orElseThrow();
        assertEquals(Issue.VICTOIRE, resultat.issuePour("alice"));
        assertEquals(Issue.DEFAITE, resultat.issuePour("bob"));
        assertThrows(ActionInvalideException.class, () -> partie.jouer("bob", "C3"));
        assertFalse(partie.vue("bob").actionAttendue());
    }

    @Test
    void detecteUneColonneEtUneDiagonale() throws Exception {
        jouer("A1", "B1", "A2", "B2", "A3");
        assertEquals(Issue.VICTOIRE, partie.resultat().orElseThrow().issuePour("alice"));

        Partie diagonale = new MorpionJeu().creerPartie(List.of("alice", "bob"));
        for (String[] coup : new String[][] {{"alice", "A1"}, {"bob", "B1"}, {"alice", "B2"}, {"bob", "C1"}, {"alice", "C3"}}) {
            diagonale.jouer(coup[0], coup[1]);
        }
        assertEquals(Issue.VICTOIRE, diagonale.resultat().orElseThrow().issuePour("alice"));
    }

    @Test
    void leSecondJoueurPeutGagner() throws Exception {
        jouer("A1", "B1", "A2", "B2", "C3", "B3");
        var resultat = partie.resultat().orElseThrow();
        assertEquals(Issue.VICTOIRE, resultat.issuePour("bob"));
        assertEquals(Issue.DEFAITE, resultat.issuePour("alice"));
    }

    @Test
    void detecteLeMatchNul() throws Exception {
        // X O X / X O O / O X X : aucune ligne complète
        jouer("A1", "B1", "C1", "B2", "A2", "C2", "B3", "A3", "C3");
        var resultat = partie.resultat().orElseThrow();
        assertEquals(Issue.NUL, resultat.issuePour("alice"));
        assertEquals(Issue.NUL, resultat.issuePour("bob"));
        assertFalse(grille().contains("."));
    }

    @Test
    void unAbandonTermineLaPartie() {
        var resultat = partie.abandonner("alice");
        assertEquals(Issue.VICTOIRE, resultat.issuePour("bob"));
        assertThrows(ActionInvalideException.class, () -> partie.jouer("bob", "A1"));
    }

    @Test
    void laPresentationConvertitLesSaisies() throws Exception {
        MorpionPresentation presentation = new MorpionPresentation();
        assertEquals("B2", presentation.actionDepuisSaisie("b2"));
        assertEquals("B2", presentation.actionDepuisSaisie(" 2 B "));
        assertEquals("C3", presentation.actionDepuisSaisie("c 3"));
        assertThrows(SaisieInvalideException.class, () -> presentation.actionDepuisSaisie("z9"));
    }

    @Test
    void laPresentationAfficheLaGrilleEtLeTour() throws Exception {
        MorpionPresentation presentation = new MorpionPresentation();
        partie.jouer("alice", "B2");

        String pourBob = presentation.afficher("bob", partie.joueurs(), partie.vue("bob"));
        assertTrue(pourBob.contains("A   B   C"));
        assertTrue(pourBob.contains("À vous de jouer"));
        String pourAlice = presentation.afficher("alice", partie.joueurs(), partie.vue("alice"));
        assertTrue(pourAlice.contains("En attente du coup de bob"));
    }
}
