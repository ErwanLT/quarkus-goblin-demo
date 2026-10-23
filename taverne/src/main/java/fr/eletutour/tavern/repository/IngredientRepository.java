package fr.eletutour.tavern.repository;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import fr.eletutour.tavern.domain.Ingredient;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

/**
 * L'intendant de la réserve.
 */
@ApplicationScoped
public class IngredientRepository implements PanacheRepository<Ingredient> {

    public Optional<Ingredient> findByName(String name) {
        return find("lower(name) = lower(?1)", name).firstResultOptional();
    }
}
