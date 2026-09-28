package fr.eletutour.tavern.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import fr.eletutour.tavern.domain.AdventurerClass;

@Schema(description = "Nouvel aventurier à inscrire au registre")
public record AdventurerRequest(
        @Schema(description = "Nom", examples = "Lyra") @NotBlank @Size(max = 60) String name,
        @Schema(description = "Classe", examples = "BARDE") @NotNull AdventurerClass adventurerClass,
        @Schema(description = "Pièces d'or à l'inscription", examples = "80") @PositiveOrZero @Max(100_000) int gold) {
}
