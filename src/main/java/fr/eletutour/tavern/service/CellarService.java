package fr.eletutour.tavern.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import fr.eletutour.tavern.client.Delivery;
import fr.eletutour.tavern.domain.Ingredient;
import fr.eletutour.tavern.domain.Recipe;
import fr.eletutour.tavern.domain.Stock;
import fr.eletutour.tavern.dto.DtoMapper;
import fr.eletutour.tavern.dto.RestockDTO;
import fr.eletutour.tavern.dto.StockDTO;
import fr.eletutour.tavern.exception.business.TavernError;
import fr.eletutour.tavern.exception.business.TavernException;
import fr.eletutour.tavern.repository.StockRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 * La cave : les étagères, ce qu'on y puise pour chaque commande, et les livraisons de la guilde des marchands.
 */
@ApplicationScoped
public class CellarService {

    private static final Logger LOG = Logger.getLogger(CellarService.class);

    @Inject
    StockRepository stockRepository;

    @Inject
    MerchantService merchantService;

    public List<StockDTO> inventaire() {
        return stockRepository.listAllWithIngredient().stream().map(DtoMapper::toDto).toList();
    }

    public StockDTO trouverEtagere(Long ingredientId) {
        return DtoMapper.toDto(charger(ingredientId));
    }

    /**
     * Puise dans la cave de quoi préparer les portions demandées : une unité de chaque ingrédient par portion. Appelé
     * dans la transaction de la commande.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    @WithSpan("puiser-dans-la-cave")
    public void puiser(Recipe recipe, int portions) {
        Map<Long, Stock> stocks = stockRepository
                .list("ingredient.id in ?1", recipe.ingredients.stream().map(i -> i.id).toList())
                .stream()
                .collect(Collectors.toMap(s -> s.ingredient.id, Function.identity()));
        List<String> missing = new ArrayList<>();
        for (Ingredient ingredient : recipe.ingredients) {
            Stock stock = stocks.get(ingredient.id);
            if (stock == null || stock.quantity < portions) {
                missing.add(ingredient.name);
            }
        }
        if (!missing.isEmpty()) {
            Span.current().setAttribute("tavern.out_of_stock", String.join(", ", missing));
            throw TavernException.outOfStock(recipe.title, portions, missing);
        }
        stocks.values().forEach(stock -> stock.quantity -= portions);
        LOG.debugf("Puisé dans la cave pour %d x %s", (Object) portions, recipe.title);
    }

    /**
     * Commande un ingrédient à la guilde des marchands et range la livraison sur son étagère. Si le marchand ne vient
     * pas, l'étagère reste en l'état et la réponse le dit.
     */
    @Transactional
    public RestockDTO reapprovisionner(Long ingredientId, int quantity) {
        Stock stock = charger(ingredientId);
        Delivery delivery = merchantService.commander(stock.ingredient.name, quantity);
        stock.quantity += delivery.quantity();
        boolean delivered = delivery.carrier() != null;
        if (delivered) {
            LOG.infof("Réapprovisionnement : %d %s livré(s) par %s, étagère à %d", (Object) delivery.quantity(),
                    stock.ingredient.name, delivery.carrier(), stock.quantity);
        } else {
            LOG.warnf("Réapprovisionnement manqué : la guilde n'a pas livré %s, étagère toujours à %d",
                    stock.ingredient.name, stock.quantity);
        }
        return new RestockDTO(DtoMapper.toDto(stock), delivery.quantity(), delivered ? "LIVRE" : "MARCHAND_ABSENT",
                delivered ? "Livré par le chariot de " + delivery.carrier()
                        : "La guilde des marchands n'a pas répondu, réessayez plus tard.");
    }

    private Stock charger(Long ingredientId) {
        return stockRepository.findByIngredientId(ingredientId)
                .orElseThrow(() -> TavernException.of(TavernError.STOCK_INTROUVABLE, ingredientId));
    }
}
