package fr.eletutour.tavern.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Une étagère de la cave")
public record StockDTO(
        @Schema(description = "Identifiant de l'ingrédient", examples = "2") Long ingredientId,
        @Schema(description = "Nom de l'ingrédient", examples = "Malt de Nain") String ingredientName,
        @Schema(description = "Unité de mesure", examples = "kg") String unit,
        @Schema(description = "Quantité disponible", examples = "40") int quantity) {
}
