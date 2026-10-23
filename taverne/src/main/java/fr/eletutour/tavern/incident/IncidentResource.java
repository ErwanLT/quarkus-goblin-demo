package fr.eletutour.tavern.incident;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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

import fr.eletutour.tavern.exception.model.Problem;
import fr.eletutour.tavern.resource.TavernApi;

/**
 * Le bureau du tavernier d'astreinte : la main courante des incidents, et le post-mortem de chacun.
 */
@Path("/exploitation/incidents")
@Tag(name = TavernApi.EXPLOITATION)
public class IncidentResource {

    static final String MARKDOWN = "text/markdown";

    @Inject
    PostMortemService postMortems;

    @Inject
    MainCourante mainCourante;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Les derniers incidents", description = "Du plus récent au plus ancien. La main courante survit "
            + "aux redémarrages de la taverne.")
    @APIResponse(responseCode = "200", description = "Les incidents")
    public List<Incident> incidents(
            @Parameter(description = "Nombre maximal d'incidents", example = "10") @QueryParam("limite") @DefaultValue("20") @Min(1) @Max(100) int limite) {
        return postMortems.derniers(limite);
    }

    @GET
    @Path("/en-cours")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "L'incident en cours", description = "Tel que la salle l'affiche. Un incident s'ouvre au premier "
            + "signe de dégradation et se clôt après une période de calme, disjoncteur refermé.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Un incident est en cours", content = @Content(schema = @Schema(implementation = MainCourante.IncidentEnCours.class))),
            @APIResponse(responseCode = "204", description = "Aucun incident en cours")
    })
    public Response enCours() {
        return mainCourante.incidentEnCours().map(Response::ok).orElseGet(Response::noContent).build();
    }

    @GET
    @Path("/{id}/post-mortem")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Le post-mortem d'un incident", description = "Sans blâme : chronologie, impact, cause probable, "
            + "ce qui a tenu, ce qui a cédé, actions correctives, et la commande qui rallume le même feu. Chaque phrase "
            + "découle d'un fait consigné. Aussi disponible en Markdown (Accept: text/markdown).")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Le post-mortem", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PostMortem.class)),
                    @Content(mediaType = MARKDOWN) }),
            @APIResponse(responseCode = "404", description = "Incident inconnu", content = @Content(mediaType = TavernApi.PROBLEM_JSON, schema = @Schema(implementation = Problem.class)))
    })
    public PostMortem postMortem(@Parameter(description = "Identifiant de l'incident", example = "1") @PathParam("id") long id) {
        return postMortems.rediger(id);
    }

    @GET
    @Path("/{id}/post-mortem")
    @Produces(MARKDOWN + ";charset=UTF-8")
    @Operation(hidden = true)
    public String postMortemEnMarkdown(@PathParam("id") long id) {
        return postMortems.enMarkdown(postMortems.rediger(id));
    }
}
