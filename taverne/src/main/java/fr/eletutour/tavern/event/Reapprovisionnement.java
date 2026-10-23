package fr.eletutour.tavern.event;

/**
 * Une étagère de la cave vient d'être réapprovisionnée auprès de la guilde des marchands, ou pas.
 *
 * @param ingredient ingrédient commandé
 * @param livre quantité effectivement livrée
 * @param chariot nom du convoyeur, {@code null} quand le marchand n'est pas venu
 * @param stock quantité sur l'étagère après la livraison
 */
public record Reapprovisionnement(String ingredient, int livre, String chariot, int stock) {

    public boolean livree() {
        return chariot != null;
    }
}
