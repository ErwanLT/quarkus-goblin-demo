package fr.eletutour.tavern.exception.mapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.jboss.logging.Logger;

import fr.eletutour.tavern.exception.model.Problem;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;

/**
 * Une seule encre pour dessiner la carte : chaque mapper ne décrit que ce qui lui est propre (statut, titre,
 * enrichissement), la construction de la réponse {@code application/problem+json} est commune.
 * <p>
 * Chaque problème porte le {@code traceId} de la requête, pour retrouver la trace complète dans Grafana / Tempo.
 */
public abstract class AbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

    private static final Logger LOG = Logger.getLogger(AbstractExceptionMapper.class);

    public static final String PROBLEM_JSON = "application/problem+json";

    @Context
    UriInfo uriInfo;

    @Context
    Request request;

    protected abstract int status(T ex);

    protected abstract String title(T ex);

    protected String type(T ex) {
        return "about:blank";
    }

    protected String detail(T ex) {
        return ex.getMessage();
    }

    protected void enrich(Problem problem, T ex) {
        // rien par défaut
    }

    protected Response.ResponseBuilder customize(Response.ResponseBuilder builder, T ex) {
        return builder;
    }

    @Override
    public Response toResponse(T ex) {
        Problem problem = Problem.of(status(ex), title(ex))
                .type(type(ex))
                .detail(detail(ex))
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC).toString());
        if (uriInfo != null && uriInfo.getRequestUri() != null) {
            problem.instance(uriInfo.getRequestUri().toString());
        }
        enrich(problem, ex);
        traceProblem(problem, ex);

        return customize(Response.status(problem.getStatus()), ex)
                .type(PROBLEM_JSON)
                .entity(problem)
                .build();
    }

    private void traceProblem(Problem problem, T ex) {
        Span span = Span.current();
        SpanContext spanContext = span.getSpanContext();
        if (spanContext.isValid()) {
            problem.with("traceId", spanContext.getTraceId());
        }
        String call = (request != null ? request.getMethod() + " " : "")
                + (uriInfo != null ? "/" + uriInfo.getPath().replaceFirst("^/", "") : "");
        if (problem.getStatus() >= 500) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, problem.getTitle());
            // pas de pile d'appels ici : sous le chaos, des dizaines d'erreurs injectées par seconde noieraient le reste
            LOG.errorf("%s -> %d %s : %s (%s: %s)", call, problem.getStatus(), problem.getTitle(), problem.getDetail(),
                    ex.getClass().getSimpleName(), ex.getMessage());
            LOG.debugf(ex, "%s -> %d, pile d'appels", call, problem.getStatus());
        } else {
            LOG.infof("%s -> %d %s : %s", call, problem.getStatus(), problem.getTitle(), problem.getDetail());
        }
    }
}
