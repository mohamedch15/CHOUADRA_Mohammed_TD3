package fr.uha.miage.plateforme.jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PartieBaseTest {

    /** Jeu minimal : le premier joueur qui joue « stop » gagne. */
    private static final class PartieStop extends PartieBase {
        PartieStop(List<String> joueurs) {
            super(joueurs);
        }

        @Override
        protected void appliquer(String joueur, String action) throws ActionInvalideException {
            if (!action.equals("stop")) {
                throw new ActionInvalideException("Seul « stop » est accepté.");
            }
            terminer(ResultatPartie.victoire(joueurs(), joueur, joueur + " a gagné."));
        }

        @Override
        public Vue vue(String joueur) {
            return new Vue(!estTerminee(), "");
        }
    }

    @Test
    void refuseLesJoueursEnDouble() {
        assertThrows(IllegalArgumentException.class, () -> new PartieStop(List.of("a", "a")));
    }

    @Test
    void refuseUnNonParticipant() {
        PartieStop partie = new PartieStop(List.of("a", "b"));
        assertThrows(ActionInvalideException.class, () -> partie.jouer("intrus", "stop"));
        assertTrue(partie.resultat().isEmpty());
    }

    @Test
    void refuseUneActionInvalideSansTerminer() {
        PartieStop partie = new PartieStop(List.of("a", "b"));
        assertThrows(ActionInvalideException.class, () -> partie.jouer("a", "autre"));
        assertTrue(partie.resultat().isEmpty());
    }

    @Test
    void refuseLesActionsApresLaFin() throws Exception {
        PartieStop partie = new PartieStop(List.of("a", "b"));
        partie.jouer("a", "stop");
        assertEquals(Issue.VICTOIRE, partie.resultat().orElseThrow().issuePour("a"));
        assertThrows(ActionInvalideException.class, () -> partie.jouer("b", "stop"));
        assertFalse(partie.vue("b").actionAttendue());
    }

    @Test
    void abandonDonneLaVictoireAuxAutresEtEstIdempotent() {
        PartieStop partie = new PartieStop(List.of("a", "b"));
        ResultatPartie resultat = partie.abandonner("a");
        assertEquals(Issue.DEFAITE, resultat.issuePour("a"));
        assertEquals(Issue.VICTOIRE, resultat.issuePour("b"));
        assertEquals(resultat, partie.abandonner("b"));
    }
}
