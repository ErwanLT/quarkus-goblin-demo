package fr.eletutour.tavern.repository;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import fr.eletutour.tavern.domain.Adventurer;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

/**
 * Le registre des aventuriers.
 */
@ApplicationScoped
public class AdventurerRepository implements PanacheRepository<Adventurer> {

    public Optional<Adventurer> findByName(String name) {
        return find("lower(name) = lower(?1)", name).firstResultOptional();
    }
}
