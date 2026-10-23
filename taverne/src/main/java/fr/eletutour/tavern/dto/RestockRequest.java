package fr.eletutour.tavern.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Demande de réapprovisionnement auprès de la guilde des marchands")
public record RestockRequest(
        @Schema(description = "Quantité à commander", examples = "20") @Positive @Max(500) int quantity) {
}
