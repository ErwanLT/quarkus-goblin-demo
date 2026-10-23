package fr.eletutour.tavern.gobelin;

/**
 * Celui à qui le gobelin rend compte de ses méfaits : chaque assaut, chaque réveil, chaque coucher. Implémenté par
 * l'application, appelé par le module {@code gobelin} sur le thread de l'assaut : il ne doit ni bloquer ni échouer.
 */
public interface TemoinDesMefaits {

    /**
     * Un assaut vient d'avoir lieu.
     *
     * @param cle la source et le type de l'assaut, par exemple {@code server/LATENCY}
     * @param detail ce qui a été attaqué, et avec quelle configuration
     * @param latenceMs la latence injectée, ou {@code null} quand l'assaut n'en injecte pas
     */
    void assaut(String cle, String detail, Long latenceMs);

    /**
     * Le gobelin se réveille ou s'endort.
     *
     * @param actif {@code true} quand il se réveille
     */
    void eveil(boolean actif);
}
