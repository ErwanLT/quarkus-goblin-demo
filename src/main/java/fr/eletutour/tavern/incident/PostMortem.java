package fr.eletutour.tavern.incident;

import java.time.Instant;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Le post-mortem d'un incident, sans blâme : il ne contient que des faits consignés dans la main courante, et les
 * enseignements qui en découlent mécaniquement. Il ne dit jamais qui a fait quoi, seulement ce qui s'est passé.
 *
 * @param incident identifiant de l'incident
 * @param statut en cours, clos ou interrompu par un redémarrage
 * @param ouvertLe première dégradation
 * @param closLe retour au calme, {@code null} si l'incident n'est pas clos
 * @param dureeSecondes durée de l'incident, jusqu'à maintenant s'il est en cours
 * @param declencheur le premier signe de dégradation
 * @param chronologie les étapes marquantes, dans l'ordre
 * @param impact ce que les clients et la taverne ont subi
 * @param causeProbable ce que le gobelin a injecté pendant l'incident, ou l'absence de panne injectée
 * @param ceQuiAFonctionne les garde-fous qui ont tenu
 * @param ceQuiNAPasFonctionne ce que les clients ont subi sans repli
 * @param actionsCorrectives les gestes à faire, déduits des faits
 * @param rallumerLeFeu la commande qui rejoue la même attaque, pour vérifier un correctif ; {@code null} sans gobelin
 */
@Schema(description = "Post-mortem sans blâme d'un incident, tiré des seuls faits consignés dans la main courante")
public record PostMortem(long incident, StatutIncident statut, Instant ouvertLe, Instant closLe, long dureeSecondes,
        String declencheur, List<Etape> chronologie, Impact impact, List<String> causeProbable,
        List<String> ceQuiAFonctionne, List<String> ceQuiNAPasFonctionne, List<String> actionsCorrectives,
        String rallumerLeFeu) {

    /**
     * Une étape de la chronologie.
     *
     * @param horodatage quand
     * @param etape quoi
     * @param detail ce qui s'est passé
     */
    public record Etape(Instant horodatage, String etape, String detail) {
    }

    /**
     * Ce que l'incident a coûté.
     *
     * @param commandesServies commandes servies pendant l'incident
     * @param commandesRefusees refus métier (bourse, stock...) : la taverne a répondu correctement
     * @param commandesRefoulees refus pour trop de commandes (429)
     * @param commandesAbandonnees commandes coupées par le délai du comptoir (504)
     * @param commandesPerdues commandes perdues en erreur serveur (5xx)
     * @param dureeMaxCommandeMs la commande la plus longue, servie ou non
     * @param cartesServiesDepuisLArdoise cartes servies depuis la dernière carte connue
     * @param livraisonsManquees réapprovisionnements sans livraison
     * @param disjoncteurOuvertSecondes temps passé disjoncteur ouvert
     * @param assautsDuGobelin pannes injectées pendant l'incident
     */
    public record Impact(long commandesServies, long commandesRefusees, long commandesRefoulees, long commandesAbandonnees,
            long commandesPerdues, long dureeMaxCommandeMs, long cartesServiesDepuisLArdoise, long livraisonsManquees,
            long disjoncteurOuvertSecondes, long assautsDuGobelin) {
    }
}
