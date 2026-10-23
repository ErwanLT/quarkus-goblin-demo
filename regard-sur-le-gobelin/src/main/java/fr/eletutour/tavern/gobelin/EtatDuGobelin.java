package fr.eletutour.tavern.gobelin;

import java.util.List;

/**
 * Ce que fait le gobelin éveillé, figé à un instant : ce qu'il injecte, où, et sur quelle part des requêtes.
 *
 * @param niveau part des requêtes attaquées, en pourcentage
 * @param couches les couches armées, triées par nom
 * @param latence latence injectée côté serveur
 * @param latenceMinMs borne basse de la latence, en millisecondes
 * @param latenceMaxMs borne haute de la latence, en millisecondes
 * @param exceptions exceptions levées
 * @param statutHttp statut HTTP d'erreur renvoyé
 * @param codeHttp le statut renvoyé
 * @param dependanceDegradee dépendance dégradée
 * @param corpsAbime corps de réponse tronqué ou gonflé
 * @param enTetesModifies en-têtes de réponse modifiés
 * @param coursiersRalentis appels sortants ralentis
 * @param coursiersPerdus appels sortants en échec
 * @param totalAssauts nombre d'assauts depuis la dernière remise à zéro des compteurs
 */
public record EtatDuGobelin(int niveau, List<String> couches, boolean latence, long latenceMinMs, long latenceMaxMs,
        boolean exceptions, boolean statutHttp, int codeHttp, boolean dependanceDegradee, boolean corpsAbime,
        boolean enTetesModifies, boolean coursiersRalentis, boolean coursiersPerdus, long totalAssauts) {
}
