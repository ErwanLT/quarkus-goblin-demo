package fr.eletutour.tavern.event;

/**
 * Une commande vient de quitter le comptoir, servie ou non, telle que le client l'a vécue : la réponse HTTP la raconte,
 * y compris quand un {@code @Timeout} l'a coupée avant qu'elle ne sorte du service.
 *
 * @param status statut HTTP de la réponse
 * @param reponse corps de la réponse : la commande servie, un {@code Problem}, ou autre chose si le gobelin l'a abîmé
 * @param dureeMs temps passé au comptoir
 */
public record CommandeAuComptoir(int status, Object reponse, long dureeMs) {
}
