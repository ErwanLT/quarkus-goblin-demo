package fr.eletutour.tavern.service;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import fr.eletutour.tavern.domain.Adventurer;
import fr.eletutour.tavern.dto.AdventurerDTO;
import fr.eletutour.tavern.dto.AdventurerRequest;
import fr.eletutour.tavern.dto.DtoMapper;
import fr.eletutour.tavern.exception.business.TavernError;
import fr.eletutour.tavern.exception.business.TavernException;
import fr.eletutour.tavern.repository.AdventurerRepository;
import io.quarkus.panache.common.Sort;

/**
 * Le registre des aventuriers.
 */
@ApplicationScoped
public class AdventurerService {

    private static final Logger LOG = Logger.getLogger(AdventurerService.class);

    @Inject
    AdventurerRepository adventurerRepository;

    public List<AdventurerDTO> consulterLeRegistre() {
        return adventurerRepository.listAll(Sort.by("name")).stream().map(DtoMapper::toDto).toList();
    }

    public AdventurerDTO trouverAventurier(Long id) {
        return DtoMapper.toDto(charger(id));
    }

    @Transactional
    public AdventurerDTO inscrire(AdventurerRequest request) {
        adventurerRepository.findByName(request.name()).ifPresent(existing -> {
            throw TavernException.of(TavernError.AVENTURIER_EXISTANT, existing.name);
        });
        Adventurer adventurer = new Adventurer(request.name().trim(), request.adventurerClass(), request.gold());
        adventurerRepository.persist(adventurer);
        LOG.infof("Nouvel aventurier au registre : %s (%s, %d pièces d'or)", adventurer.name, adventurer.adventurerClass,
                adventurer.gold);
        return DtoMapper.toDto(adventurer);
    }

    Adventurer charger(Long id) {
        return adventurerRepository.findByIdOptional(id)
                .orElseThrow(() -> TavernException.of(TavernError.AVENTURIER_INTROUVABLE, id));
    }
}
