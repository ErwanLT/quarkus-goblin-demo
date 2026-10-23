package fr.eletutour.tavern.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Le coursier vers la guilde des marchands : un appel sortant, donc la couche {@code HTTP_OUT} de Quarkus Goblin.
 */
@RegisterRestClient(configKey = "guilde-des-marchands")
@Path("/guilde-des-marchands")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface MerchantGuildClient {

    @POST
    @Path("/livraisons")
    Delivery deliver(DeliveryRequest request);
}
