package fr.eletutour.tavern.incident;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.goblin.AssaultEngine;
import io.quarkiverse.goblin.AssaultObserver;

/**
 * Le témoin des méfaits du gobelin : chaque assaut, et chaque réveil ou coucher, est noté dans la main courante. C'est
 * ce qui permet au post-mortem de nommer la cause d'un incident provoqué par le chaos, plutôt que de la deviner.
 * <p>
 * Branché sur le point d'extension de Goblin, {@link AssaultObserver}, comme ses modules métriques et traces. Il tourne
 * sur le thread de l'assaut : il ne fait que poser le fait dans la file de la main courante.
 */
@ApplicationScoped
public class TemoinDuGobelin implements AssaultObserver {

    @Inject
    MainCourante mainCourante;

    @Override
    public void onAssault(AssaultEngine.AssaultRecord assaut) {
        mainCourante.noter(TypeDeFait.ASSAUT, assaut.sourceTag() + "/" + assaut.type(),
                assaut.type() + " sur " + assaut.method() + " (" + assaut.configSnapshot() + ")",
                assaut.latencyMs() > 0 ? assaut.latencyMs() : null);
    }

    @Override
    public void onActiveChange(boolean actif) {
        mainCourante.noter(actif ? TypeDeFait.GOBELIN_REVEILLE : TypeDeFait.GOBELIN_ENDORMI, null,
                actif ? "Le gobelin se met à rôder" : "Le gobelin s'endort", null);
    }
}
