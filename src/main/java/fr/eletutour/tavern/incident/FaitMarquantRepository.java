package fr.eletutour.tavern.incident;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;

/**
 * Les faits consignés dans la main courante.
 */
@ApplicationScoped
public class FaitMarquantRepository implements PanacheRepository<FaitMarquant> {

    /**
     * @param incident l'incident
     * @return ses faits, dans l'ordre où ils se sont produits
     */
    public List<FaitMarquant> deLIncident(Incident incident) {
        return list("incident", Sort.ascending("horodatage", "id"), incident);
    }
}
