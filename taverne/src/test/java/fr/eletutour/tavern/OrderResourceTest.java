package fr.eletutour.tavern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class OrderResourceTest {

    @Test
    void uneCommandeEstServieEncaisseeEtPuiseeDansLaCave() {
        int adventurer = inscrire("Gimli", 100);
        int hydromel = recette("Hydromel de l'Elfe");
        int maltAvant = quantite("Malt de Nain");

        given().contentType(ContentType.JSON)
                .body(commande(adventurer, hydromel, 2))
                .when().post("/commandes")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("adventurerName", equalTo("Gimli"))
                .body("recipeTitle", equalTo("Hydromel de l'Elfe"))
                .body("total", equalTo(16))
                .body("status", equalTo("SERVIE"));

        given().when().get("/aventuriers/" + adventurer).then().body("gold", equalTo(84));
        given().when().get("/aventuriers/" + adventurer + "/commandes").then().body("size()", equalTo(1));
        org.junit.jupiter.api.Assertions.assertEquals(maltAvant - 2, quantite("Malt de Nain"));
    }

    @Test
    void uneBourseInsuffisanteEstExpliquee() {
        int adventurer = inscrire("Pip", 5);

        given().contentType(ContentType.JSON)
                .body(commande(adventurer, recette("Ragoût de Basilic"), 1))
                .when().post("/commandes")
                .then().statusCode(422)
                .contentType("application/problem+json")
                .body("code", equalTo("bourse-insuffisante"))
                .body("prix", equalTo(14))
                .body("bourse", equalTo(5));
    }

    @Test
    void uneRuptureDeStockListeLesIngredientsManquants() {
        int adventurer = inscrire("Crésus", 50_000);

        given().contentType(ContentType.JSON)
                .body(commande(adventurer, recette("Élixir du Phénix"), 10))
                .when().post("/commandes")
                .then().statusCode(409)
                .body("code", equalTo("rupture-de-stock"))
                .body("ingredientsManquants", hasItem("Queue de Phénix"));

        // la transaction est annulée : la bourse n'a pas été débitée
        given().when().get("/aventuriers/" + adventurer).then().body("gold", equalTo(50_000));
    }

    @Test
    void uneCommandeInvalideEstRefusee() {
        given().contentType(ContentType.JSON)
                .body("{\"recipeId\":1,\"quantity\":0}")
                .when().post("/commandes")
                .then().statusCode(400)
                .body("errors.size()", equalTo(2));
    }

    @Test
    void unAventurierInconnuNePeutPasCommander() {
        given().contentType(ContentType.JSON)
                .body(commande(9999, recette("Tartine du voyageur"), 1))
                .when().post("/commandes")
                .then().statusCode(404)
                .body("code", equalTo("aventurier-introuvable"));
    }

    static int inscrire(String name, int gold) {
        return given().contentType(ContentType.JSON)
                .body("{\"name\":\"" + name + "\",\"adventurerClass\":\"GUERRIER\",\"gold\":" + gold + "}")
                .when().post("/aventuriers")
                .then().statusCode(201)
                .extract().path("id");
    }

    static int recette(String title) {
        return given().when().get("/grimoire/recettes")
                .then().statusCode(200)
                .extract().path("find { it.title == '" + title.replace("'", "\\'") + "' }.id");
    }

    static int quantite(String ingredient) {
        return given().when().get("/cave/stocks")
                .then().statusCode(200)
                .extract().path("find { it.ingredientName == '" + ingredient + "' }.quantity");
    }

    static String commande(int adventurer, int recipe, int quantity) {
        return "{\"adventurerId\":" + adventurer + ",\"recipeId\":" + recipe + ",\"quantity\":" + quantity + "}";
    }
}
