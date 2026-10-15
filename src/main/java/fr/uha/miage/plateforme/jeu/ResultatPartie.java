package fr.uha.miage.plateforme.jeu;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Résultat final : une issue par joueur et une phrase de synthèse destinée aux joueurs. */
public record ResultatPartie(Map<String, Issue> issues, String resume) {

    public ResultatPartie {
        issues = Map.copyOf(issues);
        Objects.requireNonNull(resume, "resume");
    }

    public static ResultatPartie victoire(List<String> joueurs, String gagnant, String resume) {
        Map<String, Issue> issues = new LinkedHashMap<>();
        for (String joueur : joueurs) {
            issues.put(joueur, joueur.equals(gagnant) ? Issue.VICTOIRE : Issue.DEFAITE);
        }
        return new ResultatPartie(issues, resume);
    }

    public static ResultatPartie nul(List<String> joueurs, String resume) {
        Map<String, Issue> issues = new LinkedHashMap<>();
        for (String joueur : joueurs) {
            issues.put(joueur, Issue.NUL);
        }
        return new ResultatPartie(issues, resume);
    }

    /** Le joueur fautif perd, tous les autres gagnent. */
    public static ResultatPartie forfait(List<String> joueurs, String fautif) {
        Map<String, Issue> issues = new LinkedHashMap<>();
        for (String joueur : joueurs) {
            issues.put(joueur, joueur.equals(fautif) ? Issue.DEFAITE : Issue.VICTOIRE);
        }
        return new ResultatPartie(issues, fautif + " a abandonné la partie.");
    }

    public Issue issuePour(String joueur) {
        return issues.getOrDefault(joueur, Issue.NUL);
    }
}
