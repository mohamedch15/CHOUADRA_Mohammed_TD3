package fr.uha.miage.plateforme.jeu;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Codage simple « cle=valeur;cle=valeur » pour les données d'une Vue. */
public final class Donnees {

    private static final Pattern CLE = Pattern.compile("[A-Za-z0-9_]+");

    private Donnees() {
    }

    /** Alterne clés et valeurs : encoder("tour", "X", "grille", "........."). */
    public static String encoder(String... clesEtValeurs) {
        if (clesEtValeurs.length % 2 != 0) {
            throw new IllegalArgumentException("Il faut un nombre pair d'arguments (clé, valeur)");
        }
        StringBuilder texte = new StringBuilder();
        for (int i = 0; i < clesEtValeurs.length; i += 2) {
            String cle = clesEtValeurs[i];
            if (!CLE.matcher(cle).matches()) {
                throw new IllegalArgumentException("Clé invalide : " + cle);
            }
            if (i > 0) {
                texte.append(';');
            }
            texte.append(cle).append('=').append(URLEncoder.encode(clesEtValeurs[i + 1], StandardCharsets.UTF_8));
        }
        return texte.toString();
    }

    public static Map<String, String> decoder(String texte) {
        Map<String, String> valeurs = new LinkedHashMap<>();
        if (!texte.isEmpty()) {
            for (String paire : texte.split(";", -1)) {
                int egal = paire.indexOf('=');
                if (egal <= 0) {
                    throw new IllegalArgumentException("Données mal formées : « " + texte + " »");
                }
                valeurs.put(paire.substring(0, egal),
                        URLDecoder.decode(paire.substring(egal + 1), StandardCharsets.UTF_8));
            }
        }
        return Collections.unmodifiableMap(valeurs);
    }
}
