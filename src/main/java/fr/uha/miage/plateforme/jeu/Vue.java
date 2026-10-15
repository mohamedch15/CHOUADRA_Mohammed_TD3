package fr.uha.miage.plateforme.jeu;

import java.util.Objects;

/** Vue personnelle d'une partie : données propres au jeu (voir Donnees) et indicateur « à vous de jouer ». */
public record Vue(boolean actionAttendue, String donnees) {

    public Vue {
        Objects.requireNonNull(donnees, "donnees");
    }
}
