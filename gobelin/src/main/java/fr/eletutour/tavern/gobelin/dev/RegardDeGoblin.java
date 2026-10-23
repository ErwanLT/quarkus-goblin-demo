package fr.eletutour.tavern.gobelin.dev;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import fr.eletutour.tavern.gobelin.EtatDuGobelin;
import fr.eletutour.tavern.gobelin.RegardSurLeGobelin;
import io.quarkiverse.goblin.AssaultConfig;
import io.quarkiverse.goblin.AssaultEngine;

/**
 * Le regard de la taverne, branché sur le moteur de Goblin. Il remplace l'implémentation par défaut de l'application,
 * qui ne voit aucun gobelin.
 */
@ApplicationScoped
public class RegardDeGoblin implements RegardSurLeGobelin {

    @Inject
    AssaultEngine gobelin;

    @Override
    public Optional<EtatDuGobelin> etat() {
        if (!gobelin.isActive()) {
            return Optional.empty();
        }
        AssaultConfig vue = gobelin.configSnapshot();
        return Optional.of(new EtatDuGobelin(vue.getTargetLevel(),
                vue.getLayers().stream().map(Enum::name).sorted().toList(),
                vue.isLatencyEnabled(), vue.getLatencyMinMs(), vue.getLatencyMaxMs(),
                vue.isExceptionEnabled(), vue.isHttpStatusEnabled(), vue.getHttpStatusCode(),
                vue.isDependencyDegradationEnabled(), vue.isResponseBodyEnabled(), vue.isResponseHeaderEnabled(),
                vue.isClientLatencyEnabled(), vue.isClientExceptionEnabled(), gobelin.getTotalAssaultCount()));
    }
}
