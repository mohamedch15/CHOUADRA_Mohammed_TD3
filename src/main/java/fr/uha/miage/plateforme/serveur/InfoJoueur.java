package fr.uha.miage.plateforme.serveur;

import java.util.Set;

public record InfoJoueur(String pseudo, EtatJoueur etat, Set<String> jeux) {
}
