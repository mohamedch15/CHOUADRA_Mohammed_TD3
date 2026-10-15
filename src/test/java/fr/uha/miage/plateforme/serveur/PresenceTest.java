package fr.uha.miage.plateforme.serveur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uha.miage.plateforme.protocole.CodeErreur;
import fr.uha.miage.plateforme.protocole.Message;
import fr.uha.miage.plateforme.protocole.Verbes;
import fr.uha.miage.plateforme.serveur.BancPlateforme.Participant;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(20)
class PresenceTest {

    private BancPlateforme banc;

    @BeforeEach
    void creerBanc() {
        banc = new BancPlateforme();
    }

    @AfterEach
    void fermerBanc() {
        banc.close();
    }

    @Test
    void lAccueilEstLePremierMessageRecuParLeJoueur() throws Exception {
        Participant alice = banc.connecter("alice", "shifumi");
        banc.connecter("bob", "shifumi");

        assertEquals(Verbes.BIENVENUE, alice.canal().historique().get(0).verbe());
        assertEquals(alice.joueur().jeton(), alice.canal().historique().get(0).argument(1));
    }

    @Test
    void plusieursJoueursSontListesAvecLeurEtatEtLeursJeux() throws Exception {
        banc.connecter("bob", "shifumi", "morpion");
        Participant alice = banc.connecter("alice", "morpion");
        banc.plateforme.definirDisponibilite(alice.joueur(), true);

        List<InfoJoueur> liste = banc.plateforme.listerJoueurs();
        assertEquals(List.of("alice", "bob"), liste.stream().map(InfoJoueur::pseudo).toList());
        assertEquals(EtatJoueur.DISPONIBLE, liste.get(0).etat());
        assertEquals(EtatJoueur.CONNECTE, liste.get(1).etat());
        assertEquals(Set.of("shifumi", "morpion"), liste.get(1).jeux());
    }

    @Test
    void lesJeuxInconnusDuServeurSontIgnores() throws Exception {
        Participant alice = banc.connecter("alice", "shifumi", "poker");
        assertEquals(Set.of("shifumi"), alice.joueur().jeux());
    }

    @Test
    void refuseUnPseudoInvalideOuDejaPris() throws Exception {
        banc.connecter("alice");
        assertEquals(CodeErreur.PSEUDO_PRIS, assertThrows(ErreurPlateforme.class, () -> banc.connecter("ALICE")).code());
        for (String invalide : List.of("", "a b", "é", "x".repeat(21), "a;b")) {
            assertEquals(CodeErreur.PSEUDO_INVALIDE,
                    assertThrows(ErreurPlateforme.class, () -> banc.connecter(invalide)).code(), invalide);
        }
        assertEquals(1, banc.plateforme.listerJoueurs().size());
    }

    @Test
    void lesChangementsDePresenceSontDiffusesAuxAutres() throws Exception {
        Participant alice = banc.connecter("alice", "shifumi");
        Participant bob = banc.connecter("bob", "shifumi");

        Message arrivee = alice.canal().attendre(m -> m.est(Verbes.PRESENCE) && m.argument(0).equals("bob"), "arrivée de bob");
        assertEquals("CONNECTE", arrivee.argument(1));

        banc.plateforme.definirDisponibilite(bob.joueur(), true);
        Message dispo = alice.canal().attendre(
                m -> m.est(Verbes.PRESENCE) && m.argument(0).equals("bob") && m.argument(1).equals("DISPONIBLE"),
                "bob disponible");
        assertEquals("shifumi", dispo.argument(2));
    }

    @Test
    void redevenirIndisponibleEstPrisEnCompte() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        banc.plateforme.definirDisponibilite(alice.joueur(), false);
        assertEquals(EtatJoueur.CONNECTE, banc.plateforme.listerJoueurs().get(0).etat());
    }

    @Test
    void unDepartEstDiffuseEtLeJoueurDisparaitDeLaListe() throws Exception {
        Participant alice = banc.connecter("alice");
        Participant bob = banc.connecter("bob");

        banc.plateforme.deconnecter(bob.joueur(), "QUITTER");

        Message depart = alice.canal().attendre(Verbes.DEPART);
        assertEquals("bob", depart.argument(0));
        assertEquals(List.of("alice"), banc.plateforme.listerJoueurs().stream().map(InfoJoueur::pseudo).toList());
        assertTrue(bob.canal().estFerme());
        assertFalse(bob.joueur().estConnecte());
    }

    @Test
    void deconnecterDeuxFoisNeDiffuseQuUnSeulDepart() throws Exception {
        Participant alice = banc.connecter("alice");
        Participant bob = banc.connecter("bob");
        banc.plateforme.deconnecter(bob.joueur(), "QUITTER");
        banc.plateforme.deconnecter(bob.joueur(), "QUITTER");
        assertEquals(1, alice.canal().compter(Verbes.DEPART));
    }

    @Test
    void unJoueurDeconnecteNePeutPlusAgirEtLePseudoEstLibere() throws Exception {
        Participant alice = banc.connecter("alice");
        banc.plateforme.deconnecter(alice.joueur(), "QUITTER");

        assertEquals(CodeErreur.NON_CONNECTE,
                assertThrows(ErreurPlateforme.class, () -> banc.plateforme.definirDisponibilite(alice.joueur(), true)).code());
        banc.connecter("alice");
        assertEquals(1, banc.plateforme.listerJoueurs().size());
    }

    @Test
    void lesSignesUdpIdentifientLeJoueurParSonJeton() throws Exception {
        Participant alice = banc.connecter("alice");
        assertTrue(banc.plateforme.enregistrerSigneUdp(alice.joueur().jeton()).isPresent());
        assertTrue(banc.plateforme.enregistrerSigneUdp("jeton-inconnu").isEmpty());
    }

    @Test
    void unJoueurQuiNeDonnePlusDeSigneUdpEstDeconnecte() throws Exception {
        try (BancPlateforme rapide = new BancPlateforme(new Plateforme.Parametres(
                Duration.ofSeconds(30), Duration.ofMillis(50), Duration.ofMillis(200), Duration.ofMillis(50)))) {
            Participant alice = rapide.connecter("alice");
            Participant bob = rapide.connecter("bob");
            rapide.plateforme.enregistrerSigneUdp(alice.joueur().jeton());
            rapide.plateforme.enregistrerSigneUdp(bob.joueur().jeton());

            long fin = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (alice.joueur().estConnecte() && System.nanoTime() < fin) {
                rapide.plateforme.enregistrerSigneUdp(bob.joueur().jeton());
                Thread.sleep(20);
            }
            assertFalse(alice.joueur().estConnecte(), "alice est silencieuse");
            assertTrue(bob.joueur().estConnecte(), "bob continue à donner signe");
            assertEquals("alice", bob.canal().attendre(Verbes.DEPART).argument(0));
        }
    }

    @Test
    void sansAucunSigneUdpLeJoueurNEstJamaisEvinceParSilence() throws Exception {
        try (BancPlateforme rapide = new BancPlateforme(new Plateforme.Parametres(
                Duration.ofSeconds(30), Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(30)))) {
            Participant alice = rapide.connecter("alice");
            Thread.sleep(500);
            assertTrue(alice.joueur().estConnecte(), "UDP peut être bloqué par un pare-feu : on se fie alors à TCP");
        }
    }
}
