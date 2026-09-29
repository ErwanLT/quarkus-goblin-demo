package fr.eletutour.tavern.incident;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.eletutour.tavern.dto.OrderDTO;
import fr.eletutour.tavern.event.CarteServie;
import fr.eletutour.tavern.event.CommandeAuComptoir;
import fr.eletutour.tavern.event.Reapprovisionnement;
import fr.eletutour.tavern.exception.model.Problem;
import fr.eletutour.tavern.service.MerchantService;
import io.quarkiverse.goblin.AssaultEngine;
import io.quarkiverse.goblin.MutableAssaultConfig;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;

/**
 * La main courante du tavernier : le journal des incidents, tenu fait par fait pendant la soirée, dont le post-mortem
 * est tiré le lendemain.
 * <p>
 * Les faits arrivent de partout -- le comptoir, le grimoire, la cave, le disjoncteur, le gobelin -- et sur les threads
 * des requêtes. Ils sont d'abord posés dans une file en mémoire, sans jamais bloquer ni échouer, puis une tâche planifiée
 * les consigne en base. Le journal ne brûle donc pas avec la taverne : une requête attaquée par le gobelin n'écrit jamais
 * elle-même dans la main courante, et si la base est injoignable, les faits attendent la consignation suivante.
 * <p>
 * Un incident s'ouvre au premier signe de dégradation ({@link TypeDeFait#degradation()}), et se clôt quand aucune
 * dégradation n'a été vue pendant la période de calme, disjoncteur refermé. En dehors d'un incident, seuls les signes de
 * dégradation sont retenus : une soirée ordinaire ne remplit pas le journal.
 */
@ApplicationScoped
public class MainCourante {

    private static final Logger LOG = Logger.getLogger(MainCourante.class);

    /** Au-delà, les faits sont comptés mais plus retenus : la mémoire de la taverne n'est pas infinie. */
    private static final int CAPACITE = 50_000;

    @Inject
    IncidentRepository incidents;

    @Inject
    FaitMarquantRepository faits;

    @Inject
    CircuitBreakerMaintenance disjoncteurs;

    @Inject
    AssaultEngine gobelin;

    @Inject
    ObjectMapper json;

    @ConfigProperty(name = "taverne.main-courante.calme", defaultValue = "30s")
    Duration calme;

    private final ConcurrentLinkedQueue<Fait> enAttente = new ConcurrentLinkedQueue<>();
    private final AtomicInteger taille = new AtomicInteger();
    private final AtomicLong oublies = new AtomicLong();
    private final AtomicReference<CarteServie.Source> carte = new AtomicReference<>(CarteServie.Source.FRAICHE);

    /** Les faits d'une consignation échouée, repris en tête de la suivante. */
    private final List<Fait> aReprendre = new ArrayList<>();

    /** Tenus par la seule consignation, sous le verrou de l'instance. */
    private Long incidentEnCours;
    private Instant derniereDegradation;

    /** Publiés pour la salle, lus sans verrou. */
    private volatile IncidentEnCours enCours;
    private volatile Long dernierClos;

    /**
     * Un fait en attente de consignation.
     */
    record Fait(Instant horodatage, TypeDeFait type, String cle, String detail, Long valeur) {
    }

    /**
     * L'incident en cours, tel que la salle l'affiche.
     *
     * @param id identifiant de l'incident
     * @param depuis heure de son ouverture
     * @param declencheur le premier signe de dégradation
     */
    public record IncidentEnCours(long id, Instant depuis, String declencheur) {
    }

    /**
     * À l'ouverture : un incident resté ouvert vient d'une soirée interrompue par un redémarrage, sa fin n'a pas été
     * observée. Puis la main courante commence à suivre le disjoncteur de la guilde.
     */
    void onStart(@Observes @Priority(Interceptor.Priority.APPLICATION + 900) StartupEvent event) {
        QuarkusTransaction.requiringNew().run(() -> incidents.enCours().forEach(incident -> {
            incident.statut = StatutIncident.INTERROMPU;
            // la fin n'a pas été observée : l'incident s'arrête, au plus tard, au redémarrage
            incident.closLe = Instant.now();
            consigner(incident, new Fait(Instant.now(), TypeDeFait.CLOTURE, null,
                    "La taverne a redémarré pendant l'incident : sa fin n'a pas pu être observée", null));
            LOG.warnf("Incident %d interrompu par un redémarrage de la taverne", incident.id);
        }));
        dernierClos = QuarkusTransaction.requiringNew().call(
                () -> incidents.derniers(1).stream().map(incident -> incident.id).findFirst().orElse(null));
        disjoncteurs.onStateChange(MerchantService.DISJONCTEUR, this::disjoncteur);
    }

