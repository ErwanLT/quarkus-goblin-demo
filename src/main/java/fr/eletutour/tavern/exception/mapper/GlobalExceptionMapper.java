package fr.eletutour.tavern.exception.mapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;

/**
 * Le filet de sécurité : même l'inconnu a sa place sur la carte. Les exceptions injectées par Quarkus Goblin
 * finissent ici, en 500 {@code application/problem+json}.
 */
@Provider
public class GlobalExceptionMapper extends AbstractExceptionMapper<Throwable> {

    @Override
    protected int status(Throwable ex) {
        if (ex instanceof WebApplicationException we) {
            return we.getResponse().getStatus();
        }
        return 500;
    }

    @Override
    protected String title(Throwable ex) {
        if (ex instanceof WebApplicationException) {
            return "Erreur lors du traitement de la requête";
        }
        return "Erreur interne du serveur";
    }

    @Override
    protected String detail(Throwable ex) {
        if (ex instanceof WebApplicationException we) {
            return we.getMessage();
        }
        return "Une erreur inattendue est survenue lors du traitement de votre requête.";
    }
}
