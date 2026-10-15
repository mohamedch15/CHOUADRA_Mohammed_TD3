package fr.uha.miage.plateforme.client;

import fr.uha.miage.plateforme.jeu.ModuleJeu;
import fr.uha.miage.plateforme.jeu.PresentationJeu;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Catalogue immuable des jeux que ce client sait afficher et jouer. */
public final class CataloguePresentations {

    private final Map<String, PresentationJeu> presentations;

    private CataloguePresentations(Collection<PresentationJeu> presentations) {
        Map<String, PresentationJeu> parId = new LinkedHashMap<>();
        for (PresentationJeu presentation : presentations) {
            if (parId.put(presentation.idJeu(), presentation) != null) {
                throw new IllegalArgumentException("Présentation enregistrée deux fois : « " + presentation.idJeu() + " »");
            }
        }
        this.presentations = Collections.unmodifiableMap(parId);
    }

    public static CataloguePresentations de(PresentationJeu... presentations) {
        return new CataloguePresentations(java.util.List.of(presentations));
    }

    public static CataloguePresentations depuis(Collection<ModuleJeu> modules) {
        return new CataloguePresentations(modules.stream().map(ModuleJeu::presentation).toList());
    }

    public Optional<PresentationJeu> trouver(String id) {
        return Optional.ofNullable(presentations.get(id));
    }

    public Set<String> ids() {
        return presentations.keySet();
    }
}
