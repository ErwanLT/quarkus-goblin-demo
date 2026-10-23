package fr.eletutour.tavern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import fr.eletutour.tavern.incident.MainCourante;
import io.quarkiverse.goblin.AssaultEngine;
import io.quarkiverse.goblin.ChaosLayer;
import io.quarkiverse.goblin.MutableAssaultConfig;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

/**
 * La nuit où la cave a pris feu, racontée par la main courante : l'incident s'ouvre au premier signe de dégradation, se
 * clôt au retour du calme, et son post-mortem ne contient que des faits comptés.
 */
@QuarkusTest
@TestProfile(PostMortemTest.MainCouranteProfile.class)
class PostMortemTest {

    @Inject
    AssaultEngine engine;

    @Inject
    MainCourante mainCourante;

    @AfterEach
    void desarmerLeGobelin() {
        engine.setActive(false);
    }

    @Test
    void laCaveBruleEtLeLendemainOnSaitPourquoi() throws InterruptedException {
        int aventurier = OrderResourceTest.inscrire("Gimli", 500);
        int tartine = OrderResourceTest.recette("Tartine du voyageur");
        mainCourante.consigner();
        given().when().get("/exploitation/incidents/en-cours").then().statusCode(204);

        // le feu prend dans la cave
        MutableAssaultConfig config = engine.getMutableConfig();
        config.setLatencyEnabled(false);
        config.setClientLatencyEnabled(false);
        config.setClientExceptionEnabled(false);
        config.setExceptionEnabled(true);
        config.setLayers(Set.of(ChaosLayer.DATABASE));
        config.setTargetLevel(100);
        engine.setActive(true);

        given().when().get("/grimoire/recettes").then().statusCode(200);
        given().contentType(ContentType.JSON).body(OrderResourceTest.commande(aventurier, tartine, 1))
                .when().post("/commandes").then().statusCode(500);
        mainCourante.consigner();
        int incident = given().when().get("/exploitation/incidents/en-cours")
                .then().statusCode(200).extract().path("id");

        // le feu est maîtrisé, le calme revient
        engine.setActive(false);
        Thread.sleep(200);
        mainCourante.consigner();
        given().when().get("/exploitation/incidents/en-cours").then().statusCode(204);

        given().when().get("/exploitation/incidents/" + incident + "/post-mortem")
                .then().statusCode(200)
                .body("statut", equalTo("CLOS"))
                .body("impact.cartesServiesDepuisLArdoise", greaterThanOrEqualTo(1))
                .body("impact.commandesPerdues", equalTo(1))
                .body("causeProbable[0]", containsString("source database"))
                .body("ceQuiAFonctionne", hasItem(containsString("depuis l'ardoise")))
                .body("ceQuiNAPasFonctionne", hasItem(containsString("1 commande(s) perdue(s)")))
                .body("actionsCorrectives", hasItem(containsString("503")))
                .body("rallumerLeFeu", containsString("\"layers\":[\"DATABASE\"]"));

        given().accept("text/markdown").when().get("/exploitation/incidents/" + incident + "/post-mortem")
                .then().statusCode(200)
                .body(containsString("# Post-mortem de l'incident " + incident))
                .body(containsString("| Commandes perdues (5xx) | 1 |"));
    }

    @Test
    void unIncidentInconnuNestPasDansLaMainCourante() {
        given().when().get("/exploitation/incidents/999999/post-mortem")
                .then().statusCode(404)
                .contentType("application/problem+json");
    }

    /**
     * Une taverne à elle seule : aucun disjoncteur ouvert ni fait laissé par un autre test.
     */
    public static class MainCouranteProfile extends ChaosTestProfile {
    }
}
