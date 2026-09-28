package fr.eletutour.tavern.client;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Commande passée à la guilde des marchands")
public record DeliveryRequest(
        @Schema(description = "Ingrédient demandé", examples = "Malt de Nain") String ingredient,
        @Schema(description = "Quantité demandée", examples = "20") int quantity) {
}
