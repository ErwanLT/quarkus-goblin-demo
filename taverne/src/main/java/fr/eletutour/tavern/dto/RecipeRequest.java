package fr.eletutour.tavern.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Nouvelle recette à inscrire dans le grimoire")
public record RecipeRequest(
        @Schema(description = "Titre de la recette", examples = "Soupe du Kraken") @NotBlank @Size(max = 100) String title,
        @Schema(description = "Description", examples = "Un bouillon iodé qui réveille les marins.") @Size(max = 1000) String description,
        @Schema(description = "Prix d'une portion en pièces d'or", examples = "12") @Positive @Max(10_000) int price,
        @Schema(description = "Identifiants des ingrédients utilisés", examples = "[2, 3]") @NotEmpty List<Long> ingredientIds) {
}
