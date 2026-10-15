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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(20)
class InvitationTest {

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

    private CodeErreur codeDe(ThrowingRunnable action) {
        return assertThrows(ErreurPlateforme.class, action::run).code();
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private Message attendreResultat(Participant participant, String id, String etat) {
        return participant.canal().attendre(
                m -> m.est(Verbes.INVITATION_RESULTAT) && m.argument(0).equals(id) && m.argument(1).equals(etat),
                "INVITATION_RESULTAT " + id + " " + etat);
    }

    @Test
    void lInviteRecoitLIdentiteDeLInvitantEtLeJeuPropose() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");

        var resultat = plateforme.inviter(alice.joueur(), "BOB", "shifumi");

        assertFalse(resultat.partieDemarree());
        Message invitation = bob.canal().attendre(Verbes.INVITATION);
        assertEquals(resultat.idInvitation(), invitation.argument(0));
        assertEquals("alice", invitation.argument(1));
        assertEquals("shifumi", invitation.argument(2));
        assertEquals("30", invitation.argument(3));
        assertEquals(EtatJoueur.DISPONIBLE, alice.joueur().etat(), "attendre une réponse n'engage personne");
    }

    @Test
    void lAcceptationLanceLaPartieEtMetAJourLaDisponibilite() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        Participant clara = banc.connecterDisponible("clara", "shifumi");
        String id = plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation();

        plateforme.accepter(bob.joueur(), id);

