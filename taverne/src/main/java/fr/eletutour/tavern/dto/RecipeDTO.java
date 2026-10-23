package fr.eletutour.tavern.dto;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Une recette du grimoire")
public record RecipeDTO(
        @Schema(description = "Identifiant de la recette", examples = "1") Long id,
        @Schema(description = "Titre de la recette", examples = "Hydromel de l'Elfe") String title,
        @Schema(description = "Description de la recette", examples = "Une boisson rafraîchissante et légèrement magique.") String description,
        @Schema(description = "Prix d'une portion en pièces d'or", examples = "8") int price,
        @Schema(description = "Ingrédients nécessaires, une unité de chacun par portion") List<IngredientDTO> ingredients) {
}
