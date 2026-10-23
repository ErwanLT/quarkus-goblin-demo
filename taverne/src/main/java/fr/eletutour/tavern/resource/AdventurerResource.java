package fr.eletutour.tavern.resource;

import java.net.URI;
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
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import fr.eletutour.tavern.dto.AdventurerDTO;
import fr.eletutour.tavern.dto.AdventurerRequest;
import fr.eletutour.tavern.dto.OrderDTO;
import fr.eletutour.tavern.exception.model.Problem;
import fr.eletutour.tavern.service.AdventurerService;
import fr.eletutour.tavern.service.OrderService;

@Path("/aventuriers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = TavernApi.AVENTURIERS)
public class AdventurerResource {

    @Inject
    AdventurerService adventurerService;

    @Inject
    OrderService orderService;

    @GET
    @Operation(summary = "Consulter le registre", description = "Retourne tous les aventuriers inscrits, avec leur bourse.")
    public List<AdventurerDTO> registre() {
        return adventurerService.consulterLeRegistre();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consulter un aventurier")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "L'aventurier", content = @Content(schema = @Schema(implementation = AdventurerDTO.class))),
            @APIResponse(responseCode = "404", description = "Aventurier inconnu", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public AdventurerDTO aventurier(@Parameter(description = "Identifiant de l'aventurier", example = "1") @PathParam("id") Long id) {
        return adventurerService.trouverAventurier(id);
    }

    @GET
    @Path("/{id}/commandes")
    @Operation(summary = "Commandes d'un aventurier", description = "Retourne les commandes de l'aventurier, de la plus récente à la plus ancienne.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Les commandes"),
            @APIResponse(responseCode = "404", description = "Aventurier inconnu", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public List<OrderDTO> commandes(@Parameter(description = "Identifiant de l'aventurier", example = "1") @PathParam("id") Long id) {
        return orderService.commandesDe(id);
    }

    @POST
    @Operation(summary = "Inscrire un aventurier")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Aventurier inscrit", content = @Content(schema = @Schema(implementation = AdventurerDTO.class))),
            @APIResponse(responseCode = "400", description = "Données invalides", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "409", description = "Aventurier déjà inscrit", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public Response inscrire(@NotNull @Valid AdventurerRequest request) {
        AdventurerDTO created = adventurerService.inscrire(request);
        return Response.created(URI.create("/aventuriers/" + created.id())).entity(created).build();
    }
}
