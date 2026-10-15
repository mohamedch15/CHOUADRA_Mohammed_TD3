package fr.uha.miage.plateforme.serveur;

import fr.uha.miage.plateforme.jeu.Jeu;
import fr.uha.miage.plateforme.jeu.ModuleJeu;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Catalogue immuable des jeux que le serveur sait arbitrer. */
public final class CatalogueJeux {

    private static final Pattern ID = Pattern.compile("[a-z0-9-]{1,20}");

    private final Map<String, Jeu> jeux;

    private CatalogueJeux(Collection<Jeu> jeux) {
        Map<String, Jeu> parId = new LinkedHashMap<>();
        for (Jeu jeu : jeux) {
            if (!ID.matcher(jeu.id()).matches()) {
                throw new IllegalArgumentException("Identifiant de jeu invalide : « " + jeu.id() + " »");
            }
            if (parId.put(jeu.id(), jeu) != null) {
                throw new IllegalArgumentException("Jeu enregistré deux fois : « " + jeu.id() + " »");
            }
        }
        this.jeux = Collections.unmodifiableMap(parId);
    }

    public static CatalogueJeux de(Jeu... jeux) {
        return new CatalogueJeux(java.util.List.of(jeux));
    }

    public static CatalogueJeux depuis(Collection<ModuleJeu> modules) {
        return new CatalogueJeux(modules.stream().map(ModuleJeu::jeu).toList());
    }

    public Optional<Jeu> trouver(String id) {
        return Optional.ofNullable(jeux.get(id));
    }

    public Set<String> ids() {
        return jeux.keySet();
    }
}
