package fr.eletutour.tavern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class CellarResourceTest {

    @Test
    void laGuildeDesMarchandsLivreParLeRestClient() {
        int basilic = given().when().get("/cave/stocks")
                .then().extract().path("find { it.ingredientName == 'Basilic séché' }.ingredientId");
        int avant = given().when().get("/cave/stocks/" + basilic).then().extract().path("quantity");

        given().contentType(ContentType.JSON)
                .body("{\"quantity\":20}")
                .when().post("/cave/stocks/" + basilic + "/reapprovisionnement")
                .then().statusCode(200)
                .body("status", equalTo("LIVRE"))
                .body("delivered", equalTo(20))
                .body("message", startsWith("Livré par le chariot d"))
                .body("stock.quantity", equalTo(avant + 20));
    }

    @Test
    void uneQuantiteExtravaganteEstRefusee() {
        given().contentType(ContentType.JSON)
                .body("{\"quantity\":10000}")
                .when().post("/cave/stocks/1/reapprovisionnement")
                .then().statusCode(400)
                .body("errors[0]", notNullValue());
    }

    @Test
    void uneEtagereInconnueRenvoieUn404() {
        given().when().get("/cave/stocks/9999")
                .then().statusCode(404)
                .body("code", equalTo("stock-introuvable"));
    }
}
