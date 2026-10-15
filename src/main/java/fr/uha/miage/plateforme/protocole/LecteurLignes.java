package fr.uha.miage.plateforme.protocole;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Lit des lignes UTF-8 en refusant les lignes plus longues que la limite (protection mémoire). */
public final class LecteurLignes implements Closeable {

    private final InputStream flux;
    private final int tailleMax;

    public LecteurLignes(InputStream flux, int tailleMax) {
        this.flux = new BufferedInputStream(flux);
        this.tailleMax = tailleMax;
    }

    /** Retourne la ligne suivante sans son terminateur, ou null en fin de flux. */
    public String lireLigne() throws IOException {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream(128);
        int octet;
        while ((octet = flux.read()) != -1) {
            if (octet == '\n') {
                return construire(tampon);
            }
            if (tampon.size() >= tailleMax * 4) {
                throw new LigneTropLongueException();
            }
            tampon.write(octet);
        }
        return tampon.size() == 0 ? null : construire(tampon);
    }

    private static String construire(ByteArrayOutputStream tampon) {
        String ligne = tampon.toString(StandardCharsets.UTF_8);
        return ligne.endsWith("\r") ? ligne.substring(0, ligne.length() - 1) : ligne;
    }

    @Override
    public void close() throws IOException {
        flux.close();
    }

    public static final class LigneTropLongueException extends IOException {
        public LigneTropLongueException() {
            super("Ligne reçue trop longue");
        }
    }
}
