package fr.eletutour.tavern.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import fr.eletutour.tavern.domain.Adventurer;
import fr.eletutour.tavern.domain.AdventurerClass;
import fr.eletutour.tavern.domain.Ingredient;
import fr.eletutour.tavern.domain.Recipe;
import fr.eletutour.tavern.domain.Stock;
import fr.eletutour.tavern.repository.AdventurerRepository;
import fr.eletutour.tavern.repository.IngredientRepository;
import fr.eletutour.tavern.repository.RecipeRepository;
import fr.eletutour.tavern.repository.StockRepository;
import fr.eletutour.tavern.service.GrimoireService;
import io.quarkus.runtime.StartupEvent;

/**
 * Remplit les étagères avant l'arrivée du premier client, quelle que soit la base de données. Ne fait rien si la
 * réserve contient déjà des ingrédients.
 */
@ApplicationScoped
public class InitialData {

    private static final Logger LOG = Logger.getLogger(InitialData.class);

    @Inject
    IngredientRepository ingredientRepository;

    @Inject
    StockRepository stockRepository;

    @Inject
    RecipeRepository recipeRepository;

    @Inject
    AdventurerRepository adventurerRepository;

    @Inject
    GrimoireService grimoireService;

    void onStart(@Observes StartupEvent event) {
        remplirLesEtageres();
        // la première lecture du grimoire écrit l'ardoise, servie si la cave tombe en panne
        grimoireService.consulterLeGrimoire();
    }

    @Transactional
    void remplirLesEtageres() {
        if (ingredientRepository.count() > 0) {
            return;
        }
        Map<String, Ingredient> reserve = new LinkedHashMap<>();
        ingredient(reserve, "Queue de Phénix", "unité", 150.0, 5);
        ingredient(reserve, "Malt de Nain", "kg", 5.0, 60);
        ingredient(reserve, "Eau de source elfique", "litre", 2.5, 80);
        ingredient(reserve, "Basilic séché", "gramme", 0.5, 50);
        ingredient(reserve, "Miel sauvage", "pot", 4.0, 40);
        ingredient(reserve, "Sanglier fumé", "kg", 12.0, 30);
        ingredient(reserve, "Champignon des cavernes", "unité", 3.0, 45);
        ingredient(reserve, "Pain de seigle", "miche", 1.0, 70);

        recette(reserve, "Hydromel de l'Elfe", "Une boisson rafraîchissante et légèrement magique.", 8,
                "Malt de Nain", "Eau de source elfique", "Miel sauvage");
        recette(reserve, "Ragoût de Basilic", "Un plat consistant pour les guerriers fatigués.", 14,
                "Basilic séché", "Sanglier fumé");
        recette(reserve, "Tourte aux champignons", "Croustillante, avec des champignons ramassés au fond de la mine.", 9,
                "Champignon des cavernes", "Pain de seigle", "Basilic séché");
        recette(reserve, "Tartine du voyageur", "Du pain, du miel, et la route reprend.", 3,
                "Pain de seigle", "Miel sauvage");
        recette(reserve, "Élixir du Phénix", "Ramène un aventurier d'entre les presque-morts. Stock très limité.", 320,
                "Queue de Phénix", "Eau de source elfique");

        List.of(new Adventurer("Arthas", AdventurerClass.GUERRIER, 500),
                new Adventurer("Lyra", AdventurerClass.BARDE, 120),
                new Adventurer("Morgana", AdventurerClass.MAGE, 1500),
                new Adventurer("Bilbon", AdventurerClass.VOLEUR, 25),
                new Adventurer("Frère Tuck", AdventurerClass.CLERC, 200),
                new Adventurer("Aragorn", AdventurerClass.RODEUR, 350))
                .forEach(adventurerRepository::persist);

        LOG.infof("La taverne ouvre : %d ingrédients, %d recettes, %d aventuriers", ingredientRepository.count(),
                recipeRepository.count(), adventurerRepository.count());
    }

    private void ingredient(Map<String, Ingredient> reserve, String name, String unit, double cost, int quantity) {
        Ingredient ingredient = new Ingredient(name, unit, cost);
        ingredientRepository.persist(ingredient);
        stockRepository.persist(new Stock(ingredient, quantity));
        reserve.put(name, ingredient);
    }

    private void recette(Map<String, Ingredient> reserve, String title, String description, int price,
            String... ingredients) {
        Recipe recipe = new Recipe(title, description, price);
        for (String ingredient : ingredients) {
            recipe.ingredients.add(reserve.get(ingredient));
        }
        recipeRepository.persist(recipe);
    }
}
