package fr.eletutour.tavern.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import fr.eletutour.tavern.domain.AdventurerClass;

@Schema(description = "Un aventurier inscrit au registre")
public record AdventurerDTO(
        @Schema(description = "Identifiant de l'aventurier", examples = "1") Long id,
        @Schema(description = "Nom", examples = "Arthas") String name,
        @Schema(description = "Classe", examples = "GUERRIER") AdventurerClass adventurerClass,
        @Schema(description = "Pièces d'or dans la bourse", examples = "120") int gold) {
}
