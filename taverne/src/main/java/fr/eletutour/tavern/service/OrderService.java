package fr.eletutour.tavern.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import fr.eletutour.tavern.domain.Adventurer;
import fr.eletutour.tavern.domain.OrderStatus;
import fr.eletutour.tavern.domain.Recipe;
import fr.eletutour.tavern.domain.TavernOrder;
import fr.eletutour.tavern.dto.DtoMapper;
import fr.eletutour.tavern.dto.OrderDTO;
import fr.eletutour.tavern.dto.OrderRequest;
import fr.eletutour.tavern.exception.business.TavernError;
import fr.eletutour.tavern.exception.business.TavernException;
import fr.eletutour.tavern.observability.BusinessTimed;
import fr.eletutour.tavern.repository.OrderRepository;
import fr.eletutour.tavern.repository.RecipeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.smallrye.faulttolerance.api.RateLimit;

/**
 * Le comptoir des commandes : vérifier la bourse, faire préparer, puiser dans la cave, encaisser, noter la commande.
 * Le tout dans une seule transaction.
 * <p>
 * Deux lois de la taverne s'y appliquent : une commande doit être servie en moins d'une seconde et demie
 * ({@code @Timeout}, 504 sinon), et le comptoir n'accepte pas plus de 60 commandes toutes les 10 secondes
 * ({@code @RateLimit}, 429 sinon).
 */
@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);

    @Inject
    AdventurerService adventurerService;

    @Inject
    RecipeRepository recipeRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    CellarService cellarService;

    @Inject
    Kitchen kitchen;

    @Inject
    MeterRegistry meterRegistry;

    @Transactional
    @Timeout(1500)
    @RateLimit(value = 60, window = 10, windowUnit = ChronoUnit.SECONDS)
    @BusinessTimed(value = "tavern.order", description = "Commandes passées au comptoir")
    public OrderDTO passerCommande(OrderRequest request) {
        Adventurer adventurer = adventurerService.charger(request.adventurerId());
        Recipe recipe = recipeRepository.findByIdWithIngredients(request.recipeId())
                .orElseThrow(() -> TavernException.of(TavernError.RECETTE_INTROUVABLE, request.recipeId()));
        int total = recipe.price * request.quantity();

        Span span = Span.current();
        span.setAttribute("tavern.adventurer", adventurer.name);
        span.setAttribute("tavern.recipe", recipe.title);
        span.setAttribute("tavern.total", total);

        if (adventurer.gold < total) {
            throw TavernException.notEnoughGold(adventurer.name, total, adventurer.gold);
        }
        kitchen.preparer(recipe.title, request.quantity());
        cellarService.puiser(recipe, request.quantity());
        adventurer.gold -= total;

        TavernOrder order = new TavernOrder();
        order.adventurer = adventurer;
        order.recipe = recipe;
        order.quantity = request.quantity();
        order.total = total;
        order.status = OrderStatus.SERVIE;
        order.createdAt = Instant.now();
        orderRepository.persist(order);
        LOG.infof("Commande %d servie : %s, %d x %s, %d pièces d'or (bourse restante : %d)", order.id, adventurer.name,
                order.quantity, recipe.title, total, adventurer.gold);

        Counter.builder("tavern.gold.earned")
                .description("Pièces d'or encaissées au comptoir")
                .tag("recipe", recipe.title)
                .register(meterRegistry)
                .increment(total);
        return DtoMapper.toDto(order);
    }

    public OrderDTO trouverCommande(Long id) {
        return orderRepository.findByIdWithDetails(id)
                .map(DtoMapper::toDto)
                .orElseThrow(() -> TavernException.of(TavernError.COMMANDE_INTROUVABLE, id));
    }

    public List<OrderDTO> dernieresCommandes(int limit) {
        return orderRepository.latest(limit).stream().map(DtoMapper::toDto).toList();
    }

    public List<OrderDTO> commandesDe(Long adventurerId) {
        adventurerService.charger(adventurerId);
        return orderRepository.byAdventurer(adventurerId).stream().map(DtoMapper::toDto).toList();
    }
}
