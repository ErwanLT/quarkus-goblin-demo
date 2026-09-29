package fr.eletutour.tavern.salle;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import fr.eletutour.tavern.dto.AdventurerDTO;
import fr.eletutour.tavern.dto.OrderDTO;
import fr.eletutour.tavern.dto.RecipeDTO;
import fr.eletutour.tavern.event.CarteServie;
import fr.eletutour.tavern.event.CommandeAuComptoir;
import fr.eletutour.tavern.event.Reapprovisionnement;
import fr.eletutour.tavern.exception.model.Problem;
import fr.eletutour.tavern.incident.MainCourante;
import fr.eletutour.tavern.service.AdventurerService;
import fr.eletutour.tavern.service.GrimoireService;
import fr.eletutour.tavern.service.MerchantService;
import io.quarkiverse.goblin.AssaultEngine;
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
 * <p>
 * La page elle-même ne touche jamais la base : la carte et les bourses sont lues au démarrage, puis les bourses sont
 * tenues à jour par les commandes servies. L'écran de démo reste debout même quand le gobelin met le feu à la cave.
 */
@ApplicationScoped
public class SalleService {

    private static final Duration ETAT_PERIODE = Duration.ofSeconds(2);

    @Inject
    AssaultEngine gobelin;

    @Inject
    CircuitBreakerMaintenance disjoncteurs;

    @Inject
    GrimoireService grimoireService;

    @Inject
    AdventurerService adventurerService;

    @Inject
    MainCourante mainCourante;

    private volatile List<RecipeDTO> carteDuJour = List.of();
    private final Map<Long, AdventurerDTO> bourses = new ConcurrentHashMap<>();

    private final BroadcastProcessor<SalleEvent> diffusion = BroadcastProcessor.create();

    private final AtomicReference<CarteServie.Source> carte = new AtomicReference<>(CarteServie.Source.FRAICHE);
    private final AtomicReference<Instant> carteDepuis = new AtomicReference<>(Instant.now());
    private final AtomicLong ardoisesServies = new AtomicLong();

    /**
     * Ouvre la salle, après que la cave a été remplie : aucun gobelin n'agit au démarrage, la lecture est sûre.
     */
    void onStart(@Observes @Priority(Interceptor.Priority.APPLICATION + 1000) StartupEvent event) {
        carteDuJour = grimoireService.consulterLeGrimoire();
        adventurerService.consulterLeRegistre().forEach(aventurier -> bourses.put(aventurier.id(), aventurier));
        disjoncteurs.onStateChange(MerchantService.DISJONCTEUR,
                state -> publier(SalleEvent.of("disjoncteur", Map.of("etat", state.name()))));
    }

    /**
     * @return la carte du jour, telle qu'elle était à l'ouverture de la taverne
     */
    public List<RecipeDTO> carteDuJour() {
        return carteDuJour;
    }

    /**
     * @return les aventuriers et leur bourse, tenue à jour par les commandes servies, par ordre alphabétique
     */
    public List<AdventurerDTO> bourses() {
        return bourses.values().stream().sorted(Comparator.comparing(AdventurerDTO::name)).toList();
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
     * @param commande la réponse du comptoir : {@link OrderDTO} quand la commande est servie, {@link Problem} sinon
     */
    void commande(@Observes CommandeAuComptoir commande) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("statut", statut(commande.status()));
        data.put("status", commande.status());
        data.put("dureeMs", commande.dureeMs());
        switch (commande.reponse()) {
            case OrderDTO order -> {
                bourses.computeIfPresent(order.adventurerId(),
                        (id, aventurier) -> new AdventurerDTO(id, aventurier.name(), aventurier.adventurerClass(),
                                order.adventurerGold()));
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
        data.put("incident", incidentData());
        return SalleEvent.of("etat", data);
    }

    private Map<String, Object> carteData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("source", carte.get().name());
        data.put("depuis", carteDepuis.get().toString());
        data.put("ardoisesServies", ardoisesServies.get());
        return data;
    }

    /**
     * L'incident en cours tel que la main courante le tient, ou le dernier incident terminé et son post-mortem.
     */
    private Map<String, Object> incidentData() {
        Map<String, Object> data = new LinkedHashMap<>();
        mainCourante.incidentEnCours().ifPresent(incident -> {
            data.put("id", incident.id());
            data.put("depuis", incident.depuis().toString());
            data.put("declencheur", incident.declencheur());
        });
        mainCourante.dernierIncidentTermine().ifPresent(id -> data.put("dernier", id));
        return data;
    }

    private CircuitBreakerState etatDuDisjoncteur() {
        return disjoncteurs.currentState(MerchantService.DISJONCTEUR);
    }

    /**
     * Ce que fait le gobelin, en mots de la taverne : ce qu'il injecte, et sur quelle part des requêtes.
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
