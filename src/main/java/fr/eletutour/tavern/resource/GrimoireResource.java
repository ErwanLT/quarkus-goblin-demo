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

import fr.eletutour.tavern.dto.IngredientDTO;
import fr.eletutour.tavern.dto.IngredientRequest;
import fr.eletutour.tavern.dto.RecipeDTO;
import fr.eletutour.tavern.dto.RecipeRequest;
import fr.eletutour.tavern.exception.model.Problem;
import fr.eletutour.tavern.service.GrimoireService;

@Path("/grimoire")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = TavernApi.GRIMOIRE)
public class GrimoireResource {

    @Inject
    GrimoireService grimoireService;

    @GET
    @Path("/ingredients")
    @Operation(summary = "Lister les ingrédients", description = "Retourne tous les ingrédients de la réserve, par ordre alphabétique.")
    public List<IngredientDTO> ingredients() {
        return grimoireService.consulterLaReserve();
    }

    @GET
    @Path("/ingredients/{id}")
    @Operation(summary = "Consulter un ingrédient")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "L'ingrédient", content = @Content(schema = @Schema(implementation = IngredientDTO.class))),
            @APIResponse(responseCode = "404", description = "Ingrédient inconnu", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public IngredientDTO ingredient(@Parameter(description = "Identifiant de l'ingrédient", example = "1") @PathParam("id") Long id) {
        return grimoireService.trouverIngredient(id);
    }

    @POST
    @Path("/ingredients")
    @Operation(summary = "Ajouter un ingrédient", description = "Inscrit un ingrédient dans la réserve et lui réserve une étagère vide dans la cave.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Ingrédient créé", content = @Content(schema = @Schema(implementation = IngredientDTO.class))),
            @APIResponse(responseCode = "400", description = "Données invalides", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "409", description = "Ingrédient déjà connu", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public Response ajouterIngredient(@NotNull @Valid IngredientRequest request) {
        IngredientDTO created = grimoireService.acquerirIngredient(request);
        return Response.created(URI.create("/grimoire/ingredients/" + created.id())).entity(created).build();
    }

    @GET
    @Path("/recettes")
    @Operation(summary = "Lister les recettes", description = "Retourne le grimoire complet. Si la cave ne répond plus, "
            + "le tavernier réessaie puis sert la dernière carte connue (l'ardoise).")
    public List<RecipeDTO> recettes() {
        return grimoireService.consulterLeGrimoire();
    }

    @GET
    @Path("/recettes/recherche")
    @Operation(summary = "Rechercher des recettes", description = "Recherche des recettes par morceau de titre.")
    public List<RecipeDTO> rechercher(@Parameter(description = "Morceau de titre", example = "Hydromel") @QueryParam("titre") String titre) {
        return grimoireService.chercherDansLeGrimoire(titre);
    }

    @GET
    @Path("/recettes/{id}")
    @Operation(summary = "Consulter une recette")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "La recette", content = @Content(schema = @Schema(implementation = RecipeDTO.class))),
            @APIResponse(responseCode = "404", description = "Recette inconnue", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public RecipeDTO recette(@Parameter(description = "Identifiant de la recette", example = "1") @PathParam("id") Long id) {
        return grimoireService.trouverRecette(id);
    }

    @POST
    @Path("/recettes")
    @Operation(summary = "Ajouter une recette", description = "Inscrit une recette dans le grimoire à partir d'ingrédients connus.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Recette créée", content = @Content(schema = @Schema(implementation = RecipeDTO.class))),
            @APIResponse(responseCode = "400", description = "Données invalides", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class))),
            @APIResponse(responseCode = "404", description = "Ingrédient inconnu", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public Response ajouterRecette(@NotNull @Valid RecipeRequest request) {
        RecipeDTO created = grimoireService.inscrireRecette(request);
        return Response.created(URI.create("/grimoire/recettes/" + created.id())).entity(created).build();
    }
}
