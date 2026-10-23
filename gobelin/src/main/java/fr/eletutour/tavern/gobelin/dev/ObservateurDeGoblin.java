package fr.eletutour.tavern.gobelin.dev;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import fr.eletutour.tavern.gobelin.TemoinDesMefaits;
import io.quarkiverse.goblin.AssaultEngine;
import io.quarkiverse.goblin.AssaultObserver;

/**
 * Branché sur le point d'extension de Goblin, {@link AssaultObserver}, comme ses modules métriques et traces : chaque
 * assaut, et chaque réveil ou coucher, est rapporté au témoin de la taverne. Il tourne sur le thread de l'assaut.
 */
@ApplicationScoped
public class ObservateurDeGoblin implements AssaultObserver {

    @Inject
    TemoinDesMefaits temoin;

    @Override
    public void onAssault(AssaultEngine.AssaultRecord assaut) {
        temoin.assaut(assaut.sourceTag() + "/" + assaut.type(),
                assaut.type() + " sur " + assaut.method() + " (" + assaut.configSnapshot() + ")",
                assaut.latencyMs() > 0 ? assaut.latencyMs() : null);
    }

    @Override
    public void onActiveChange(boolean actif) {
        temoin.eveil(actif);
    }
}
