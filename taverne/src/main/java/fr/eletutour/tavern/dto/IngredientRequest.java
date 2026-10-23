package fr.eletutour.tavern.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Nouvel ingrédient à inscrire dans la réserve")
public record IngredientRequest(
        @Schema(description = "Nom de l'ingrédient", examples = "Poudre de licorne") @NotBlank @Size(max = 100) String name,
        @Schema(description = "Unité de mesure", examples = "gramme") @NotBlank @Size(max = 30) String unit,
        @Schema(description = "Coût en pièces d'or", examples = "12.5") @PositiveOrZero double cost) {
}
