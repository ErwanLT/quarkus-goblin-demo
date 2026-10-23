package fr.eletutour.tavern.service;

import java.util.concurrent.ThreadLocalRandom;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 * La cuisine : chaque portion demande un peu de temps de préparation.
 */
@ApplicationScoped
public class Kitchen {

    private static final Logger LOG = Logger.getLogger(Kitchen.class);

    @WithSpan("preparer-la-recette")
    public void preparer(@SpanAttribute("tavern.recipe") String recipe, @SpanAttribute("tavern.portions") int portions) {
        long millis = portions * ThreadLocalRandom.current().nextLong(10, 40);
        LOG.debugf("En cuisine : %d x %s (%d ms)", (Object) portions, recipe, millis);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // la commande a dépassé son délai : on laisse le comptoir répondre
            Thread.currentThread().interrupt();
        }
    }
}
