package fr.eletutour.tavern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.goblin.AssaultEngine;
import io.quarkiverse.goblin.ChaosLayer;
import io.quarkiverse.goblin.MutableAssaultConfig;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

/**
 * Le gobelin passe à l'attaque : on vérifie que les vieilles lois de la taverne tiennent vraiment.
 */
@QuarkusTest
@TestProfile(ChaosTestProfile.class)
class ChaosResilienceTest {

    @Inject
    AssaultEngine engine;

    MutableAssaultConfig config;

    @BeforeEach
    void armerLeGobelin() {
        config = engine.getMutableConfig();
        config.setLatencyEnabled(false);
        config.setExceptionEnabled(false);
        config.setClientLatencyEnabled(false);
        config.setClientExceptionEnabled(false);
        config.setTargetLevel(100);
        engine.clearHistory();
        engine.setActive(true);
    }

    @AfterEach
    void desarmerLeGobelin() {
        engine.setActive(false);
    }

    @Test
    void quandLaCaveTombeLeTavernierLitLArdoise() {
        config.setLayers(Set.of(ChaosLayer.DATABASE));
        config.setExceptionEnabled(true);

        given().when().get("/grimoire/recettes")
                .then().statusCode(200)
                .body("title", hasItems("Hydromel de l'Elfe", "Ragoût de Basilic"));

        long databaseFaults = engine.getHistory().stream().filter(r -> "database".equals(r.sourceTag())).count();
        assertTrue(databaseFaults >= 3, "la première lecture et les deux nouvelles tentatives ont échoué : " + databaseFaults);
    }

    @Test
    void uneCuisineTropLenteDeclencheLeTimeout() {
        config.setLayers(Set.of(ChaosLayer.SERVICE));
        config.setLatencyEnabled(true);
        config.setLatencyRange(1800, 2000);
        int adventurer = OrderResourceTest.inscrire("Legolas", 100);

        given().contentType(ContentType.JSON)
                .body(OrderResourceTest.commande(adventurer, OrderResourceTest.recette("Tartine du voyageur"), 1))
                .when().post("/commandes")
                .then().statusCode(504)
                .contentType("application/problem+json")
                .body("faultTolerance", equalTo("TimeoutException"));

        // la transaction de la commande est annulée
        given().when().get("/aventuriers/" + adventurer).then().body("gold", equalTo(100));
    }

    @Test
    void quandLeMarchandNeVientPasLaCaveLeDit() {
        config.setLayers(Set.of(ChaosLayer.HTTP_OUT));
        config.setClientExceptionEnabled(true);

        given().contentType(ContentType.JSON)
                .body("{\"quantity\":5}")
                .when().post("/cave/stocks/1/reapprovisionnement")
                .then().statusCode(200)
                .body("status", equalTo("MARCHAND_ABSENT"))
                .body("delivered", equalTo(0));

        long clientFaults = engine.getHistory().stream().filter(r -> "rest-client".equals(r.sourceTag())).count();
        assertTrue(clientFaults >= 3, "l'appel et ses deux nouvelles tentatives ont été attaqués : " + clientFaults);
    }

    @Test
    void unDisjoncteurOuvertRepondSansAttendreLesNouvellesTentatives() {
        config.setLayers(Set.of(ChaosLayer.HTTP_OUT));
        config.setClientExceptionEnabled(true);
        // assez d'échecs pour ouvrir le disjoncteur (6 appels, 50 % d'échecs)
        for (int i = 0; i < 3; i++) {
            given().contentType(ContentType.JSON).body("{\"quantity\":1}")
                    .when().post("/cave/stocks/2/reapprovisionnement").then().statusCode(200);
        }

        long start = System.nanoTime();
        given().contentType(ContentType.JSON).body("{\"quantity\":1}")
                .when().post("/cave/stocks/2/reapprovisionnement")
                .then().statusCode(200)
                .body("status", equalTo("MARCHAND_ABSENT"));
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertTrue(millis < 300, "le repli doit être immédiat, sans les 2 x 200 ms de nouvelles tentatives : " + millis + " ms");
    }
}
