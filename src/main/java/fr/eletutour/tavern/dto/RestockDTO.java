package fr.eletutour.tavern.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Résultat d'un réapprovisionnement")
public record RestockDTO(
        @Schema(description = "Étagère après livraison") StockDTO stock,
        @Schema(description = "Quantité effectivement livrée", examples = "20") int delivered,
        @Schema(description = "LIVRE, ou MARCHAND_ABSENT quand la guilde n'a pas répondu", examples = "LIVRE") String status,
        @Schema(description = "Message du marchand", examples = "Livré par le chariot de Borin") String message) {
}
