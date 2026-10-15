package fr.uha.miage.plateforme.serveur;

import fr.uha.miage.plateforme.jeux.JeuxStandard;
import fr.uha.miage.plateforme.protocole.Message;
import fr.uha.miage.plateforme.protocole.Verbes;
import java.time.Duration;
import java.util.Set;

/** Plateforme sans réseau pour les tests : les joueurs sont reliés à des CanalEnregistreur. */
final class BancPlateforme implements AutoCloseable {

    record Participant(Joueur joueur, CanalEnregistreur canal) {
        String pseudo() {
            return joueur.pseudo();
        }
    }

    final Plateforme plateforme;

    BancPlateforme() {
        this(new Plateforme.Parametres(Duration.ofSeconds(30), Duration.ofSeconds(5), Duration.ofSeconds(20),
                Duration.ofSeconds(5)));
    }

    BancPlateforme(Plateforme.Parametres parametres) {
        this(CatalogueJeux.depuis(JeuxStandard.modules()), parametres);
    }

    BancPlateforme(CatalogueJeux catalogue, Plateforme.Parametres parametres) {
        this.plateforme = new Plateforme(catalogue, parametres);
    }

    Participant connecter(String pseudo, String... jeux) throws ErreurPlateforme {
        CanalEnregistreur canal = new CanalEnregistreur();
        Joueur joueur = plateforme.connecter(pseudo, Set.of(jeux), canal,
                j -> Message.of(Verbes.BIENVENUE, j.pseudo(), j.jeton()));
        return new Participant(joueur, canal);
    }

    Participant connecterDisponible(String pseudo, String... jeux) throws ErreurPlateforme {
        Participant participant = connecter(pseudo, jeux);
        plateforme.definirDisponibilite(participant.joueur(), true);
        return participant;
    }

    @Override
    public void close() {
        plateforme.close();
    }
}
