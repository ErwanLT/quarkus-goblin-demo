package fr.eletutour.tavern.incident;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import fr.eletutour.tavern.exception.business.TavernError;
import fr.eletutour.tavern.exception.business.TavernException;

/**
 * Tire le post-mortem d'un incident de la main courante. Chaque phrase découle d'un fait compté : aucune n'est écrite
 * à la main pour un incident donné.
 */
@ApplicationScoped
public class PostMortemService {

    /** Les faits qui marquent la chronologie à chaque occurrence ; les autres n'y apparaissent qu'à leur première. */
    private static final Set<TypeDeFait> TOUJOURS_DANS_LA_CHRONOLOGIE = EnumSet.of(TypeDeFait.OUVERTURE,
            TypeDeFait.CLOTURE, TypeDeFait.GOBELIN_REVEILLE, TypeDeFait.GOBELIN_ENDORMI, TypeDeFait.DISJONCTEUR_OUVERT,
            TypeDeFait.DISJONCTEUR_MI_OUVERT, TypeDeFait.DISJONCTEUR_FERME);

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Inject
    IncidentRepository incidents;

    @Inject
    FaitMarquantRepository faits;

    /**
     * @param limite nombre maximal d'incidents
     * @return les derniers incidents, du plus récent au plus ancien
     */
    @Transactional
    public List<Incident> derniers(int limite) {
        return incidents.derniers(limite);
    }

    /**
     * @param id identifiant de l'incident
     * @return son post-mortem
     * @throws TavernException quand l'incident n'existe pas
     */
    @Transactional
    public PostMortem rediger(long id) {
        Incident incident = incidents.findByIdOptional(id)
                .orElseThrow(() -> TavernException.of(TavernError.INCIDENT_INTROUVABLE, id));
        List<FaitMarquant> journal = faits.deLIncident(incident);

        Map<TypeDeFait, Long> comptes = new EnumMap<>(TypeDeFait.class);
        journal.forEach(fait -> comptes.merge(fait.type, 1L, Long::sum));
        long dureeMax = journal.stream()
                .filter(fait -> fait.type.name().startsWith("COMMANDE_") && fait.valeur != null)
                .mapToLong(fait -> fait.valeur).max().orElse(0);
        PostMortem.Impact impact = new PostMortem.Impact(
                compte(comptes, TypeDeFait.COMMANDE_SERVIE), compte(comptes, TypeDeFait.COMMANDE_REFUSEE),
                compte(comptes, TypeDeFait.COMMANDE_REFOULEE), compte(comptes, TypeDeFait.COMMANDE_ABANDONNEE),
                compte(comptes, TypeDeFait.COMMANDE_PERDUE), dureeMax, compte(comptes, TypeDeFait.CARTE_ARDOISE),
                compte(comptes, TypeDeFait.LIVRAISON_MANQUEE), disjoncteurOuvert(journal, incident),
                compte(comptes, TypeDeFait.ASSAUT));

        Map<String, Long> assauts = journal.stream().filter(fait -> fait.type == TypeDeFait.ASSAUT)
                .collect(Collectors.groupingBy(fait -> fait.cle, LinkedHashMap::new, Collectors.counting()));
        String gobelin = journal.stream().filter(fait -> fait.type == TypeDeFait.GOBELIN_ETAT && "eveille".equals(fait.cle))
                .map(fait -> fait.detail).findFirst().orElse(null);

        Instant fin = incident.closLe != null ? incident.closLe : Instant.now();
        return new PostMortem(incident.id, incident.statut, incident.ouvertLe, incident.closLe,
                Duration.between(incident.ouvertLe, fin).toSeconds(), incident.declencheur, chronologie(journal),
                impact, causeProbable(assauts, journal), ceQuiAFonctionne(impact, incident),
                ceQuiNAPasFonctionne(impact, incident), actionsCorrectives(impact, assauts, gobelin),
                gobelin != null ? "node scripts/gobelin.mjs armer '" + gobelin + "' && node scripts/gobelin.mjs actif on" : null);
    }

