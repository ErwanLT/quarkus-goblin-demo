package fr.eletutour.tavern.repository;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import fr.eletutour.tavern.domain.Recipe;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

/**
 * L'archiviste du grimoire : les recettes sont toujours chargées avec leurs ingrédients, en une seule requête.
 */
@ApplicationScoped
public class RecipeRepository implements PanacheRepository<Recipe> {

    public List<Recipe> listAllWithIngredients() {
        return find("select distinct r from Recipe r left join fetch r.ingredients order by r.id").list();
    }

    public Optional<Recipe> findByIdWithIngredients(Long id) {
        return find("from Recipe r left join fetch r.ingredients where r.id = ?1", id).firstResultOptional();
    }

    public List<Recipe> searchByTitle(String title) {
        return find("select distinct r from Recipe r left join fetch r.ingredients where lower(r.title) like ?1 order by r.id",
                "%" + title.toLowerCase() + "%").list();
    }
}
