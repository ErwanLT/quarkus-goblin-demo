package fr.eletutour.tavern.event;

/**
 * La carte du grimoire vient d'être servie, fraîche depuis la cave ou recopiée de l'ardoise.
 *
 * @param source d'où vient la carte
 * @param recettes nombre de recettes servies
 */
public record CarteServie(Source source, int recettes) {

    public enum Source {
        /** Lue dans la cave, à jour. */
        FRAICHE,
        /** La cave ne répond plus : dernière carte connue, recopiée de l'ardoise. */
        ARDOISE
    }
}
