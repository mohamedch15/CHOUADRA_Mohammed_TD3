package fr.uha.miage.plateforme.protocole;

public class MessageMalformeException extends RuntimeException {

    public MessageMalformeException(String message) {
        super(message);
    }
}
