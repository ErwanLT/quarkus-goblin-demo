package fr.eletutour.tavern.resource;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import fr.eletutour.tavern.client.Delivery;
import fr.eletutour.tavern.client.DeliveryRequest;

/**
 * La guilde des marchands, fournisseur « distant » simulé par l'application elle-même : la cave l'appelle via le REST
 * Client {@link fr.eletutour.tavern.client.MerchantGuildClient}, ce qui donne un vrai appel HTTP sortant à perturber.
 */
@Path("/guilde-des-marchands")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = TavernApi.GUILDE)
public class MerchantGuildResource {

    private static final Logger LOG = Logger.getLogger(MerchantGuildResource.class);

    private static final List<String> CARRIERS = List.of("Borin", "Elwen", "Grimsby", "Maëlys", "Thorgar");

    @POST
    @Path("/livraisons")
    @Operation(summary = "Livrer un ingrédient", description = "Simule une livraison, avec un temps de trajet de 30 à 150 ms.")
    public Delivery livrer(DeliveryRequest request) throws InterruptedException {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long trajet = random.nextLong(30, 150);
        Thread.sleep(trajet);
        String carrier = CARRIERS.get(random.nextInt(CARRIERS.size()));
        LOG.infof("La guilde livre %d %s, chariot de %s (%d ms de trajet)", (Object) request.quantity(), request.ingredient(),
                carrier, trajet);
        return new Delivery(request.ingredient(), request.quantity(), carrier);
    }
}
