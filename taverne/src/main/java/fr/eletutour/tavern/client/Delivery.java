package fr.eletutour.tavern.client;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Livraison de la guilde des marchands")
public record Delivery(
        @Schema(description = "Ingrédient livré", examples = "Malt de Nain") String ingredient,
        @Schema(description = "Quantité livrée", examples = "20") int quantity,
        @Schema(description = "Nom du convoyeur", examples = "Borin") String carrier) {
}
