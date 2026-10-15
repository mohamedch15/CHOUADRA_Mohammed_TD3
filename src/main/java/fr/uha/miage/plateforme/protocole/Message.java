package fr.uha.miage.plateforme.protocole;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Message applicatif : un verbe suivi d'arguments, sérialisé sur une ligne de texte UTF-8. */
public record Message(String verbe, List<String> arguments) {

    public static final int TAILLE_MAX = 4096;
    private static final Pattern VERBE = Pattern.compile("[A-Z][A-Z_]{0,31}");
    private static final String ARGUMENT_VIDE = "~";

    public Message {
        Objects.requireNonNull(verbe, "verbe");
        if (!VERBE.matcher(verbe).matches()) {
            throw new MessageMalformeException("Verbe invalide : « " + verbe + " »");
        }
        arguments = List.copyOf(arguments);
    }

    public static Message of(String verbe, Object... arguments) {
        List<String> liste = new ArrayList<>(arguments.length);
        for (Object argument : arguments) {
            liste.add(String.valueOf(argument));
        }
        return new Message(verbe, liste);
    }

    public static Message decoder(String ligne) {
        if (ligne == null || ligne.isEmpty()) {
            throw new MessageMalformeException("Message vide");
        }
        if (ligne.length() > TAILLE_MAX) {
            throw new MessageMalformeException("Message trop long (" + TAILLE_MAX + " caractères maximum)");
        }
        String[] jetons = ligne.split(" ", -1);
        List<String> arguments = new ArrayList<>(jetons.length - 1);
        for (int i = 1; i < jetons.length; i++) {
            arguments.add(decoderArgument(jetons[i]));
        }
        return new Message(jetons[0], arguments);
    }

    public String encoder() {
        StringBuilder ligne = new StringBuilder(verbe);
        for (String argument : arguments) {
            ligne.append(' ').append(encoderArgument(argument));
        }
        if (ligne.length() > TAILLE_MAX) {
            throw new MessageMalformeException("Message trop long (" + TAILLE_MAX + " caractères maximum)");
        }
        return ligne.toString();
    }

    public int nombreArguments() {
        return arguments.size();
    }

    public String argument(int index) {
        if (index < 0 || index >= arguments.size()) {
            throw new MessageMalformeException(
                    "Argument n°" + (index + 1) + " manquant pour " + verbe);
        }
        return arguments.get(index);
    }

    public int argumentEntier(int index) {
        String valeur = argument(index);
        try {
            return Integer.parseInt(valeur);
        } catch (NumberFormatException e) {
            throw new MessageMalformeException(
                    "L'argument n°" + (index + 1) + " de " + verbe + " doit être un entier : « " + valeur + " »");
        }
    }

    public boolean est(String verbeAttendu) {
        return verbe.equals(verbeAttendu);
    }

    private static String encoderArgument(String argument) {
        return argument.isEmpty() ? ARGUMENT_VIDE : URLEncoder.encode(argument, StandardCharsets.UTF_8);
    }

    private static String decoderArgument(String jeton) {
        if (jeton.isEmpty()) {
            throw new MessageMalformeException("Espaces superflus dans le message");
        }
        if (jeton.equals(ARGUMENT_VIDE)) {
            return "";
        }
        try {
            return URLDecoder.decode(jeton, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new MessageMalformeException("Argument mal encodé : « " + jeton + " »");
        }
    }
}
