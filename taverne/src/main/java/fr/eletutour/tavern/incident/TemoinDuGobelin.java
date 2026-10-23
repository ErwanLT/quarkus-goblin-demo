package fr.eletutour.tavern.incident;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import fr.eletutour.tavern.gobelin.TemoinDesMefaits;

/**
 * Le témoin des méfaits du gobelin : chaque assaut, et chaque réveil ou coucher, est noté dans la main courante. C'est
 * ce qui permet au post-mortem de nommer la cause d'un incident provoqué par le chaos, plutôt que de la deviner.
 * <p>
 * Le module {@code gobelin} l'appelle depuis le point d'extension de Goblin, sur le thread de l'assaut : il ne fait que
 * poser le fait dans la file de la main courante.
 */
@ApplicationScoped
public class TemoinDuGobelin implements TemoinDesMefaits {

    @Inject
    MainCourante mainCourante;

    @Override
    public void assaut(String cle, String detail, Long latenceMs) {
        mainCourante.noter(TypeDeFait.ASSAUT, cle, detail, latenceMs);
    }

    @Override
    public void eveil(boolean actif) {
        mainCourante.noter(actif ? TypeDeFait.GOBELIN_REVEILLE : TypeDeFait.GOBELIN_ENDORMI, null,
                actif ? "Le gobelin se met à rôder" : "Le gobelin s'endort", null);
    }
}
