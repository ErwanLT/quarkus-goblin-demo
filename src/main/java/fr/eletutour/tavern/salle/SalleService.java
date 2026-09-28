package fr.eletutour.tavern.salle;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

import fr.eletutour.tavern.dto.OrderDTO;
import fr.eletutour.tavern.event.CarteServie;
import fr.eletutour.tavern.event.Reapprovisionnement;
import fr.eletutour.tavern.exception.model.Problem;
import fr.eletutour.tavern.service.MerchantService;
import io.quarkiverse.goblin.AssaultEngine;
import io.quarkiverse.goblin.ChaosLayer;
import io.quarkiverse.goblin.MutableAssaultConfig;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

/**
 * Ce que voient les clients de la taverne : les commandes au comptoir, la carte servie, les livraisons de la guilde et
 * l'état du disjoncteur. La salle observe l'application, elle ne simule rien : chaque événement vient d'une vraie
 * requête, d'un vrai repli ou d'un vrai changement d'état.
 */
@ApplicationScoped
public class SalleService {

    private static final Duration ETAT_PERIODE = Duration.ofSeconds(2);

    @Inject
    AssaultEngine gobelin;

    @Inject
    CircuitBreakerMaintenance disjoncteurs;

    private final BroadcastProcessor<SalleEvent> diffusion = BroadcastProcessor.create();

    private final AtomicReference<CarteServie.Source> carte = new AtomicReference<>(CarteServie.Source.FRAICHE);
    private final AtomicReference<Instant> carteDepuis = new AtomicReference<>(Instant.now());
    private final AtomicLong ardoisesServies = new AtomicLong();

    void onStart(@Observes StartupEvent event) {
        disjoncteurs.onStateChange(MerchantService.DISJONCTEUR,
                state -> publier(SalleEvent.of("disjoncteur", Map.of("etat", state.name()))));
    }

    /**
     * Le flux de la salle : les événements au fil de l'eau, et toutes les deux secondes l'état complet (gobelin,
     * disjoncteur, carte), pour qu'un écran ouvert en cours de soirée soit à jour tout de suite.
     */
    public Multi<SalleEvent> flux() {
        Multi<SalleEvent> etat = Multi.createFrom().ticks().startingAfter(Duration.ofMillis(10)).every(ETAT_PERIODE)
                .onOverflow().drop()
                .map(tick -> etat());
        return Multi.createBy().merging().streams(diffusion, etat);
    }

    /**
     * Une commande vient de quitter le comptoir, servie ou non.
     *
     * @param status statut HTTP de la réponse
     * @param entity {@link OrderDTO} quand la commande est servie, {@link Problem} sinon
     * @param dureeMs temps passé au comptoir
     */
    void commande(int status, Object entity, long dureeMs) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("statut", statut(status));
        data.put("status", status);
        data.put("dureeMs", dureeMs);
        switch (entity) {
            case OrderDTO order -> {
                data.put("aventurierId", order.adventurerId());
                data.put("aventurier", order.adventurerName());
                data.put("recette", order.recipeTitle());
                data.put("quantite", order.quantity());
                data.put("total", order.total());
                data.put("bourse", order.adventurerGold());
            }
            case Problem problem -> {
                data.put("titre", problem.getTitle());
                data.put("detail", problem.getDetail());
            }
            case null, default -> {
                // un corps inattendu (réponse tronquée par le gobelin, par exemple) : le statut suffit
            }
        }
        publier(SalleEvent.of("commande", data));
    }

    void carteServie(@Observes CarteServie servie) {
        if (servie.source() == CarteServie.Source.ARDOISE) {
            ardoisesServies.incrementAndGet();
        }
        CarteServie.Source precedente = carte.getAndSet(servie.source());
        if (precedente != servie.source()) {
            carteDepuis.set(Instant.now());
            publier(SalleEvent.of("carte", carteData()));
        }
    }

    void reapprovisionnement(@Observes(during = TransactionPhase.AFTER_COMPLETION) Reapprovisionnement livraison) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ingredient", livraison.ingredient());
        data.put("livree", livraison.livree());
        data.put("quantite", livraison.livre());
        data.put("chariot", livraison.chariot());
        data.put("stock", livraison.stock());
        publier(SalleEvent.of("livraison", data));
    }

    private SalleEvent etat() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("gobelin", etatDuGobelin());
        data.put("disjoncteur", etatDuDisjoncteur().name());
        data.put("carte", carteData());
        return SalleEvent.of("etat", data);
    }

    private Map<String, Object> carteData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("source", carte.get().name());
        data.put("depuis", carteDepuis.get().toString());
        data.put("ardoisesServies", ardoisesServies.get());
        return data;
    }

    private CircuitBreakerState etatDuDisjoncteur() {
        return disjoncteurs.currentState(MerchantService.DISJONCTEUR);
    }

    /**
     * Ce que fait le gobelin, en mots de la taverne : où il frappe, comment, et sur quelle part des requêtes.
     */
    private Map<String, Object> etatDuGobelin() {
        Map<String, Object> data = new LinkedHashMap<>();
        MutableAssaultConfig config = gobelin.getMutableConfig();
        boolean actif = gobelin.isActive() && config != null;
        data.put("actif", actif);
        if (!actif) {
            return data;
        }
        MutableAssaultConfig vue = config.snapshot();
        data.put("niveau", vue.getTargetLevel());
        data.put("couches", vue.getLayers().stream().map(SalleService::lieu).toList());
        List<String> assauts = new ArrayList<>();
        long[] latence = vue.getLatencyRange();
        if (vue.isLatencyEnabled()) {
            assauts.add("latence " + latence[0] + "-" + latence[1] + " ms");
        }
        if (vue.isExceptionEnabled()) {
            assauts.add("exceptions");
        }
        if (vue.isHttpStatusEnabled()) {
            assauts.add("statut HTTP " + vue.getHttpStatusCode());
        }
        if (vue.isDependencyDegradationEnabled()) {
            assauts.add("dépendance dégradée");
        }
        if (vue.isResponseBodyEnabled()) {
            assauts.add("corps de réponse abîmé");
        }
        if (vue.isResponseHeaderEnabled()) {
            assauts.add("en-têtes modifiés");
        }
        if (vue.isClientLatencyEnabled()) {
            assauts.add("coursiers ralentis");
        }
        if (vue.isClientExceptionEnabled()) {
            assauts.add("coursiers perdus");
        }
        data.put("assauts", assauts);
        data.put("total", gobelin.getTotalAssaultCount());
        return data;
    }

    private static String lieu(ChaosLayer layer) {
        return switch (layer) {
            case DATABASE -> "la cave";
            case MESSAGING -> "la volière";
            case SERVICE -> "les cuisines";
            case HTTP_OUT -> "la porte de derrière";
            case HTTP_IN -> "la porte d'entrée";
        };
    }

    private static String statut(int status) {
        if (status < 300) {
            return "SERVIE";
        }
        if (status == 429) {
            return "REFOULEE";
        }
        if (status == 504) {
            return "ABANDONNEE";
        }
        return status < 500 ? "REFUSEE" : "PERDUE";
    }

    private void publier(SalleEvent event) {
        synchronized (diffusion) {
            diffusion.onNext(event);
        }
    }
}