    /**
     * @param postMortem le post-mortem
     * @return le même post-mortem en Markdown, prêt à coller dans le compte rendu de l'équipe
     */
    public String enMarkdown(PostMortem postMortem) {
        StringBuilder md = new StringBuilder();
        md.append("# Post-mortem de l'incident ").append(postMortem.incident()).append("\n\n");
        md.append("- **Statut** : ").append(postMortem.statut()).append('\n');
        md.append("- **Début** : ").append(DATE_HEURE.format(postMortem.ouvertLe())).append('\n');
        md.append("- **Fin** : ").append(postMortem.closLe() != null ? DATE_HEURE.format(postMortem.closLe()) : "en cours").append('\n');
        md.append("- **Durée** : ").append(postMortem.dureeSecondes()).append(" s\n");
        md.append("- **Déclencheur** : ").append(postMortem.declencheur()).append("\n\n");
        md.append("## Chronologie\n\n| Heure | Étape | Détail |\n|---|---|---|\n");
        postMortem.chronologie().forEach(etape -> md.append("| ").append(HEURE.format(etape.horodatage())).append(" | ")
                .append(etape.etape()).append(" | ").append(etape.detail().replace("|", "\\|")).append(" |\n"));
        PostMortem.Impact impact = postMortem.impact();
        md.append("\n## Impact\n\n| Mesure | Valeur |\n|---|---|\n")
                .append("| Commandes servies | ").append(impact.commandesServies()).append(" |\n")
                .append("| Commandes refusées (métier) | ").append(impact.commandesRefusees()).append(" |\n")
                .append("| Commandes refoulées (429) | ").append(impact.commandesRefoulees()).append(" |\n")
                .append("| Commandes abandonnées (504) | ").append(impact.commandesAbandonnees()).append(" |\n")
                .append("| Commandes perdues (5xx) | ").append(impact.commandesPerdues()).append(" |\n")
                .append("| Commande la plus longue | ").append(impact.dureeMaxCommandeMs()).append(" ms |\n")
                .append("| Cartes servies depuis l'ardoise | ").append(impact.cartesServiesDepuisLArdoise()).append(" |\n")
                .append("| Livraisons manquées | ").append(impact.livraisonsManquees()).append(" |\n")
                .append("| Disjoncteur ouvert | ").append(impact.disjoncteurOuvertSecondes()).append(" s |\n")
                .append("| Assauts du gobelin | ").append(impact.assautsDuGobelin()).append(" |\n");
        section(md, "Cause probable", postMortem.causeProbable());
        section(md, "Ce qui a fonctionné", postMortem.ceQuiAFonctionne());
        section(md, "Ce qui n'a pas fonctionné", postMortem.ceQuiNAPasFonctionne());
        section(md, "Actions correctives", postMortem.actionsCorrectives());
        if (postMortem.rallumerLeFeu() != null) {
            md.append("\n## Rallumer le feu\n\nPour vérifier un correctif, rejouer la même attaque :\n\n```bash\n")
                    .append(postMortem.rallumerLeFeu()).append("\n```\n");
        }
        return md.toString();
    }

    private static void section(StringBuilder md, String titre, List<String> lignes) {
        md.append("\n## ").append(titre).append("\n\n");
        if (lignes.isEmpty()) {
            md.append("Rien à signaler.\n");
        }
        lignes.forEach(ligne -> md.append("- ").append(ligne).append('\n'));
    }

    private static long compte(Map<TypeDeFait, Long> comptes, TypeDeFait type) {
        return comptes.getOrDefault(type, 0L);
    }

    private static List<PostMortem.Etape> chronologie(List<FaitMarquant> journal) {
        Map<TypeDeFait, Long> occurrences = new EnumMap<>(TypeDeFait.class);
        journal.forEach(fait -> occurrences.merge(fait.type, 1L, Long::sum));
        Set<TypeDeFait> dejaVus = EnumSet.noneOf(TypeDeFait.class);
        List<PostMortem.Etape> etapes = new ArrayList<>();
        for (FaitMarquant fait : journal) {
            boolean premier = dejaVus.add(fait.type);
            if (TOUJOURS_DANS_LA_CHRONOLOGIE.contains(fait.type)) {
                etapes.add(new PostMortem.Etape(fait.horodatage, fait.type.libelle(), fait.detail));
            } else if (premier && fait.type != TypeDeFait.COMMANDE_SERVIE && fait.type != TypeDeFait.LIVRAISON) {
                long autres = occurrences.get(fait.type) - 1;
                String detail = fait.type == TypeDeFait.GOBELIN_ETAT && "eveille".equals(fait.cle)
                        ? "Configuration du gobelin : " + fait.detail
                        : fait.detail;
                etapes.add(new PostMortem.Etape(fait.horodatage, "Premier fait : " + fait.type.libelle(),
                        autres > 0 ? detail + " (puis " + autres + " autre(s))" : detail));
            }
        }
        return etapes;
    }

    /**
     * Additionne les périodes disjoncteur ouvert : de chaque ouverture au passage à moitié ouvert ou fermé qui suit, ou
     * jusqu'à la fin de l'incident.
     */
    private static long disjoncteurOuvert(List<FaitMarquant> journal, Incident incident) {
        long secondes = 0;
        Instant ouverture = null;
        for (FaitMarquant fait : journal) {
            if (fait.type == TypeDeFait.DISJONCTEUR_OUVERT && ouverture == null) {
                ouverture = fait.horodatage;
            } else if ((fait.type == TypeDeFait.DISJONCTEUR_MI_OUVERT || fait.type == TypeDeFait.DISJONCTEUR_FERME)
                    && ouverture != null) {
                secondes += Duration.between(ouverture, fait.horodatage).toSeconds();
                ouverture = null;
            }
        }
        if (ouverture != null) {
            secondes += Duration.between(ouverture, incident.closLe != null ? incident.closLe : Instant.now()).toSeconds();
        }
        return secondes;
    }

