package fr.eletutour.tavern.gobelin;

import java.util.Optional;

/**
 * Ce que la taverne voit du gobelin : la salle l'affiche, la main courante le fige à l'ouverture d'un incident.
 * <p>
 * Goblin ne doit jamais atteindre la production, alors l'application ne connaît que cette interface. Son implémentation,
 * branchée sur l'API de Goblin, vit dans le module {@code gobelin}, que l'application déclare {@code provided} comme
 * Goblin lui-même : le dev mode et les tests l'ont, l'application packagée jamais. Là, une implémentation par défaut ne
 * voit aucun gobelin.
 */
public interface RegardSurLeGobelin {

    /**
     * @return ce que fait le gobelin, ou rien quand il dort ou qu'il n'est pas là
     */
    Optional<EtatDuGobelin> etat();
}
