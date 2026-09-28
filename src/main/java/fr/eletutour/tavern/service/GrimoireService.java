package fr.eletutour.tavern.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.logging.Logger;

import fr.eletutour.tavern.domain.Ingredient;
import fr.eletutour.tavern.domain.Recipe;
import fr.eletutour.tavern.domain.Stock;
import fr.eletutour.tavern.dto.DtoMapper;
import fr.eletutour.tavern.dto.IngredientDTO;
import fr.eletutour.tavern.dto.IngredientRequest;
import fr.eletutour.tavern.dto.RecipeDTO;
import fr.eletutour.tavern.dto.RecipeRequest;
import fr.eletutour.tavern.exception.business.TavernError;
import fr.eletutour.tavern.exception.business.TavernException;
import fr.eletutour.tavern.repository.IngredientRepository;
import fr.eletutour.tavern.repository.RecipeRepository;
import fr.eletutour.tavern.repository.StockRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.panache.common.Sort;

/**
 * Les cuisines du grimoire : ingrédients et recettes.
 * <p>
 * La lecture du grimoire tient une <em>ardoise</em> : la dernière carte servie avec succès. Si la cave ne répond plus
 * (base de données en panne, couche {@code DATABASE} ou {@code SERVICE} de Quarkus Goblin), le tavernier réessaie
 * deux fois, puis lit l'ardoise au lieu de renvoyer une erreur.
 */
@ApplicationScoped
public class GrimoireService {

    private static final Logger LOG = Logger.getLogger(GrimoireService.class);

    @Inject
    IngredientRepository ingredientRepository;

    @Inject
    RecipeRepository recipeRepository;

    @Inject
    StockRepository stockRepository;

    private final AtomicReference<List<RecipeDTO>> ardoise = new AtomicReference<>(List.of());

    public List<IngredientDTO> consulterLaReserve() {
        return ingredientRepository.listAll(Sort.by("name")).stream().map(DtoMapper::toDto).toList();
    }

    public IngredientDTO trouverIngredient(Long id) {
        return ingredientRepository.findByIdOptional(id)
                .map(DtoMapper::toDto)
                .orElseThrow(() -> TavernException.of(TavernError.INGREDIENT_INTROUVABLE, id));
    }

    /**
     * Inscrit un nouvel ingrédient et lui réserve une étagère vide dans la cave.
     */
    @Transactional
    public IngredientDTO acquerirIngredient(IngredientRequest request) {
        ingredientRepository.findByName(request.name()).ifPresent(existing -> {
            throw TavernException.of(TavernError.INGREDIENT_EXISTANT, existing.name);
        });
        Ingredient ingredient = new Ingredient(request.name().trim(), request.unit().trim(), request.cost());
        ingredientRepository.persist(ingredient);
        stockRepository.persist(new Stock(ingredient, 0));
        LOG.infof("Nouvel ingrédient dans la réserve : %s (%s, %.2f pièces d'or), étagère vide créée", ingredient.name,
                ingredient.unit, ingredient.cost);
        return DtoMapper.toDto(ingredient);
    }

    @Retry(maxRetries = 2, delay = 100)
    @Fallback(fallbackMethod = "lireLArdoise")
    @WithSpan("consulter-le-grimoire")
    public List<RecipeDTO> consulterLeGrimoire() {
        List<RecipeDTO> carte = recipeRepository.listAllWithIngredients().stream().map(DtoMapper::toDto).toList();
        ardoise.set(carte);
        LOG.debugf("Grimoire consulté : %d recette(s), ardoise mise à jour", carte.size());
        return carte;
    }

    List<RecipeDTO> lireLArdoise() {
        List<RecipeDTO> carte = ardoise.get();
        LOG.warnf("La cave ne répond plus : le tavernier lit l'ardoise (%d recette(s))", carte.size());
        return carte;
    }

    public RecipeDTO trouverRecette(Long id) {
        return recipeRepository.findByIdWithIngredients(id)
                .map(DtoMapper::toDto)
                .orElseThrow(() -> TavernException.of(TavernError.RECETTE_INTROUVABLE, id));
    }

    public List<RecipeDTO> chercherDansLeGrimoire(String title) {
        if (title == null || title.isBlank()) {
            return consulterLeGrimoire();
        }
        return recipeRepository.searchByTitle(title.trim()).stream().map(DtoMapper::toDto).toList();
    }

    @Transactional
    public RecipeDTO inscrireRecette(RecipeRequest request) {
        Set<Long> ids = new LinkedHashSet<>(request.ingredientIds());
        if (ids.contains(null)) {
            throw TavernException.of(TavernError.RECETTE_SANS_INGREDIENT);
        }
        Recipe recipe = new Recipe(request.title().trim(), request.description(), request.price());
        for (Long id : ids) {
            recipe.ingredients.add(ingredientRepository.findByIdOptional(id)
                    .orElseThrow(() -> TavernException.of(TavernError.INGREDIENT_INTROUVABLE, id)));
        }
        recipeRepository.persist(recipe);
        LOG.infof("Nouvelle recette dans le grimoire : %s (%d pièces d'or, %d ingrédient(s))", recipe.title, recipe.price,
                recipe.ingredients.size());
        return DtoMapper.toDto(recipe);
    }
}
