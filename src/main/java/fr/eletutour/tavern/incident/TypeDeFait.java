package fr.eletutour.tavern.incident;

/**
 * Ce que la main courante sait consigner. Seuls les signes de dégradation vus par un client ou par la résilience ouvrent
 * un incident : un gobelin qui rôde ou une commande refusée par la bourse d'un aventurier font partie d'une soirée
 * ordinaire.
 */
public enum TypeDeFait {

    OUVERTURE("Ouverture de l'incident", false),
    CLOTURE("Clôture de l'incident", false),
    GOBELIN_ETAT("État du gobelin à l'ouverture", false),
    GOBELIN_REVEILLE("Le gobelin se réveille", false),
    GOBELIN_ENDORMI("Le gobelin s'endort", false),
    ASSAUT("Assaut du gobelin", false),
    COMMANDE_SERVIE("Commande servie", false),
    COMMANDE_REFUSEE("Commande refusée", false),
    COMMANDE_REFOULEE("Commande refoulée (trop de commandes)", false),
    COMMANDE_ABANDONNEE("Commande abandonnée (délai du comptoir)", true),
    COMMANDE_PERDUE("Commande perdue (erreur serveur)", true),
    CARTE_ARDOISE("Carte servie depuis l'ardoise", true),
    CARTE_RETABLIE("Carte de nouveau fraîche", false),
    LIVRAISON("Livraison de la guilde", false),
    LIVRAISON_MANQUEE("Livraison manquée", true),
    DISJONCTEUR_OUVERT("Disjoncteur ouvert", true),
    DISJONCTEUR_MI_OUVERT("Disjoncteur à moitié ouvert", false),
    DISJONCTEUR_FERME("Disjoncteur refermé", false);

    private final String libelle;
    private final boolean degradation;

    TypeDeFait(String libelle, boolean degradation) {
        this.libelle = libelle;
        this.degradation = degradation;
    }

    /**
     * @return le libellé du fait, tel qu'il apparaît dans le post-mortem
     */
    public String libelle() {
        return libelle;
    }

    /**
     * @return {@code true} quand le fait est un signe de dégradation : il ouvre un incident, et repousse sa clôture
     */
    public boolean degradation() {
        return degradation;
    }
}
