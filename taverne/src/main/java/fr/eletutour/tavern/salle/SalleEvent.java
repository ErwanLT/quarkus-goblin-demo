package fr.eletutour.tavern.salle;

import java.time.Instant;
import java.util.Map;

/**
 * Un événement de la salle, poussé en direct à l'écran de démo.
 *
 * @param type {@code commande}, {@code carte}, {@code livraison}, {@code disjoncteur} ou {@code etat}
 * @param at horodatage de l'événement
 * @param data contenu de l'événement, propre à chaque type
 */
public record SalleEvent(String type, Instant at, Map<String, Object> data) {

    static SalleEvent of(String type, Map<String, Object> data) {
        return new SalleEvent(type, Instant.now(), data);
    }
}
