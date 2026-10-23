package fr.eletutour.tavern.observability;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import fr.eletutour.tavern.repository.RecipeRepository;

/**
 * La taverne est prête à servir quand le grimoire contient au moins une recette.
 */
@Readiness
@ApplicationScoped
public class TavernReadinessCheck implements HealthCheck {

    @Inject
    RecipeRepository recipeRepository;

    @Override
    @ActivateRequestContext
    public HealthCheckResponse call() {
        try {
            long recipes = recipeRepository.count();
            return HealthCheckResponse.named("tavern-readiness")
                    .status(recipes > 0)
                    .withData("recettes", recipes)
                    .build();
        } catch (RuntimeException e) {
            return HealthCheckResponse.named("tavern-readiness")
                    .down()
                    .withData("erreur", e.getClass().getSimpleName())
                    .build();
        }
    }
}
