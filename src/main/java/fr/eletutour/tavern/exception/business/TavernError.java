package fr.eletutour.tavern.exception.business;

/**
 * Le catalogue des pièges de la taverne.
 */
public enum TavernError {

    INGREDIENT_INTROUVABLE(404, "Ressource introuvable", "L'ingrédient %s n'existe pas dans la réserve."),
    RECETTE_INTROUVABLE(404, "Ressource introuvable", "La recette %s n'existe pas dans le grimoire."),
    AVENTURIER_INTROUVABLE(404, "Ressource introuvable", "L'aventurier %s n'est pas inscrit au registre."),
    COMMANDE_INTROUVABLE(404, "Ressource introuvable", "La commande %s n'existe pas dans le livre des commandes."),
    STOCK_INTROUVABLE(404, "Ressource introuvable", "Aucune étagère de la cave ne contient l'ingrédient %s."),
    INGREDIENT_EXISTANT(409, "Conflit de données", "L'ingrédient '%s' est déjà dans la réserve."),
    AVENTURIER_EXISTANT(409, "Conflit de données", "L'aventurier '%s' est déjà inscrit au registre."),
    RUPTURE_DE_STOCK(409, "Rupture de stock", "La cave ne contient pas de quoi préparer %d portion(s) de '%s'."),
    BOURSE_INSUFFISANTE(422, "Bourse insuffisante", "%s n'a pas assez de pièces d'or pour régler la commande."),
    RECETTE_SANS_INGREDIENT(400, "Recette invalide", "Une recette doit utiliser au moins un ingrédient connu de la réserve.");

    private final int status;
    private final String title;
    private final String detailTemplate;

    TavernError(int status, String title, String detailTemplate) {
        this.status = status;
        this.title = title;
        this.detailTemplate = detailTemplate;
    }

    public int getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    /**
     * @return le code stable exposé aux clients, par exemple {@code recette-introuvable}
     */
    public String getCode() {
        return name().toLowerCase().replace('_', '-');
    }

    public String formatDetail(Object... args) {
        return String.format(detailTemplate, args);
    }
}