    /**
     * Note un fait, sur n'importe quel thread : ne bloque jamais, n'échoue jamais.
     *
     * @param type le type du fait
     * @param cle sa clé de regroupement, ou {@code null}
     * @param detail ce qui s'est passé, en mots de la taverne
     * @param valeur une mesure, ou {@code null}
     */
    public void noter(TypeDeFait type, String cle, String detail, Long valeur) {
        if (taille.incrementAndGet() > CAPACITE) {
            taille.decrementAndGet();
            oublies.incrementAndGet();
            return;
        }
        enAttente.add(new Fait(Instant.now(), type, cle, detail, valeur));
    }

    /**
     * @return l'incident en cours, s'il y en a un
     */
    public Optional<IncidentEnCours> incidentEnCours() {
        return Optional.ofNullable(enCours);
    }

    /**
     * @return le dernier incident terminé, dont le post-mortem peut être lu
     */
    public Optional<Long> dernierIncidentTermine() {
        return Optional.ofNullable(dernierClos);
    }

    void commande(@Observes CommandeAuComptoir commande) {
        int status = commande.status();
        TypeDeFait type;
        if (status < 300) {
            type = TypeDeFait.COMMANDE_SERVIE;
        } else if (status == 429) {
            type = TypeDeFait.COMMANDE_REFOULEE;
        } else if (status == 504) {
            type = TypeDeFait.COMMANDE_ABANDONNEE;
        } else if (status >= 500) {
            type = TypeDeFait.COMMANDE_PERDUE;
        } else {
            type = TypeDeFait.COMMANDE_REFUSEE;
        }
        String detail = switch (commande.reponse()) {
            case OrderDTO order -> order.adventurerName() + " : " + order.quantity() + " x " + order.recipeTitle();
            case Problem problem -> problem.getTitle();
            case null, default -> "réponse " + status;
        };
        noter(type, String.valueOf(status), detail, commande.dureeMs());
    }

    void carteServie(@Observes CarteServie servie) {
        CarteServie.Source precedente = carte.getAndSet(servie.source());
        if (servie.source() == CarteServie.Source.ARDOISE) {
            noter(TypeDeFait.CARTE_ARDOISE, null, servie.recettes() + " recette(s) servies depuis l'ardoise", null);
        } else if (precedente == CarteServie.Source.ARDOISE) {
            noter(TypeDeFait.CARTE_RETABLIE, null, "La cave répond de nouveau : carte fraîche", null);
        }
    }

    void reapprovisionnement(@Observes(during = TransactionPhase.AFTER_COMPLETION) Reapprovisionnement livraison) {
        if (livraison.livree()) {
            noter(TypeDeFait.LIVRAISON, livraison.ingredient(),
                    livraison.livre() + " " + livraison.ingredient() + ", chariot de " + livraison.chariot(), (long) livraison.livre());
        } else {
            noter(TypeDeFait.LIVRAISON_MANQUEE, livraison.ingredient(), livraison.ingredient() + " : le marchand n'est pas venu", null);
        }
    }

    private void disjoncteur(CircuitBreakerState etat) {
        switch (etat) {
            case OPEN -> noter(TypeDeFait.DISJONCTEUR_OUVERT, etat.name(), "La guilde est isolée : plus aucun coursier ne part", null);
            case HALF_OPEN -> noter(TypeDeFait.DISJONCTEUR_MI_OUVERT, etat.name(), "Un coursier d'essai part vers la guilde", null);
            case CLOSED -> noter(TypeDeFait.DISJONCTEUR_FERME, etat.name(), "La guilde répond de nouveau", null);
        }
    }

    /**
     * Consigne en base les faits en attente, ouvre et clôt les incidents. Appelée par la tâche planifiée, et par les
     * tests.
     */
    @Scheduled(every = "${taverne.main-courante.consignation:2s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public synchronized void consigner() {
        List<Fait> lot = new ArrayList<>(aReprendre);
        aReprendre.clear();
        for (Fait fait; (fait = enAttente.poll()) != null;) {
            taille.decrementAndGet();
            lot.add(fait);
        }
        long perdus = oublies.getAndSet(0);
        if (perdus > 0) {
            LOG.warnf("Main courante débordée : %d fait(s) oublié(s)", perdus);
        }
        if (lot.isEmpty() && incidentEnCours == null) {
            return;
        }
        Long incident = incidentEnCours;
        Instant degradation = derniereDegradation;
        IncidentEnCours publie = enCours;
        try {
            Etat etat = QuarkusTransaction.requiringNew().call(() -> consigner(lot, new Etat(incident, degradation, publie)));
            if (etat.clos != null) {
                dernierClos = etat.clos;
            }
            incidentEnCours = etat.incident;
            derniereDegradation = etat.derniereDegradation;
            enCours = etat.publie;
        } catch (RuntimeException e) {
            // la cave ne répond plus : les faits attendront la prochaine consignation
            aReprendre.addAll(lot);
            LOG.warnf("Main courante : consignation reportée (%d fait(s)) : %s", lot.size(), e.getMessage());
        }
    }

