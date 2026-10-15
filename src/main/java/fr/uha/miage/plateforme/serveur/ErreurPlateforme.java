package fr.uha.miage.plateforme.serveur;

import fr.uha.miage.plateforme.protocole.CodeErreur;

/** Refus d'une demande : le code est transmis au client et le message lui est montré. */
public class ErreurPlateforme extends Exception {

    private final CodeErreur code;

    public ErreurPlateforme(CodeErreur code, String message) {
        super(message);
        this.code = code;
    }

    public CodeErreur code() {
        return code;
    }
}
