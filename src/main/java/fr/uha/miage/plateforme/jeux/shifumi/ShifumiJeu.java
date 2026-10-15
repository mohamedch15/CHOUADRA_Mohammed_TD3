package fr.uha.miage.plateforme.jeux.shifumi;

import fr.uha.miage.plateforme.jeu.Jeu;
import fr.uha.miage.plateforme.jeu.Partie;
import java.util.List;

public final class ShifumiJeu implements Jeu {

    public static final String ID = "shifumi";

    private final int manchesGagnantes;

    public ShifumiJeu() {
        this(2);
    }

    public ShifumiJeu(int manchesGagnantes) {
        if (manchesGagnantes < 1) {
            throw new IllegalArgumentException("Il faut au moins une manche gagnante");
        }
        this.manchesGagnantes = manchesGagnantes;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String nom() {
        return "Shi-fu-mi";
    }

    @Override
    public int nombreJoueurs() {
        return 2;
    }

    @Override
    public Partie creerPartie(List<String> joueurs) {
        if (joueurs.size() != 2) {
            throw new IllegalArgumentException("Le shi-fu-mi se joue à deux");
        }
        return new ShifumiPartie(joueurs, manchesGagnantes);
    }
}
