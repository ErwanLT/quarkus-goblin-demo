package fr.eletutour.tavern.incident;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

/**
 * Les incidents de la main courante.
 */
@ApplicationScoped
public class IncidentRepository implements PanacheRepository<Incident> {

    /**
     * @param limite nombre maximal d'incidents
     * @return les derniers incidents, du plus récent au plus ancien
     */
    public List<Incident> derniers(int limite) {
        return findAll(Sort.descending("ouvertLe")).page(Page.ofSize(limite)).list();
    }

    /**
     * @return les incidents restés ouverts
     */
    public List<Incident> enCours() {
        return list("statut", StatutIncident.EN_COURS);
    }
}
