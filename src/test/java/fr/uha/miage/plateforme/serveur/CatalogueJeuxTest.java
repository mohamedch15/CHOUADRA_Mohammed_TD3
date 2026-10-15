package fr.uha.miage.plateforme.serveur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uha.miage.plateforme.jeu.Jeu;
import fr.uha.miage.plateforme.jeu.Partie;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CatalogueJeuxTest {

    private static Jeu jeu(String id) {
        return new Jeu() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String nom() {
                return id;
            }

            @Override
            public int nombreJoueurs() {
                return 2;
            }

            @Override
            public Partie creerPartie(List<String> joueurs) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test
    void retrouveLesJeuxEnregistres() {
        CatalogueJeux catalogue = CatalogueJeux.de(jeu("a"), jeu("b-2"));
        assertEquals(Set.of("a", "b-2"), catalogue.ids());
        assertTrue(catalogue.trouver("a").isPresent());
        assertTrue(catalogue.trouver("inconnu").isEmpty());
    }

    @Test
    void refuseLesDoublonsEtLesIdentifiantsInvalides() {
        assertThrows(IllegalArgumentException.class, () -> CatalogueJeux.de(jeu("a"), jeu("a")));
        assertThrows(IllegalArgumentException.class, () -> CatalogueJeux.de(jeu("Majuscule")));
        assertThrows(IllegalArgumentException.class, () -> CatalogueJeux.de(jeu("avec espace")));
    }
}
