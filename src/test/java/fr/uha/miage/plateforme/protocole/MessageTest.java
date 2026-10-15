package fr.uha.miage.plateforme.protocole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MessageTest {

    @Test
    void encodeUnVerbeSansArgument() {
        assertEquals("DISPONIBLE", Message.of("DISPONIBLE").encoder());
    }

    @Test
    void encodeEtDecodeSontInverses() {
        Message message = Message.of("INVITER", "alice", "shifumi");
        assertEquals("INVITER alice shifumi", message.encoder());
        assertEquals(message, Message.decoder(message.encoder()));
    }

    @Test
    void lesCaracteresSpeciauxSurvivent() {
        String delicat = "é à+ %20 \"x\"; a=b\nligne2 ~";
        Message message = Message.of("VUE", "p1", delicat);
        String ligne = message.encoder();
        assertFalse(ligne.contains("\n"), "une ligne ne contient jamais de saut de ligne");
        assertEquals(3, ligne.split(" ").length, "les espaces sont encodés");
        assertEquals(delicat, Message.decoder(ligne).argument(1));
    }

    @Test
    void unArgumentVideEstConserve() {
        Message message = Message.of("VUE", "", "x", "");
        Message relu = Message.decoder(message.encoder());
        assertEquals(List.of("", "x", ""), relu.arguments());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "inviter alice", "INVITER  alice", " INVITER", "INVITER alice ", "A%ZZ b",
            "VERBE_TROP_LONG_VERBE_TROP_LONG_VERBE_TROP_LONG x", "1ABC", "ABC%20 DEF"})
    void refuseLesMessagesMalformes(String ligne) {
        assertThrows(MessageMalformeException.class, () -> Message.decoder(ligne));
    }

    @Test
    void refuseLesMessagesTropLongs() {
        String ligne = "VUE " + "a".repeat(Message.TAILLE_MAX);
        assertThrows(MessageMalformeException.class, () -> Message.decoder(ligne));
        assertThrows(MessageMalformeException.class, () -> Message.of("VUE", "a".repeat(Message.TAILLE_MAX)).encoder());
    }

    @Test
    void signaleLesArgumentsManquantsEtNonEntiers() {
        Message message = Message.of("BIENVENUE", "alice", "abc");
        assertEquals("alice", message.argument(0));
        assertThrows(MessageMalformeException.class, () -> message.argument(2));
        assertThrows(MessageMalformeException.class, () -> message.argumentEntier(1));
        assertEquals(42, Message.of("X", 42).argumentEntier(0));
    }

    @Test
    void testeLeVerbe() {
        assertTrue(Message.of("OK").est(Verbes.OK));
        assertFalse(Message.of("OK").est(Verbes.ERREUR));
    }
}
