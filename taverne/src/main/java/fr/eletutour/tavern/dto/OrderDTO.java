package fr.eletutour.tavern.dto;

import java.time.Instant;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import fr.eletutour.tavern.domain.OrderStatus;

@Schema(description = "Une commande servie")
public record OrderDTO(
        @Schema(description = "Identifiant de la commande", examples = "12") Long id,
        @Schema(description = "Identifiant de l'aventurier", examples = "1") Long adventurerId,
        @Schema(description = "Nom de l'aventurier", examples = "Arthas") String adventurerName,
        @Schema(description = "Identifiant de la recette", examples = "1") Long recipeId,
        @Schema(description = "Titre de la recette", examples = "Hydromel de l'Elfe") String recipeTitle,
        @Schema(description = "Nombre de portions", examples = "2") int quantity,
        @Schema(description = "Total réglé en pièces d'or", examples = "16") int total,
        @Schema(description = "Pièces d'or restant dans la bourse de l'aventurier", examples = "84") int adventurerGold,
        @Schema(description = "État", examples = "SERVIE") OrderStatus status,
        @Schema(description = "Date de la commande") Instant createdAt) {
}
