package fr.eletutour.tavern.gobelin;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.DefaultBean;

/**
 * Le regard de la taverne quand le module {@code gobelin} n'est pas là, c'est-à-dire dans l'application packagée : il
 * ne voit aucun gobelin. En dev mode et en test, l'implémentation du module le remplace.
 */
@DefaultBean
@ApplicationScoped
public class GobelinAbsent implements RegardSurLeGobelin {

    @Override
    public Optional<EtatDuGobelin> etat() {
        return Optional.empty();
    }
}
