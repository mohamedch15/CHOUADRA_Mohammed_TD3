package fr.uha.miage.plateforme.serveur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uha.miage.plateforme.jeu.ActionInvalideException;
import fr.uha.miage.plateforme.jeu.Jeu;
import fr.uha.miage.plateforme.jeu.Partie;
import fr.uha.miage.plateforme.jeu.PartieBase;
import fr.uha.miage.plateforme.jeu.ResultatPartie;
import fr.uha.miage.plateforme.jeu.Vue;
import fr.uha.miage.plateforme.jeux.JeuxStandard;
import fr.uha.miage.plateforme.jeux.shifumi.ShifumiJeu;
import fr.uha.miage.plateforme.protocole.CodeErreur;
import fr.uha.miage.plateforme.protocole.Message;
import fr.uha.miage.plateforme.protocole.Verbes;
import fr.uha.miage.plateforme.serveur.BancPlateforme.Participant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(20)
class PartieTest {

    private BancPlateforme banc;
    private Plateforme plateforme;

    @BeforeEach
    void creerBanc() {
        banc = new BancPlateforme();
        plateforme = banc.plateforme;
    }

    @AfterEach
    void fermerBanc() {
        banc.close();
    }

    private String lancer(Participant invitant, Participant invite, String jeu) throws Exception {
        String id = plateforme.inviter(invitant.joueur(), invite.pseudo(), jeu).idInvitation();
        plateforme.accepter(invite.joueur(), id);
        return invitant.canal().attendre(Verbes.PARTIE_DEBUT).argument(0);
    }

    private CodeErreur codeDe(Participant joueur, String idPartie, String action) {
        return assertThrows(ErreurPlateforme.class, () -> plateforme.jouer(joueur.joueur(), idPartie, action)).code();
    }

    @Test
    void uneManchePuisUnePartieCompleteJusquAuResultat() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        String partie = lancer(alice, bob, "shifumi");

        for (int manche = 0; manche < 2; manche++) {
            plateforme.jouer(alice.joueur(), partie, "PIERRE");
            plateforme.jouer(bob.joueur(), partie, "CISEAUX");
        }

