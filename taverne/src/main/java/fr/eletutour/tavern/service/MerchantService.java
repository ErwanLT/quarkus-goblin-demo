package fr.eletutour.tavern.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import fr.eletutour.tavern.client.Delivery;
import fr.eletutour.tavern.client.DeliveryRequest;
import fr.eletutour.tavern.client.MerchantGuildClient;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 * Les relations avec la guilde des marchands : un appel distant, protégé par toutes les vieilles lois de la taverne.
 * Un délai maximal, deux nouvelles tentatives, un disjoncteur qui cesse d'envoyer des coursiers après trop d'échecs
 * (sans réessayer tant qu'il est ouvert), et un repli quand le marchand ne vient pas.
 */
@ApplicationScoped
public class MerchantService {

    private static final Logger LOG = Logger.getLogger(MerchantService.class);

    /** Nom du disjoncteur de la guilde, pour suivre son état depuis la salle. */
    public static final String DISJONCTEUR = "guilde-des-marchands";

    @Inject
    @RestClient
    MerchantGuildClient merchantGuild;

    @Timeout(2000)
    // un disjoncteur ouvert répond tout de suite : inutile de réessayer, on passe directement au repli
    @Retry(maxRetries = 2, delay = 200, abortOn = CircuitBreakerOpenException.class)
    @CircuitBreaker(requestVolumeThreshold = 6, failureRatio = 0.5, delay = 10_000)
    @CircuitBreakerName(MerchantService.DISJONCTEUR)
    @Fallback(fallbackMethod = "marchandAbsent")
    @WithSpan("commander-a-la-guilde")
    public Delivery commander(@SpanAttribute("tavern.ingredient") String ingredient,
            @SpanAttribute("tavern.quantity") int quantity) {
        return merchantGuild.deliver(new DeliveryRequest(ingredient, quantity));
    }

    Delivery marchandAbsent(String ingredient, int quantity) {
        LOG.warnf("La guilde des marchands n'a pas livré %d %s", quantity, ingredient);
        return new Delivery(ingredient, 0, null);
    }
}
