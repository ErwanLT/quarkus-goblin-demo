package fr.eletutour.tavern.resource;

import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import fr.eletutour.tavern.dto.OrderDTO;
import fr.eletutour.tavern.dto.OrderRequest;
import fr.eletutour.tavern.exception.model.Problem;
import fr.eletutour.tavern.service.OrderService;

@Path("/commandes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = TavernApi.COMMANDES)
public class OrderResource {

    @Inject
    OrderService orderService;

    @POST
    @Operation(summary = "Passer une commande", description = "Vérifie la bourse de l'aventurier, fait préparer la recette, "
            + "puise les ingrédients dans la cave et encaisse. Une commande doit être servie en moins de 1,5 s, et le comptoir "
            + "accepte au plus 60 commandes toutes les 10 secondes.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Commande servie", content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @APIResponse(responseCode = "400", description = "Commande invalide", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "404", description = "Aventurier ou recette inconnu", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "409", description = "Rupture de stock (liste des ingrédients manquants)", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "422", description = "Bourse insuffisante (prix et bourse)", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "429", description = "Trop de commandes", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "504", description = "Commande servie trop lentement", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public Response commander(@NotNull @Valid OrderRequest request) {
        OrderDTO order = orderService.passerCommande(request);
        return Response.created(URI.create("/commandes/" + order.id())).entity(order).build();
    }

    @GET
    @Operation(summary = "Dernières commandes", description = "Retourne les dernières commandes servies, de la plus récente à la plus ancienne.")
    public List<OrderDTO> dernieres(
            @Parameter(description = "Nombre maximal de commandes", example = "20") @QueryParam("limite") @DefaultValue("20") @Min(1) @Max(100) int limite) {
        return orderService.dernieresCommandes(limite);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consulter une commande")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "La commande", content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @APIResponse(responseCode = "404", description = "Commande inconnue", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public OrderDTO commande(@Parameter(description = "Identifiant de la commande", example = "1") @PathParam("id") Long id) {
        return orderService.trouverCommande(id);
    }
}
