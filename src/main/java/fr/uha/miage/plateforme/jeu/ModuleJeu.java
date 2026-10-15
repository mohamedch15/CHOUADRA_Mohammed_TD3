package fr.uha.miage.plateforme.jeu;

import java.util.Objects;

/** Un jeu complet : sa partie côté serveur et sa présentation côté client. */
public record ModuleJeu(Jeu jeu, PresentationJeu presentation) {

    public ModuleJeu {
        Objects.requireNonNull(jeu, "jeu");
        Objects.requireNonNull(presentation, "presentation");
        if (!jeu.id().equals(presentation.idJeu())) {
            throw new IllegalArgumentException(
                    "La présentation « " + presentation.idJeu() + " » ne correspond pas au jeu « " + jeu.id() + " »");
        }
    }
}