    private static List<String> causeProbable(Map<String, Long> assauts, List<FaitMarquant> journal) {
        if (assauts.isEmpty()) {
            return List.of("Aucune panne injectée par le gobelin pendant l'incident : la cause est à chercher hors du "
                    + "chaos, dans les logs et les traces (le traceId de chaque réponse en erreur)");
        }
        List<String> causes = new ArrayList<>();
        assauts.entrySet().stream().sorted(Map.Entry.<String, Long> comparingByValue().reversed()).forEach(entree -> {
            String[] cle = entree.getKey().split("/", 2);
            String exemple = journal.stream().filter(fait -> fait.type == TypeDeFait.ASSAUT && entree.getKey().equals(fait.cle))
                    .map(fait -> fait.detail).findFirst().orElse("");
            causes.add("Le gobelin a injecté " + entree.getValue() + " " + cle[1] + " (source " + cle[0] + "), par exemple : "
                    + exemple);
        });
        return causes;
    }

    private static List<String> ceQuiAFonctionne(PostMortem.Impact impact, Incident incident) {
        List<String> lignes = new ArrayList<>();
        if (impact.cartesServiesDepuisLArdoise() > 0) {
            lignes.add("La carte est restée servie : " + impact.cartesServiesDepuisLArdoise()
                    + " fois depuis l'ardoise, la dernière carte connue (@Retry puis @Fallback du grimoire)");
        }
        if (impact.commandesAbandonnees() > 0) {
            lignes.add("Le délai du comptoir (@Timeout) a coupé " + impact.commandesAbandonnees()
                    + " commande(s) au lieu de laisser les clients attendre, la plus longue en " + impact.dureeMaxCommandeMs()
                    + " ms");
        }
        if (impact.disjoncteurOuvertSecondes() > 0 || impact.livraisonsManquees() > 0) {
            lignes.add("Chaque réapprovisionnement sans livraison a reçu une réponse (repli MARCHAND_ABSENT) : "
                    + impact.livraisonsManquees() + " au total"
                    + (impact.disjoncteurOuvertSecondes() > 0
                            ? ", et le disjoncteur a isolé la guilde pendant " + impact.disjoncteurOuvertSecondes() + " s"
                            : ""));
        }
        if (impact.commandesRefoulees() > 0) {
            lignes.add("Le comptoir a refoulé " + impact.commandesRefoulees()
                    + " commande(s) en 429 (@RateLimit) plutôt que de se laisser submerger");
        }
        if (impact.commandesServies() > 0) {
            lignes.add(impact.commandesServies() + " commande(s) servie(s) pendant l'incident");
        }
        if (incident.statut == StatutIncident.CLOS) {
            lignes.add("La taverne est revenue au calme d'elle-même, sans redémarrage");
        }
        return lignes;
    }

    private static List<String> ceQuiNAPasFonctionne(PostMortem.Impact impact, Incident incident) {
        List<String> lignes = new ArrayList<>();
        if (impact.commandesPerdues() > 0) {
            lignes.add(impact.commandesPerdues() + " commande(s) perdue(s) en erreur serveur : ces clients n'ont reçu "
                    + "aucun repli");
        }
        if (impact.livraisonsManquees() > 0) {
            lignes.add(impact.livraisonsManquees() + " réapprovisionnement(s) sans livraison : les étagères se vident, "
                    + "et rien ne relance ces livraisons ensuite");
        }
        if (incident.statut == StatutIncident.INTERROMPU) {
            lignes.add("La taverne a redémarré avant la fin de l'incident : le retour au calme n'a pas été observé");
        }
        return lignes;
    }

    private static List<String> actionsCorrectives(PostMortem.Impact impact, Map<String, Long> assauts, String gobelin) {
        List<String> actions = new ArrayList<>();
        boolean caveAttaquee = assauts.keySet().stream().anyMatch(cle -> cle.startsWith("database/"));
        if (impact.commandesPerdues() > 0 && caveAttaquee) {
            actions.add("Les commandes n'ont aucun repli quand la cave tombe : répondre 503 avec un Retry-After, ou "
                    + "mettre les commandes en attente plutôt que de les perdre");
        } else if (impact.commandesPerdues() > 0) {
            actions.add("Chercher la cause des erreurs serveur dans les logs et les traces, à partir du traceId des "
                    + "réponses en erreur");
        }
        if (impact.commandesAbandonnees() > 0) {
            actions.add("Chaque commande abandonnée est une vente perdue : alerter sur la latence des commandes "
                    + "(tavern_order_seconds) avant qu'elle n'atteigne le délai de 1,5 s");
        }
        if (impact.livraisonsManquees() > 0) {
            actions.add("Relancer les réapprovisionnements manqués une fois la guilde revenue : aucune relance "
                    + "automatique n'existe aujourd'hui");
        }
        if (impact.cartesServiesDepuisLArdoise() > 0) {
            actions.add("L'ardoise ne contient que la dernière carte lue avec succès : une recette ajoutée pendant la "
                    + "panne n'y apparaît pas, le signaler aux clients");
        }
        if (gobelin != null) {
            actions.add("Une fois un correctif en place, rallumer le même feu (commande ci-dessous) pour prouver qu'il tient");
        }
        return actions;
    }
}
