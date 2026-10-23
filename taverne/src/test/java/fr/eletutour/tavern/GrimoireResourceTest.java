package fr.eletutour.tavern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class GrimoireResourceTest {

    @Test
    void laReserveEstConsultable() {
        given().when().get("/grimoire/ingredients")
                .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(8))
                .body("name", hasItems("Queue de Phénix", "Malt de Nain", "Eau de source elfique"));
    }

    @Test
    void leGrimoireExposeSesRecettesAvecLeursIngredients() {
        given().when().get("/grimoire/recettes")
                .then().statusCode(200)
                .body("title", hasItems("Hydromel de l'Elfe", "Ragoût de Basilic"))
                .body("find { it.title == \"Hydromel de l'Elfe\" }.ingredients.name",
                        hasItems("Malt de Nain", "Eau de source elfique", "Miel sauvage"));
    }

    @Test
    void laRechercheTrouveLaBonneRecette() {
        given().queryParam("titre", "hydromel").when().get("/grimoire/recettes/recherche")
                .then().statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].title", equalTo("Hydromel de l'Elfe"));
    }

    @Test
    void unIngredientSAjouteEtObtientUneEtagereVide() {
        int id = given().contentType(ContentType.JSON)
                .body("{\"name\":\"Poudre de licorne\",\"unit\":\"gramme\",\"cost\":42.0}")
                .when().post("/grimoire/ingredients")
                .then().statusCode(201)
                .header("Location", containsString("/grimoire/ingredients/"))
                .body("id", notNullValue())
                .extract().path("id");

        given().when().get("/cave/stocks/" + id)
                .then().statusCode(200)
                .body("ingredientName", equalTo("Poudre de licorne"))
                .body("quantity", equalTo(0));
    }

    @Test
    void unIngredientEnDoubleEstRefuseAuFormatProblem() {
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"malt de nain\",\"unit\":\"kg\",\"cost\":5.0}")
                .when().post("/grimoire/ingredients")
                .then().statusCode(409)
                .contentType("application/problem+json")
                .body("title", equalTo("Conflit de données"))
                .body("code", equalTo("ingredient-existant"))
                .body("type", equalTo("urn:falling-whale:problem:ingredient-existant"))
                .body("instance", containsString("/grimoire/ingredients"));
    }

    @Test
    void uneCommandeMalRedigeeListeSesErreurs() {
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"\",\"unit\":\"kg\",\"cost\":-1}")
                .when().post("/grimoire/ingredients")
                .then().statusCode(400)
                .contentType("application/problem+json")
                .body("title", equalTo("Erreur de validation des données"))
                .body("errors", hasItem(containsString("name")))
                .body("errors", hasItem(containsString("cost")));
    }

    @Test
    void uneRecetteInconnueRenvoieUn404() {
        given().when().get("/grimoire/recettes/9999")
                .then().statusCode(404)
                .contentType("application/problem+json")
                .body("detail", equalTo("La recette 9999 n'existe pas dans le grimoire."));
    }

    @Test
    void uneRecetteAvecUnIngredientInconnuEstRefusee() {
        given().contentType(ContentType.JSON)
                .body("{\"title\":\"Soupe fantôme\",\"price\":5,\"ingredientIds\":[9999]}")
                .when().post("/grimoire/recettes")
                .then().statusCode(404)
                .body("code", equalTo("ingredient-introuvable"));
    }

    @Test
    void uneRouteInconnueRestePolie() {
        given().when().get("/grimoire/parchemins")
                .then().statusCode(404)
                .contentType("application/problem+json")
                .body("status", equalTo(404));
    }
}
