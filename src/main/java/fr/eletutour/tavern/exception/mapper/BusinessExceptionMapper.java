package fr.eletutour.tavern.exception.mapper;

import jakarta.ws.rs.ext.Provider;

import fr.eletutour.tavern.exception.business.BusinessException;
import fr.eletutour.tavern.exception.business.TavernException;
import fr.eletutour.tavern.exception.model.Problem;

/**
 * Le mapper de toutes les erreurs métier : ce qui est générique est centralisé, ce qui est spécifique passe par le
 * pattern matching.
 */
@Provider
public class BusinessExceptionMapper extends AbstractExceptionMapper<BusinessException> {

    @Override
    protected int status(BusinessException ex) {
        return ex.getStatus();
    }

    @Override
    protected String title(BusinessException ex) {
        return ex.getTitle();
    }

    @Override
    protected String type(BusinessException ex) {
        return switch (ex) {
            case TavernException te -> "urn:falling-whale:problem:" + te.getError().getCode();
            default -> "about:blank";
        };
    }

    @Override
    protected void enrich(Problem problem, BusinessException ex) {
        switch (ex) {
            case TavernException te -> problem.with("code", te.getError().getCode()).additional(te.getContext());
            default -> {
                // rien
            }
        }
    }
}
