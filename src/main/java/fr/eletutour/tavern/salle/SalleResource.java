package fr.eletutour.tavern.salle;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestStreamElementType;

import fr.eletutour.tavern.dto.AdventurerDTO;
import fr.eletutour.tavern.dto.RecipeDTO;
import fr.eletutour.tavern.service.AdventurerService;
import fr.eletutour.tavern.service.GrimoireService;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Multi;

/**
 * La salle de la taverne : l'écran de démo, à projeter à côté de la Dev UI Goblin. La page est rendue par Qute, puis
 * animée par un flux SSE.
 */
@Path("/salle")
@Tag(name = "Salle", description = "L'écran de démo : ce que vivent les clients de la taverne, en direct")
public class SalleResource {

    @Inject
    GrimoireService grimoireService;

    @Inject
    AdventurerService adventurerService;

    @Inject
    SalleService salle;

    @CheckedTemplate
    static class Templates {

        static native TemplateInstance salle(List<RecipeDTO> recettes, List<AdventurerDTO> aventuriers);

        private Templates() {
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Operation(summary = "La salle", description = "Page de démo : comptoir, carte du jour, porte de derrière et bourses.")
    public TemplateInstance page() {
        return Templates.salle(grimoireService.consulterLeGrimoire(), adventurerService.consulterLeRegistre());
    }

    @GET
    @Path("/evenements")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Operation(summary = "Événements de la salle", description = "Flux SSE : commandes, carte, livraisons, disjoncteur et état du gobelin.")
    public Multi<SalleEvent> evenements() {
        return salle.flux();
    }
}
