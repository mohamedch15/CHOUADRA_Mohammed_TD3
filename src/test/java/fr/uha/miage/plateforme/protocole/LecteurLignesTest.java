package fr.uha.miage.plateforme.protocole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LecteurLignesTest {

    private static LecteurLignes lecteur(String texte, int tailleMax) {
        return new LecteurLignes(new ByteArrayInputStream(texte.getBytes(StandardCharsets.UTF_8)), tailleMax);
    }

    @Test
    void litLesLignesJusqueLaFinDuFlux() throws Exception {
        try (LecteurLignes lecteur = lecteur("un\r\ndeux é\ntrois", 100)) {
            assertEquals("un", lecteur.lireLigne());
            assertEquals("deux é", lecteur.lireLigne());
            assertEquals("trois", lecteur.lireLigne());
            assertNull(lecteur.lireLigne());
        }
    }

    @Test
    void refuseUneLigneTropLongue() throws Exception {
        try (LecteurLignes lecteur = lecteur("x".repeat(1000) + "\n", 10)) {
            assertThrows(LecteurLignes.LigneTropLongueException.class, lecteur::lireLigne);
        }
    }
}
