package fr.eletutour.tavern.resource;

import jakarta.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * La carte du menu : déclaration des sections de l'API pour la documentation OpenAPI.
 */
@OpenAPIDefinition(
        info = @Info(title = "The Falling Whale API", version = "1.0.0"),
        tags = {
                @Tag(name = TavernApi.GRIMOIRE, description = "Ingrédients et recettes du grimoire"),
                @Tag(name = TavernApi.CAVE, description = "Les étagères de la cave et leur réapprovisionnement"),
                @Tag(name = TavernApi.AVENTURIERS, description = "Le registre des aventuriers"),
                @Tag(name = TavernApi.COMMANDES, description = "Le comptoir des commandes"),
                @Tag(name = TavernApi.GUILDE, description = "Fournisseur simulé, appelé par le REST Client de la cave")
        })
public class TavernApi extends Application {

    public static final String GRIMOIRE = "Grimoire";
    public static final String CAVE = "Cave";
    public static final String AVENTURIERS = "Aventuriers";
    public static final String COMMANDES = "Commandes";
    public static final String GUILDE = "Guilde des marchands";

    public static final String PROBLEM_JSON = "application/problem+json";
}
