package fr.eletutour.tavern.resource;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import fr.eletutour.tavern.dto.RestockDTO;
import fr.eletutour.tavern.dto.RestockRequest;
import fr.eletutour.tavern.dto.StockDTO;
import fr.eletutour.tavern.exception.model.Problem;
import fr.eletutour.tavern.service.CellarService;

@Path("/cave")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = TavernApi.CAVE)
public class CellarResource {

    @Inject
    CellarService cellarService;

    @GET
    @Path("/stocks")
    @Operation(summary = "Inventaire de la cave", description = "Retourne la quantité disponible de chaque ingrédient.")
    public List<StockDTO> inventaire() {
        return cellarService.inventaire();
    }

    @GET
    @Path("/stocks/{ingredientId}")
    @Operation(summary = "Consulter une étagère")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "L'étagère", content = @Content(schema = @Schema(implementation = StockDTO.class))),
            @APIResponse(responseCode = "404", description = "Ingrédient absent de la cave", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public StockDTO etagere(@Parameter(description = "Identifiant de l'ingrédient", example = "2") @PathParam("ingredientId") Long ingredientId) {
        return cellarService.trouverEtagere(ingredientId);
    }

    @POST
    @Path("/stocks/{ingredientId}/reapprovisionnement")
    @Operation(summary = "Réapprovisionner une étagère", description = "Commande l'ingrédient à la guilde des marchands "
            + "(appel REST sortant). Si la guilde ne répond pas après deux nouvelles tentatives, ou si le disjoncteur est "
            + "ouvert, la réponse l'indique avec le statut MARCHAND_ABSENT.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Résultat du réapprovisionnement", content = @Content(schema = @Schema(implementation = RestockDTO.class))),
            @APIResponse(responseCode = "400", description = "Quantité invalide", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "404", description = "Ingrédient absent de la cave", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public RestockDTO reapprovisionner(
            @Parameter(description = "Identifiant de l'ingrédient", example = "2") @PathParam("ingredientId") Long ingredientId,
            @NotNull @Valid RestockRequest request) {
        return cellarService.reapprovisionner(ingredientId, request.quantity());
    }
}
