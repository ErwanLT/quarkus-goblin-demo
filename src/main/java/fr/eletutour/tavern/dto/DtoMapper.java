package fr.eletutour.tavern.dto;

import fr.eletutour.tavern.domain.Adventurer;
import fr.eletutour.tavern.domain.Ingredient;
import fr.eletutour.tavern.domain.Recipe;
import fr.eletutour.tavern.domain.Stock;
import fr.eletutour.tavern.domain.TavernOrder;

/**
 * Ce que Panache sort de la cave, présenté proprement avant d'être servi en salle.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static IngredientDTO toDto(Ingredient ingredient) {
        return new IngredientDTO(ingredient.id, ingredient.name, ingredient.unit, ingredient.cost);
    }

    public static RecipeDTO toDto(Recipe recipe) {
        return new RecipeDTO(recipe.id, recipe.title, recipe.description, recipe.price,
                recipe.ingredients.stream().map(DtoMapper::toDto).toList());
    }

    public static StockDTO toDto(Stock stock) {
        return new StockDTO(stock.ingredient.id, stock.ingredient.name, stock.ingredient.unit, stock.quantity);
    }

    public static AdventurerDTO toDto(Adventurer adventurer) {
        return new AdventurerDTO(adventurer.id, adventurer.name, adventurer.adventurerClass, adventurer.gold);
    }

    public static OrderDTO toDto(TavernOrder order) {
        return new OrderDTO(order.id, order.adventurer.id, order.adventurer.name, order.recipe.id, order.recipe.title,
                order.quantity, order.total, order.status, order.createdAt);
    }
}
