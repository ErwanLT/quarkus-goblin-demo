package fr.eletutour.tavern;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import fr.eletutour.tavern.salle.SalleEvent;
import fr.eletutour.tavern.salle.SalleService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

@QuarkusTest
class SalleTest {

    @Inject
    SalleService salle;

    @Test
    void laSalleAfficheLaCarteEtLesBourses() {
        given().when().get("/salle")
                .then().statusCode(200)
                .contentType(ContentType.HTML)
                .body(containsString("La salle"))
                .body(containsString("Ragoût de Basilic"))
                .body(containsString("Arthas"))
                .body(containsString("/assets/salle.js"));
    }

    @Test
    void uneCommandeServieArriveEnDirectAvecLaBourseRestante() {
        AssertSubscriber<SalleEvent> commandes = abonner("commande");
        int adventurer = OrderResourceTest.inscrire("Samsagace", 100);

        given().contentType(ContentType.JSON)
                .body(OrderResourceTest.commande(adventurer, OrderResourceTest.recette("Tartine du voyageur"), 2))
                .when().post("/commandes").then().statusCode(201);

        Map<String, Object> data = premier(commandes).data();
        assertEquals("SERVIE", data.get("statut"));
        assertEquals(adventurer, ((Number) data.get("aventurierId")).intValue());
        assertEquals(94, ((Number) data.get("bourse")).intValue(), "100 - 2 x 3 pièces d'or");
    }

    @Test
    void uneCommandeRefuseeArriveAvecSaRaison() {
        AssertSubscriber<SalleEvent> commandes = abonner("commande");
        int adventurer = OrderResourceTest.inscrire("Gollum", 1);

        given().contentType(ContentType.JSON)
                .body(OrderResourceTest.commande(adventurer, OrderResourceTest.recette("Élixir du Phénix"), 1))
                .when().post("/commandes").then().statusCode(422);

        Map<String, Object> data = premier(commandes).data();
        assertEquals("REFUSEE", data.get("statut"));
        assertEquals("Bourse insuffisante", data.get("titre"));
    }

    @Test
    void lEtatDeLaSalleDecritLeGobelinEtLeDisjoncteur() {
        Map<String, Object> data = premier(abonner("etat")).data();
        @SuppressWarnings("unchecked")
        Map<String, Object> gobelin = (Map<String, Object>) data.get("gobelin");
        assertFalse((Boolean) gobelin.get("actif"), "le gobelin dort dans les tests");
        assertEquals("CLOSED", data.get("disjoncteur"));
    }

    private AssertSubscriber<SalleEvent> abonner(String type) {
        return salle.flux().filter(event -> type.equals(event.type()))
                .subscribe().withSubscriber(AssertSubscriber.create(10));
    }

    private static SalleEvent premier(AssertSubscriber<SalleEvent> subscriber) {
        subscriber.awaitItems(1, Duration.ofSeconds(10));
        subscriber.cancel();
        return subscriber.getItems().getFirst();
    }
}