    /**
     * L'état de la main courante, modifié dans la transaction et publié seulement si elle aboutit.
     */
    private static final class Etat {
        Long incident;
        Instant derniereDegradation;
        IncidentEnCours publie;
        Long clos;

        Etat(Long incident, Instant derniereDegradation, IncidentEnCours publie) {
            this.incident = incident;
            this.derniereDegradation = derniereDegradation;
            this.publie = publie;
        }
    }

    private Etat consigner(List<Fait> lot, Etat etat) {
        Incident incident = etat.incident != null ? incidents.findById(etat.incident) : null;
        for (Fait fait : lot) {
            if (incident == null) {
                if (!fait.type().degradation()) {
                    continue;
                }
                incident = ouvrir(fait);
                etat.incident = incident.id;
                etat.publie = new IncidentEnCours(incident.id, incident.ouvertLe, incident.declencheur);
            }
            consigner(incident, fait);
            if (fait.type().degradation()) {
                etat.derniereDegradation = fait.horodatage();
            }
        }
        if (incident != null && calmeRevenu(etat.derniereDegradation)) {
            incident.statut = StatutIncident.CLOS;
            incident.closLe = Instant.now();
            consigner(incident, new Fait(incident.closLe, TypeDeFait.CLOTURE, null,
                    "Retour au calme : aucune dégradation depuis " + calme.toSeconds() + " s, disjoncteur fermé", null));
            LOG.infof("Incident %d clos : post-mortem sur /exploitation/incidents/%d/post-mortem", incident.id, incident.id);
            etat.clos = incident.id;
            etat.incident = null;
            etat.derniereDegradation = null;
            etat.publie = null;
        }
        return etat;
    }

    private Incident ouvrir(Fait declencheur) {
        Incident incident = new Incident();
        incident.ouvertLe = declencheur.horodatage();
        incident.statut = StatutIncident.EN_COURS;
        incident.declencheur = declencheur.type().libelle() + " : " + declencheur.detail();
        incidents.persist(incident);
        consigner(incident, new Fait(declencheur.horodatage(), TypeDeFait.OUVERTURE, null, incident.declencheur, null));
        consigner(incident, etatDuGobelin(declencheur.horodatage()));
        LOG.warnf("Incident %d ouvert : %s", incident.id, incident.declencheur);
        return incident;
    }

    private void consigner(Incident incident, Fait fait) {
        FaitMarquant marquant = new FaitMarquant();
        marquant.incident = incident;
        marquant.horodatage = fait.horodatage();
        marquant.type = fait.type();
        marquant.cle = tronquer(fait.cle(), 200);
        marquant.detail = tronquer(fait.detail(), 1000);
        marquant.valeur = fait.valeur();
        faits.persist(marquant);
    }

    private boolean calmeRevenu(Instant derniereDegradation) {
        return derniereDegradation != null
                && Duration.between(derniereDegradation, Instant.now()).compareTo(calme) >= 0
                && disjoncteurs.currentState(MerchantService.DISJONCTEUR) == CircuitBreakerState.CLOSED;
    }

    /**
     * Ce que faisait le gobelin à l'ouverture de l'incident, sous la forme exacte que {@code scripts/gobelin.mjs armer}
     * accepte : de quoi rallumer le même feu, une fois le correctif en place.
     */
    private Fait etatDuGobelin(Instant horodatage) {
        MutableAssaultConfig config = gobelin.getMutableConfig();
        if (!gobelin.isActive() || config == null) {
            return new Fait(horodatage, TypeDeFait.GOBELIN_ETAT, "endormi", "Le gobelin dormait", null);
        }
        MutableAssaultConfig vue = config.snapshot();
        long[] latence = vue.getLatencyRange();
        Map<String, Object> armer = new LinkedHashMap<>();
        armer.put("layers", vue.getLayers().stream().map(Enum::name).sorted().toList());
        armer.put("latencyEnabled", vue.isLatencyEnabled());
        armer.put("latency", Map.of("minMilliseconds", latence[0], "maxMilliseconds", latence[1]));
        armer.put("exceptionEnabled", vue.isExceptionEnabled());
        armer.put("httpStatusEnabled", vue.isHttpStatusEnabled());
        armer.put("httpStatus", Map.of("code", vue.getHttpStatusCode()));
        armer.put("clientLatencyEnabled", vue.isClientLatencyEnabled());
        armer.put("clientExceptionEnabled", vue.isClientExceptionEnabled());
        armer.put("level", vue.getTargetLevel());
        try {
            return new Fait(horodatage, TypeDeFait.GOBELIN_ETAT, "eveille", json.writeValueAsString(armer), null);
        } catch (JsonProcessingException e) {
            return new Fait(horodatage, TypeDeFait.GOBELIN_ETAT, "eveille", armer.toString(), null);
        }
    }

    private static String tronquer(String texte, int longueur) {
        return texte == null || texte.length() <= longueur ? texte : texte.substring(0, longueur);
    }
}
