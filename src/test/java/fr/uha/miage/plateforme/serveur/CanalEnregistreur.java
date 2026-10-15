package fr.uha.miage.plateforme.serveur;

import static org.junit.jupiter.api.Assertions.fail;

import fr.uha.miage.plateforme.protocole.Message;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/** Canal de test : mémorise tout ce que la plateforme envoie à un joueur. */
final class CanalEnregistreur implements CanalJoueur {

    private static final long ATTENTE_MS = 5000;

    private final BlockingQueue<Message> arrivees = new LinkedBlockingQueue<>();
    private final List<Message> tampon = new ArrayList<>();
    private final List<Message> historique = new CopyOnWriteArrayList<>();
    private volatile boolean ferme;

    @Override
    public boolean envoyer(Message message) {
        if (ferme) {
            return false;
        }
        historique.add(message);
        arrivees.add(message);
        return true;
    }

    @Override
    public void fermer() {
        ferme = true;
    }

    boolean estFerme() {
        return ferme;
    }

    List<Message> historique() {
        return List.copyOf(historique);
    }

    long compter(String verbe) {
        return historique.stream().filter(m -> m.est(verbe)).count();
    }

    Message attendre(String verbe) {
        return attendre(m -> m.est(verbe), verbe);
    }

    /** Attend et consomme le premier message qui satisfait le prédicat. */
    Message attendre(Predicate<Message> filtre, String description) {
        long fin = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ATTENTE_MS);
        try {
            while (true) {
                Iterator<Message> it = tampon.iterator();
                while (it.hasNext()) {
                    Message m = it.next();
                    if (filtre.test(m)) {
                        it.remove();
                        return m;
                    }
                }
                long reste = fin - System.nanoTime();
                Message m = reste <= 0 ? null : arrivees.poll(reste, TimeUnit.NANOSECONDS);
                if (m == null) {
                    return fail("Message attendu mais non reçu : " + description + " ; reçus : " + historique);
                }
                tampon.add(m);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fail("Interrompu en attendant " + description);
        }
    }

    /** Vérifie qu'aucun message correspondant n'arrive pendant un court délai. */
    boolean aucunMessage(Predicate<Message> filtre, long delaiMs) throws InterruptedException {
        Thread.sleep(delaiMs);
        return historique.stream().noneMatch(filtre);
    }
}
