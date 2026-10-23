package fr.eletutour.tavern.salle;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

import fr.eletutour.tavern.event.CommandeAuComptoir;

/**
 * Regarde passer les commandes au comptoir. Une commande qui dépasse son {@code @Timeout} ou qui tombe en erreur ne
 * sort jamais du service : seule la réponse HTTP la raconte. Le filtre est {@link PreMatching} pour démarrer le
 * chronomètre avant tout autre filtre, y compris ceux du gobelin.
 */
@Provider
@PreMatching
public class SalleComptoirFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String DEBUT = SalleComptoirFilter.class.getName() + ".debut";

    @Inject
    Event<CommandeAuComptoir> comptoir;

    @Override
    public void filter(ContainerRequestContext request) {
        if (estUneCommande(request)) {
            request.setProperty(DEBUT, System.nanoTime());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        if (request.getProperty(DEBUT) instanceof Long debut) {
            comptoir.fire(new CommandeAuComptoir(response.getStatus(), response.getEntity(),
                    (System.nanoTime() - debut) / 1_000_000));
        }
    }

    private static boolean estUneCommande(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath();
        return "POST".equals(request.getMethod()) && ("/commandes".equals(path) || "commandes".equals(path));
    }
}
