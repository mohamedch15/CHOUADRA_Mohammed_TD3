package fr.uha.miage.plateforme.jeux.morpion;

import fr.uha.miage.plateforme.jeu.Jeu;
import fr.uha.miage.plateforme.jeu.Partie;
import java.util.List;

public final class MorpionJeu implements Jeu {

    public static final String ID = "morpion";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String nom() {
        return "Morpion";
    }

    @Override
    public int nombreJoueurs() {
        return 2;
    }

    @Override
    public Partie creerPartie(List<String> joueurs) {
        if (joueurs.size() != 2) {
            throw new IllegalArgumentException("Le morpion se joue à deux");
        }
        return new MorpionPartie(joueurs);
    }
}
