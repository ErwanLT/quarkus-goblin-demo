package fr.eletutour.tavern.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Une commande passée au comptoir")
public record OrderRequest(
        @Schema(description = "Aventurier qui commande", examples = "1") @NotNull Long adventurerId,
        @Schema(description = "Recette commandée", examples = "1") @NotNull Long recipeId,
        @Schema(description = "Nombre de portions", examples = "2") @Min(1) @Max(10) int quantity) {
}
