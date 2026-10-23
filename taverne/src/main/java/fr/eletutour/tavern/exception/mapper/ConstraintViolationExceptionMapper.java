package fr.eletutour.tavern.exception.mapper;

import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.ext.Provider;

import fr.eletutour.tavern.exception.model.Problem;

/**
 * Les commandes mal rédigées : chaque violation de contrainte est listée.
 */
@Provider
public class ConstraintViolationExceptionMapper extends AbstractExceptionMapper<ConstraintViolationException> {

    @Override
    protected int status(ConstraintViolationException ex) {
        return 400;
    }

    @Override
    protected String title(ConstraintViolationException ex) {
        return "Erreur de validation des données";
    }

    @Override
    protected String type(ConstraintViolationException ex) {
        return "urn:falling-whale:problem:validation";
    }

    @Override
    protected String detail(ConstraintViolationException ex) {
        return "La commande transmise au comptoir est incomplète ou incorrecte.";
    }

    @Override
    protected void enrich(Problem problem, ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(ConstraintViolationExceptionMapper::formatViolation)
                .sorted()
                .toList();
        problem.additional(Map.of("errors", errors));
    }

    private static String formatViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        // "creerIngredient.request.name" -> "name"
        String field = path.substring(path.lastIndexOf('.') + 1);
        return field + " : " + violation.getMessage();
    }
}
