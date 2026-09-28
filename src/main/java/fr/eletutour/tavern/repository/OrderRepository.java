package fr.eletutour.tavern.repository;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import fr.eletutour.tavern.domain.TavernOrder;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

/**
 * Le livre des commandes.
 */
@ApplicationScoped
public class OrderRepository implements PanacheRepository<TavernOrder> {

    private static final String WITH_DETAILS = "from TavernOrder o join fetch o.adventurer join fetch o.recipe ";

    public Optional<TavernOrder> findByIdWithDetails(Long id) {
        return find(WITH_DETAILS + "where o.id = ?1", id).firstResultOptional();
    }

    public List<TavernOrder> latest(int limit) {
        return find(WITH_DETAILS + "order by o.id desc").page(0, limit).list();
    }

    public List<TavernOrder> byAdventurer(Long adventurerId) {
        return find(WITH_DETAILS + "where o.adventurer.id = ?1 order by o.id desc", adventurerId).list();
    }
}
