package fr.eletutour.tavern.repository;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import fr.eletutour.tavern.domain.Stock;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

/**
 * Le gardien de la cave.
 */
@ApplicationScoped
public class StockRepository implements PanacheRepository<Stock> {

    public List<Stock> listAllWithIngredient() {
        return find("from Stock s join fetch s.ingredient order by s.ingredient.name").list();
    }

    public Optional<Stock> findByIngredientId(Long ingredientId) {
        return find("from Stock s join fetch s.ingredient where s.ingredient.id = ?1", ingredientId).firstResultOptional();
    }
}
