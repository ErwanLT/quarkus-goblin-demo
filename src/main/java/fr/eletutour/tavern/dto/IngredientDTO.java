package fr.eletutour.tavern.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Un ingrédient de la réserve")
public record IngredientDTO(
        @Schema(description = "Identifiant de l'ingrédient", examples = "1") Long id,
        @Schema(description = "Nom de l'ingrédient", examples = "Queue de Phénix") String name,
        @Schema(description = "Unité de mesure", examples = "unité") String unit,
        @Schema(description = "Coût en pièces d'or", examples = "150.0") double cost) {
}