        attendreResultat(alice, id, "ACCEPTEE");
        for (Participant joueur : new Participant[] {alice, bob}) {
            Message debut = joueur.canal().attendre(Verbes.PARTIE_DEBUT);
            assertEquals("shifumi", debut.argument(1));
            assertEquals("alice,bob", debut.argument(2));
            Message vue = joueur.canal().attendre(Verbes.VUE);
            assertEquals(debut.argument(0), vue.argument(0));
            assertEquals("true", vue.argument(1));
            assertEquals(EtatJoueur.EN_PARTIE, joueur.joueur().etat());
        }
        assertEquals(1, plateforme.nombrePartiesEnCours());
        Message presence = clara.canal().attendre(
                m -> m.est(Verbes.PRESENCE) && m.argument(0).equals("bob") && m.argument(1).equals("EN_PARTIE"),
                "bob EN_PARTIE");
        assertEquals("shifumi", presence.argument(2));
    }

    @Test
    void unJoueurEnPartieNePeutNiInviterNiEtreInvite() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        Participant clara = banc.connecterDisponible("clara", "shifumi");
        plateforme.accepter(bob.joueur(), plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation());

        assertEquals(CodeErreur.JOUEUR_INDISPONIBLE, codeDe(() -> plateforme.inviter(clara.joueur(), "alice", "shifumi")));
        assertEquals(CodeErreur.ETAT_INCOMPATIBLE, codeDe(() -> plateforme.inviter(alice.joueur(), "clara", "shifumi")));
        assertEquals(CodeErreur.ETAT_INCOMPATIBLE, codeDe(() -> plateforme.definirDisponibilite(alice.joueur(), false)));
    }

    @Test
    void leRefusPrevientLInvitantEtLaisseLesDeuxDisponibles() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        String id = plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation();

        plateforme.refuser(bob.joueur(), id);

        assertTrue(attendreResultat(alice, id, "REFUSEE").argument(2).contains("bob"));
        assertEquals(EtatJoueur.DISPONIBLE, alice.joueur().etat());
        assertEquals(EtatJoueur.DISPONIBLE, bob.joueur().etat());
        assertEquals(0, plateforme.nombrePartiesEnCours());
        plateforme.inviter(alice.joueur(), "bob", "shifumi");
    }

    @Test
    void lAnnulationParLInvitantPrevientLInviteEtRendLInvitationInvalide() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        String id = plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation();

        plateforme.annuler(alice.joueur(), id);

        attendreResultat(bob, id, "ANNULEE");
        ErreurPlateforme erreur = assertThrows(ErreurPlateforme.class, () -> plateforme.accepter(bob.joueur(), id));
        assertEquals(CodeErreur.INVITATION_INVALIDE, erreur.code());
        assertTrue(erreur.getMessage().contains("annulée"), erreur.getMessage());
        assertEquals(0, plateforme.nombrePartiesEnCours());
    }

    @Test
    void uneInvitationSansReponseExpireEtPrevientLesDeuxJoueurs() throws Exception {
        try (BancPlateforme rapide = new BancPlateforme(new Plateforme.Parametres(
                Duration.ofMillis(150), Duration.ofSeconds(5), Duration.ofSeconds(20), Duration.ofSeconds(5)))) {
            Participant alice = rapide.connecterDisponible("alice", "shifumi");
            Participant bob = rapide.connecterDisponible("bob", "shifumi");
            String id = rapide.plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation();

            attendreResultat(alice, id, "EXPIREE");
            attendreResultat(bob, id, "EXPIREE");
            ErreurPlateforme erreur = assertThrows(ErreurPlateforme.class,
                    () -> rapide.plateforme.accepter(bob.joueur(), id));
            assertEquals(CodeErreur.INVITATION_INVALIDE, erreur.code());
            assertTrue(erreur.getMessage().contains("expirée"), erreur.getMessage());
            rapide.plateforme.inviter(alice.joueur(), "bob", "shifumi");
        }
    }

    @Test
    void lInviteQuiDevientIndisponibleInvalideLInvitation() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        String id = plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation();

        plateforme.definirDisponibilite(bob.joueur(), false);

        assertTrue(attendreResultat(alice, id, "INVALIDEE").argument(2).contains("bob"));
        assertEquals(CodeErreur.INVITATION_INVALIDE, codeDe(() -> plateforme.accepter(bob.joueur(), id)));
    }

    @Test
    void lInvitantQuiDevientIndisponibleInvalideSonInvitation() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        String id = plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation();

        plateforme.definirDisponibilite(alice.joueur(), false);

        attendreResultat(bob, id, "INVALIDEE");
        assertEquals(0, plateforme.nombrePartiesEnCours());
    }

    @Test
    void leDepartDUnJoueurPrevientLesAutresParticipantsDeSesInvitations() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        Participant clara = banc.connecterDisponible("clara", "shifumi");
        String versBob = plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation();
        String versClara = plateforme.inviter(bob.joueur(), "clara", "shifumi").idInvitation();

        plateforme.deconnecter(bob.joueur(), "QUITTER");

        attendreResultat(alice, versBob, "INVALIDEE");
        attendreResultat(clara, versClara, "INVALIDEE");
        assertEquals(CodeErreur.INVITATION_INVALIDE, codeDe(() -> plateforme.accepter(clara.joueur(), versClara)));
    }

    @Test
    void repondreAUneInvitationInconnueOuQuiNeNousEstPasDestineeEstRefuse() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        Participant clara = banc.connecterDisponible("clara", "shifumi");
        String id = plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation();

        assertEquals(CodeErreur.INVITATION_INVALIDE, codeDe(() -> plateforme.accepter(bob.joueur(), "i999")));
        assertEquals(CodeErreur.NON_AUTORISE, codeDe(() -> plateforme.accepter(clara.joueur(), id)));
        assertEquals(CodeErreur.NON_AUTORISE, codeDe(() -> plateforme.refuser(clara.joueur(), id)));
        assertEquals(CodeErreur.NON_AUTORISE, codeDe(() -> plateforme.annuler(bob.joueur(), id)));
        assertEquals(CodeErreur.NON_AUTORISE, codeDe(() -> plateforme.accepter(alice.joueur(), id)));
        plateforme.accepter(bob.joueur(), id);
        assertEquals(CodeErreur.INVITATION_INVALIDE, codeDe(() -> plateforme.accepter(bob.joueur(), id)),
                "une invitation ne s'accepte qu'une fois");
    }

    @Test
    void refuseLesInvitationsImpossibles() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi", "morpion");
        Participant bob = banc.connecterDisponible("bob", "morpion");
        Participant absent = banc.connecter("absent", "shifumi");
        Participant discret = banc.connecter("discret", "shifumi");
        plateforme.definirDisponibilite(absent.joueur(), true);

        assertEquals(CodeErreur.AUTO_INVITATION, codeDe(() -> plateforme.inviter(alice.joueur(), "alice", "shifumi")));
        assertEquals(CodeErreur.JOUEUR_INCONNU, codeDe(() -> plateforme.inviter(alice.joueur(), "fantome", "shifumi")));
        assertEquals(CodeErreur.JEU_INCONNU, codeDe(() -> plateforme.inviter(alice.joueur(), "bob", "poker")));
        assertEquals(CodeErreur.JEU_NON_SUPPORTE, codeDe(() -> plateforme.inviter(alice.joueur(), "bob", "shifumi")),
                "bob n'a pas le jeu côté client");
        assertEquals(CodeErreur.JOUEUR_INDISPONIBLE, codeDe(() -> plateforme.inviter(alice.joueur(), "discret", "shifumi")));
        assertEquals(CodeErreur.ETAT_INCOMPATIBLE, codeDe(() -> plateforme.inviter(discret.joueur(), "alice", "shifumi")),
                "discret ne s'est pas déclaré disponible");
        assertEquals(CodeErreur.JEU_NON_SUPPORTE, codeDe(() -> plateforme.inviter(bob.joueur(), "alice", "shifumi")),
                "bob n'a pas ce jeu non plus");
    }

    @Test
    void unJoueurNAQuUneInvitationSortanteALaFois() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        banc.connecterDisponible("bob", "shifumi");
        banc.connecterDisponible("clara", "shifumi");
        String id = plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation();

        assertEquals(CodeErreur.INVITATION_EN_COURS, codeDe(() -> plateforme.inviter(alice.joueur(), "clara", "shifumi")));
        plateforme.annuler(alice.joueur(), id);
        plateforme.inviter(alice.joueur(), "clara", "shifumi");
    }

    @Test
    void deuxInvitationsCroiseesPourLeMemeJeuLancentUneSeulePartie() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        plateforme.inviter(alice.joueur(), "bob", "shifumi");

        var croisee = plateforme.inviter(bob.joueur(), "alice", "shifumi");

        assertTrue(croisee.partieDemarree());
        assertEquals(1, plateforme.nombrePartiesEnCours());
        assertEquals("alice,bob", alice.canal().attendre(Verbes.PARTIE_DEBUT).argument(2),
                "le premier invitant garde la première place");
        bob.canal().attendre(Verbes.PARTIE_DEBUT);
        assertEquals(EtatJoueur.EN_PARTIE, alice.joueur().etat());
        assertEquals(EtatJoueur.EN_PARTIE, bob.joueur().etat());
    }

    @Test
    void deuxInvitationsCroiseesPourDesJeuxDifferentsRestentUnChoixExclusif() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi", "morpion");
        Participant bob = banc.connecterDisponible("bob", "shifumi", "morpion");
        String versBob = plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation();
        String versAlice = plateforme.inviter(bob.joueur(), "alice", "morpion").idInvitation();
        assertEquals(0, plateforme.nombrePartiesEnCours());

        plateforme.accepter(bob.joueur(), versBob);

        assertEquals(1, plateforme.nombrePartiesEnCours());
        attendreResultat(alice, versAlice, "INVALIDEE");
        assertEquals(CodeErreur.INVITATION_INVALIDE, codeDe(() -> plateforme.accepter(alice.joueur(), versAlice)));
        assertEquals("shifumi", alice.canal().attendre(Verbes.PARTIE_DEBUT).argument(1));
    }

    @Test
    void accepterUneInvitationInvalideLesAutresInvitationsRecuesDuMemeJoueur() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        Participant clara = banc.connecterDisponible("clara", "shifumi");
        Participant denis = banc.connecterDisponible("denis", "shifumi");
        String deAlice = plateforme.inviter(alice.joueur(), "clara", "shifumi").idInvitation();
        String deBob = plateforme.inviter(bob.joueur(), "clara", "shifumi").idInvitation();
        plateforme.inviter(denis.joueur(), "bob", "shifumi");

        plateforme.accepter(clara.joueur(), deAlice);

        attendreResultat(bob, deBob, "INVALIDEE");
        assertEquals(CodeErreur.INVITATION_INVALIDE, codeDe(() -> plateforme.accepter(clara.joueur(), deBob)));
        assertEquals(1, plateforme.nombrePartiesEnCours());
        assertEquals(EtatJoueur.DISPONIBLE, bob.joueur().etat(), "bob n'est pas engagé");
        assertEquals(EtatJoueur.DISPONIBLE, denis.joueur().etat());
    }

    @Test
    void accepterUneAutreInvitationAnnuleLaSienne() throws Exception {
        Participant alice = banc.connecterDisponible("alice", "shifumi");
        Participant bob = banc.connecterDisponible("bob", "shifumi");
        Participant clara = banc.connecterDisponible("clara", "shifumi");
        String versBob = plateforme.inviter(alice.joueur(), "bob", "shifumi").idInvitation();
        String versAlice = plateforme.inviter(clara.joueur(), "alice", "shifumi").idInvitation();

        plateforme.accepter(alice.joueur(), versAlice);

        attendreResultat(bob, versBob, "INVALIDEE");
        assertEquals(CodeErreur.INVITATION_INVALIDE, codeDe(() -> plateforme.accepter(bob.joueur(), versBob)));
        assertEquals(EtatJoueur.EN_PARTIE, clara.joueur().etat());
        assertEquals(EtatJoueur.DISPONIBLE, bob.joueur().etat());
    }
}