        Message finAlice = alice.canal().attendre(Verbes.FIN_PARTIE);
        Message finBob = bob.canal().attendre(Verbes.FIN_PARTIE);
        assertEquals("VICTOIRE", finAlice.argument(1));
        assertEquals("DEFAITE", finBob.argument(1));
        assertEquals("NORMALE", finAlice.argument(2));
        assertTrue(finAlice.argument(3).contains("alice"));
        assertEquals(EtatJoueur.DISPONIBLE, alice.joueur().etat());
        assertEquals(EtatJoueur.DISPONIBLE, bob.joueur().etat());
        assertEquals(0, plateforme.nombrePartiesEnCours());
        assertEquals(CodeErreur.PARTIE_TERMINEE, codeDe(bob, partie, "PIERRE"), "action tardive après la fin");
    }

    @Test
    void chaqueActionEnvoieSaVueAuxDeuxJoueursSansRevelerLesChoixCaches() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        String partie = lancer(alice, bob, "shifumi");

        plateforme.jouer(alice.joueur(), partie, "PIERRE");

        Message vueBob = bob.canal().attendre(m -> m.est(Verbes.VUE) && m.argument(2).contains("adversaireADecide=oui"),
                "vue de bob après le choix d'alice");
        assertFalse(vueBob.encoder().contains("PIERRE"), "le choix d'alice reste caché à bob");
        Message vueAlice = alice.canal().attendre(m -> m.est(Verbes.VUE) && m.argument(2).contains("monChoix=PIERRE"),
                "vue d'alice");
        assertEquals("false", vueAlice.argument(1), "alice a joué, elle attend");
        for (Message message : bob.canal().historique()) {
            assertFalse(message.encoder().contains("PIERRE"), "aucun message vers bob ne contient le choix : " + message);
        }
    }

    @Test
    void refuseLesActionsInvalidesLesIntrusEtLesPartiesInconnues() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "morpion");
        Participant bob = banc.connecterDisponible("bob", "morpion");
        Participant clara = banc.connecterDisponible("clara", "morpion");
        String partie = lancer(alice, bob, "morpion");

        assertEquals(CodeErreur.ACTION_INVALIDE, codeDe(bob, partie, "A1"), "ce n'est pas le tour de bob");
        assertEquals(CodeErreur.ACTION_INVALIDE, codeDe(alice, partie, "Z9"));
        assertEquals(CodeErreur.NON_PARTICIPANT, codeDe(clara, partie, "A1"));
        assertEquals(CodeErreur.PARTIE_INCONNUE, codeDe(alice, "p999", "A1"));
        plateforme.jouer(alice.joueur(), partie, "A1");
        assertEquals(CodeErreur.ACTION_INVALIDE, codeDe(bob, partie, "A1"), "case occupée");
        assertEquals(EtatJoueur.EN_PARTIE, alice.joueur().etat(), "les erreurs n'interrompent pas la partie");
        plateforme.jouer(bob.joueur(), partie, "B1");
    }

    @Test
    void unAbandonTermineLaPartieEtLiberelesJoueurs() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "morpion");
        Participant bob = banc.connecterDisponible("bob", "morpion");
        String partie = lancer(alice, bob, "morpion");

        plateforme.abandonner(alice.joueur(), partie);

        Message finBob = bob.canal().attendre(Verbes.FIN_PARTIE);
        assertEquals("VICTOIRE", finBob.argument(1));
        assertEquals("ABANDON", finBob.argument(2));
        assertEquals("DEFAITE", alice.canal().attendre(Verbes.FIN_PARTIE).argument(1));
        assertEquals(EtatJoueur.DISPONIBLE, alice.joueur().etat());
        assertEquals(EtatJoueur.DISPONIBLE, bob.joueur().etat());
        assertEquals(CodeErreur.PARTIE_TERMINEE,
                assertThrows(ErreurPlateforme.class, () -> plateforme.abandonner(bob.joueur(), partie)).code());
        assertEquals(CodeErreur.PARTIE_TERMINEE, codeDe(bob, partie, "A1"));
        lancer(bob, alice, "morpion");
    }

    @Test
    void laDeconnexionEnPartiePrevientLAdversaireEtLeLibere() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "morpion");
        Participant bob = banc.connecterDisponible("bob", "morpion");
        String partie = lancer(alice, bob, "morpion");

        plateforme.deconnecter(alice.joueur(), "CONNEXION_PERDUE");

        Message fin = bob.canal().attendre(Verbes.FIN_PARTIE);
        assertEquals("VICTOIRE", fin.argument(1));
        assertEquals("DECONNEXION", fin.argument(2));
        assertTrue(fin.argument(3).contains("alice"));
        assertEquals("alice", bob.canal().attendre(Verbes.DEPART).argument(0));
        assertEquals(EtatJoueur.DISPONIBLE, bob.joueur().etat());
        assertEquals(0, plateforme.nombrePartiesEnCours());
        assertEquals(CodeErreur.PARTIE_TERMINEE, codeDe(bob, partie, "A1"));
    }

    @Test
    void unMatchNulRendLesDeuxIssuesNulles() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "morpion");
        Participant bob = banc.connecterDisponible("bob", "morpion");
        String partie = lancer(alice, bob, "morpion");
        String[] coups = {"A1", "B1", "C1", "B2", "A2", "C2", "B3", "A3", "C3"};
        for (int i = 0; i < coups.length; i++) {
            plateforme.jouer(i % 2 == 0 ? alice.joueur() : bob.joueur(), partie, coups[i]);
        }
        assertEquals("NUL", alice.canal().attendre(Verbes.FIN_PARTIE).argument(1));
        assertEquals("NUL", bob.canal().attendre(Verbes.FIN_PARTIE).argument(1));
    }

    @Test
    void deuxPartiesSimultaneesRestentIndependantes() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi", "morpion");
        Participant bob = banc.connecterDisponible("bob", "shifumi", "morpion");
        Participant clara = banc.connecterDisponible("clara", "shifumi", "morpion");
        Participant denis = banc.connecterDisponible("denis", "shifumi", "morpion");
        String p1 = lancer(alice, bob, "shifumi");
        String p2 = lancer(clara, denis, "morpion");
        assertNotEquals(p1, p2);
        assertEquals(2, plateforme.nombrePartiesEnCours());

        plateforme.jouer(alice.joueur(), p1, "PIERRE");
        plateforme.jouer(clara.joueur(), p2, "B2");
        assertEquals(CodeErreur.NON_PARTICIPANT, codeDe(clara, p1, "PIERRE"));
        assertEquals(CodeErreur.NON_PARTICIPANT, codeDe(alice, p2, "A1"));
        plateforme.jouer(bob.joueur(), p1, "FEUILLE");
        plateforme.jouer(denis.joueur(), p2, "A1");

        for (Message message : concat(clara, denis)) {
            if (message.est(Verbes.VUE)) {
                assertEquals(p2, message.argument(0), "clara et denis ne reçoivent que la partie 2");
            }
        }
        for (Message message : concat(alice, bob)) {
            if (message.est(Verbes.VUE)) {
                assertEquals(p1, message.argument(0), "alice et bob ne reçoivent que la partie 1");
            }
        }

        plateforme.abandonner(clara.joueur(), p2);
        assertEquals(1, plateforme.nombrePartiesEnCours());
        assertEquals(EtatJoueur.EN_PARTIE, alice.joueur().etat(), "la partie 1 continue");
        plateforme.jouer(alice.joueur(), p1, "PIERRE");
        plateforme.jouer(bob.joueur(), p1, "CISEAUX");
        assertEquals(0, alice.canal().compter(Verbes.FIN_PARTIE));
    }

    private static List<Message> concat(Participant a, Participant b) {
        List<Message> tous = new ArrayList<>(a.canal().historique());
        tous.addAll(b.canal().historique());
        return tous;
    }

    /** Jeu de test sans aucun lien avec les jeux fournis : prouve que la plateforme reste générique. */
    private static final class JeuFragile implements Jeu {
        @Override
        public String id() {
            return "fragile";
        }

        @Override
        public String nom() {
            return "Jeu fragile";
        }

        @Override
        public int nombreJoueurs() {
            return 2;
        }

        @Override
        public Partie creerPartie(List<String> joueurs) {
            return new PartieBase(joueurs) {
                @Override
                protected void appliquer(String joueur, String action) {
                    if (action.equals("boom")) {
                        throw new IllegalStateException("bug du jeu");
                    }
                    terminer(ResultatPartie.victoire(joueurs(), joueur, joueur + " a gagné."));
                }

                @Override
                public Vue vue(String joueur) {
                    return new Vue(!estTerminee(), "");
                }
            };
        }
    }

    @Test
    void uneErreurInterneDUnJeuNAffecteNiLaPlateformeNiLesAutresParties() throws Exception {
        try (BancPlateforme special = new BancPlateforme(
                CatalogueJeux.de(new JeuFragile(), new ShifumiJeu()), Plateforme.Parametres.parDefaut())) {
            Plateforme p = special.plateforme;
            Participant alice = special.connecterDisponible("alice", "fragile", "shifumi");
            Participant bob = special.connecterDisponible("bob", "fragile", "shifumi");
            Participant clara = special.connecterDisponible("clara", "shifumi");
            Participant denis = special.connecterDisponible("denis", "shifumi");
            String fragile = p.inviter(alice.joueur(), "bob", "fragile").idInvitation();
            p.accepter(bob.joueur(), fragile);
            String idFragile = alice.canal().attendre(Verbes.PARTIE_DEBUT).argument(0);
            p.accepter(denis.joueur(), p.inviter(clara.joueur(), "denis", "shifumi").idInvitation());
            String idShifumi = clara.canal().attendre(Verbes.PARTIE_DEBUT).argument(0);

            ErreurPlateforme erreur = assertThrows(ErreurPlateforme.class, () -> p.jouer(alice.joueur(), idFragile, "boom"));
            assertEquals(CodeErreur.ERREUR_INTERNE, erreur.code());

            p.jouer(clara.joueur(), idShifumi, "PIERRE");
            p.jouer(alice.joueur(), idFragile, "ok");
            assertEquals("VICTOIRE", alice.canal().attendre(Verbes.FIN_PARTIE).argument(1));
            assertEquals(EtatJoueur.EN_PARTIE, clara.joueur().etat());
        }
    }
}
