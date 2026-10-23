package fr.eletutour.tavern.exception.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import fr.eletutour.tavern.exception.model.Problem;
import io.smallrye.faulttolerance.api.RateLimitException;

/**
 * Quand les vieilles lois de la taverne s'appliquent : limite de débit, délai dépassé, disjoncteur ouvert.
 */
@Provider
public class FaultToleranceExceptionMapper extends AbstractExceptionMapper<FaultToleranceException> {

    @Override
    protected int status(FaultToleranceException ex) {
        return switch (ex) {
            case RateLimitException e -> 429;
            case TimeoutException e -> 504;
            default -> 503;
        };
    }

    @Override
    protected String title(FaultToleranceException ex) {
        return switch (ex) {
            case RateLimitException e -> "Trop de commandes";
            case TimeoutException e -> "Service trop lent";
            case CircuitBreakerOpenException e -> "Service momentanément fermé";
            case BulkheadException e -> "Comptoir saturé";
            default -> "Service indisponible";
        };
    }

    @Override
    protected String type(FaultToleranceException ex) {
        return "urn:falling-whale:problem:fault-tolerance";
    }

    @Override
    protected String detail(FaultToleranceException ex) {
        return switch (ex) {
            case RateLimitException e -> "Holà l'ami ! Laisse-moi le temps de tirer ta bière, le tonneau n'est pas infini !";
            case TimeoutException e -> "La cuisine n'a pas servi la commande à temps.";
            case CircuitBreakerOpenException e -> "Trop d'échecs récents : le comptoir reste fermé quelques instants.";
            default -> ex.getMessage();
        };
    }

    @Override
    protected void enrich(Problem problem, FaultToleranceException ex) {
        problem.with("faultTolerance", ex.getClass().getSimpleName());
    }

    @Override
    protected Response.ResponseBuilder customize(Response.ResponseBuilder builder, FaultToleranceException ex) {
        return ex instanceof RateLimitException ? builder.header("Retry-After", "10") : builder;
    }
}
