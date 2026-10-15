package fr.uha.miage.plateforme.jeu;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Socle commun : liste des joueurs, contrôle de participation, fin de partie et forfait. */
public abstract class PartieBase implements Partie {

    private final List<String> joueurs;
    private ResultatPartie resultat;

    protected PartieBase(List<String> joueurs) {
        this.joueurs = List.copyOf(joueurs);
        if (this.joueurs.isEmpty() || new HashSet<>(this.joueurs).size() != this.joueurs.size()) {
            throw new IllegalArgumentException("Les joueurs doivent être distincts et non vides : " + joueurs);
        }
    }

    @Override
    public final List<String> joueurs() {
        return joueurs;
    }

    @Override
    public final void jouer(String joueur, String action) throws ActionInvalideException {
        if (!joueurs.contains(joueur)) {
            throw new ActionInvalideException("Vous ne participez pas à cette partie.");
        }
        if (resultat != null) {
            throw new ActionInvalideException("La partie est terminée.");
        }
        appliquer(joueur, action);
    }

    /** Règles du jeu : valide puis applique l'action, et appelle terminer(...) si la partie s'achève. */
    protected abstract void appliquer(String joueur, String action) throws ActionInvalideException;

    protected final void terminer(ResultatPartie resultatFinal) {
        this.resultat = resultatFinal;
    }

    protected final boolean estTerminee() {
        return resultat != null;
    }

    @Override
    public final Optional<ResultatPartie> resultat() {
        return Optional.ofNullable(resultat);
    }

    @Override
    public ResultatPartie abandonner(String joueur) {
        if (resultat == null) {
            ResultatPartie forfait = ResultatPartie.forfait(joueurs, joueur);
            terminer(forfait);
        }
        return resultat;
    }
}
