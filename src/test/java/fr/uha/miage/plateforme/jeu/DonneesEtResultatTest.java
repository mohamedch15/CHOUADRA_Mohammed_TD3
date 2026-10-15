package fr.uha.miage.plateforme.jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DonneesEtResultatTest {

    @Test
    void donneesAllerRetourAvecCaracteresSpeciaux() {
        String texte = Donnees.encoder("grille", "X.O;=&é", "tour", "X");
        Map<String, String> valeurs = Donnees.decoder(texte);
        assertEquals("X.O;=&é", valeurs.get("grille"));
        assertEquals("X", valeurs.get("tour"));
        assertEquals(List.of("grille", "tour"), List.copyOf(valeurs.keySet()));
    }

    @Test
    void donneesVides() {
        assertTrue(Donnees.decoder(Donnees.encoder()).isEmpty());
    }

    @Test
    void donneesInvalides() {
        assertThrows(IllegalArgumentException.class, () -> Donnees.encoder("seul"));
        assertThrows(IllegalArgumentException.class, () -> Donnees.encoder("cle invalide", "v"));
        assertThrows(IllegalArgumentException.class, () -> Donnees.decoder("sansEgal"));
    }

    @Test
    void resultatVictoireNulEtForfait() {
        List<String> joueurs = List.of("alice", "bob");
        assertEquals(Issue.VICTOIRE, ResultatPartie.victoire(joueurs, "alice", "x").issuePour("alice"));
        assertEquals(Issue.DEFAITE, ResultatPartie.victoire(joueurs, "alice", "x").issuePour("bob"));
        assertEquals(Issue.NUL, ResultatPartie.nul(joueurs, "x").issuePour("bob"));
        ResultatPartie forfait = ResultatPartie.forfait(joueurs, "bob");
        assertEquals(Issue.DEFAITE, forfait.issuePour("bob"));
        assertEquals(Issue.VICTOIRE, forfait.issuePour("alice"));
    }
}
