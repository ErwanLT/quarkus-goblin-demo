package fr.eletutour.tavern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ObservabilityTest {

    @Test
    void laTaverneEstPreteAServir() {
        given().when().get("/q/health/ready")
                .then().statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.find { it.name == 'tavern-readiness' }.status", equalTo("UP"));
    }

    @Test
    void lesMetriquesMetierSontExposees() {
        int adventurer = OrderResourceTest.inscrire("Tauriel", 100);
        given().contentType(ContentType.JSON)
                .body(OrderResourceTest.commande(adventurer, OrderResourceTest.recette("Tartine du voyageur"), 1))
                .when().post("/commandes").then().statusCode(201);

        given().when().get("/q/metrics")
                .then().statusCode(200)
                .body(containsString("tavern_order_seconds_count"))
                .body(containsString("tavern_order_count_total"))
                .body(containsString("tavern_gold_earned_total"))
                .body(containsString("goblin_active"));
    }

    @Test
    void laDocumentationOpenApiDecritLAPI() {
        given().accept("application/json").when().get("/openapi")
                .then().statusCode(200)
                .body(containsString("/grimoire/recettes"))
                .body(containsString("/commandes"))
                .body(containsString("application/problem+json"));
    }
}
