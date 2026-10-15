package fr.uha.miage.plateforme.jeux.shifumi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uha.miage.plateforme.jeu.ActionInvalideException;
import fr.uha.miage.plateforme.jeu.Donnees;
import fr.uha.miage.plateforme.jeu.Issue;
import fr.uha.miage.plateforme.jeu.Partie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShifumiPartieTest {

    private final Partie partie = new ShifumiJeu().creerPartie(List.of("alice", "bob"));

    private Map<String, String> donnees(String joueur) {
        return Donnees.decoder(partie.vue(joueur).donnees());
    }

    @Test
    void lesReglesDesCoupsSontCorrectes() {
        assertTrue(Coup.PIERRE.bat(Coup.CISEAUX));
        assertTrue(Coup.CISEAUX.bat(Coup.FEUILLE));
        assertTrue(Coup.FEUILLE.bat(Coup.PIERRE));
        assertFalse(Coup.PIERRE.bat(Coup.PIERRE));
        assertFalse(Coup.PIERRE.bat(Coup.FEUILLE));
    }

    @Test
    void lePremierChoixResteCacheJusquAuSecond() throws Exception {
        partie.jouer("alice", "PIERRE");

        assertEquals("PIERRE", donnees("alice").get("monChoix"));
        assertEquals("oui", donnees("bob").get("adversaireADecide"));
        String vueBob = partie.vue("bob").donnees();
        assertFalse(vueBob.contains("PIERRE"), "le choix d'alice ne doit pas apparaître dans la vue de bob");
        assertFalse(partie.vue("alice").actionAttendue(), "alice a déjà joué");
        assertTrue(partie.vue("bob").actionAttendue());
        assertTrue(partie.resultat().isEmpty());
    }

    @Test
    void lesCoupsSontRevelesUneFoisLaMancheResolue() throws Exception {
        partie.jouer("alice", "pierre");
        partie.jouer("bob", "ciseaux");

        Map<String, String> vueAlice = donnees("alice");
        assertEquals("GAGNE", vueAlice.get("derniereManche"));
        assertEquals("PIERRE", vueAlice.get("monDernierCoup"));
        assertEquals("CISEAUX", vueAlice.get("dernierCoupAdversaire"));
        assertEquals("1", vueAlice.get("mesPoints"));
        assertEquals("PERDU", donnees("bob").get("derniereManche"));
        assertEquals("", vueAlice.get("monChoix"), "la nouvelle manche repart sans choix");
        assertTrue(partie.vue("alice").actionAttendue());
        assertTrue(partie.vue("bob").actionAttendue());
    }

    @Test
    void uneEgaliteRejoueLaManche() throws Exception {
        partie.jouer("alice", "FEUILLE");
        partie.jouer("bob", "FEUILLE");

        assertEquals("NUL", donnees("alice").get("derniereManche"));
        assertEquals("0", donnees("alice").get("mesPoints"));
        assertEquals("0", donnees("bob").get("mesPoints"));
        assertTrue(partie.resultat().isEmpty());
    }

    @Test
    void lePremierAGagnerDeuxManchesRemporteLaPartie() throws Exception {
        partie.jouer("alice", "PIERRE");
        partie.jouer("bob", "CISEAUX");
        partie.jouer("alice", "FEUILLE");
        partie.jouer("bob", "CISEAUX");
        partie.jouer("alice", "PIERRE");
        partie.jouer("bob", "CISEAUX");

        var resultat = partie.resultat().orElseThrow();
        assertEquals(Issue.VICTOIRE, resultat.issuePour("alice"));
        assertEquals(Issue.DEFAITE, resultat.issuePour("bob"));
        assertFalse(partie.vue("alice").actionAttendue());
        assertFalse(partie.vue("bob").actionAttendue());
        assertThrows(ActionInvalideException.class, () -> partie.jouer("bob", "PIERRE"));
    }

    @Test
    void refuseUnSecondChoixDansLaMemeManche() throws Exception {
        partie.jouer("alice", "PIERRE");
        assertThrows(ActionInvalideException.class, () -> partie.jouer("alice", "FEUILLE"));
        assertEquals("PIERRE", donnees("alice").get("monChoix"), "le premier choix est conservé");
    }

    @Test
    void refuseUnCoupInconnuEtUnJoueurEtranger() {
        assertThrows(ActionInvalideException.class, () -> partie.jouer("alice", "LEZARD"));
        assertThrows(ActionInvalideException.class, () -> partie.jouer("intrus", "PIERRE"));
        assertTrue(partie.vue("alice").actionAttendue(), "une action refusée ne consomme pas le tour");
    }

    @Test
    void unAbandonDonneLaVictoireALAdversaire() {
        var resultat = partie.abandonner("bob");
        assertEquals(Issue.VICTOIRE, resultat.issuePour("alice"));
        assertEquals(Issue.DEFAITE, resultat.issuePour("bob"));
    }

    @Test
    void creerUnePartieExigeDeuxJoueurs() {
        assertThrows(IllegalArgumentException.class, () -> new ShifumiJeu().creerPartie(List.of("seul")));
        assertThrows(IllegalArgumentException.class, () -> new ShifumiJeu(0));
    }
}
