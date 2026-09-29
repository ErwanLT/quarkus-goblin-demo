package fr.eletutour.tavern.incident;

/**
 * Où en est un incident.
 */
public enum StatutIncident {
    /** La taverne souffre encore, ou vient d'aller mieux sans que le calme soit revenu assez longtemps. */
    EN_COURS,
    /** Le calme est revenu : aucune dégradation depuis la période de calme, disjoncteur refermé. */
    CLOS,
    /** La taverne a redémarré pendant l'incident : sa fin n'a pas pu être observée. */
    INTERROMPU
}
