# AGENTS.md : dresseur de gobelins

Ce fichier s'adresse aux agents IA qui travaillent sur **The Falling Whale**, une taverne écrite en Quarkus. Ta mission :
**éprouver la résilience de la taverne avec Quarkus Goblin**, l'extension de chaos engineering, et rapporter ce qui ne
tient pas ses promesses. Tu mènes des expériences, tu observes, tu conclus. Tu ne lances pas de chaos au hasard.

## Règles absolues

1. **Jamais de `git commit`, `git push` ni de tag.** Tu peux modifier le code pour proposer un correctif, mais tu laisses
   les changements non commités, et tu le dis dans ton rapport.
2. **Dev mode uniquement.** Goblin n'agit qu'en dev mode (`./mvnw quarkus:dev`) : un build de production n'en contient
   aucune trace. N'essaie pas de l'activer ailleurs.
3. **Un filet avant chaque expérience** : `node scripts/gobelin.mjs auto-off 5`. Si tu perds la main, le gobelin
   s'endort tout seul au bout de 5 minutes, même sans toi.
4. **Une variable à la fois** : une couche, un type de panne, un niveau. Sinon tu ne sauras pas ce qui a causé quoi.
5. **Toujours revenir à l'état initial.** Au début : `node scripts/gobelin.mjs sauver /tmp/gobelin-initial.json`. À la
   fin, et en cas de doute : `node scripts/gobelin.mjs tout-couper`, puis
   `node scripts/gobelin.mjs restaurer /tmp/gobelin-initial.json`. La restauration laisse le gobelin endormi : fais
   `actif on` si `etat` indiquait `"active": true` au début.
6. **Ne touche pas aux données ni à l'infrastructure** : pas de `docker compose down -v`, pas de suppression de volume,
   pas de modification de `docker-compose.yml` ni de `observability/`.
7. **Ne modifie pas les tests existants pour les faire passer.** Un test qui échoue est une information.

## Démarrer

Vérifie d'abord si la taverne tourne déjà : `curl -s localhost:8080/q/health`. Sinon, dans cet ordre :

```bash
docker compose up -d          # PostgreSQL et la stack d'observabilité
./mvnw quarkus:dev            # l'application, sur http://localhost:8080 (à lancer en arrière-plan)
```

Le trafic d'une soirée ordinaire : `./scripts/trafic.sh 120 5` (120 s, ~5 requêtes par seconde). Les logs de
l'application sont copiés dans `target/logs/the-falling-whale.log`.

## Piloter le gobelin

Le gobelin se pilote par le JSON-RPC de la Dev UI. `scripts/gobelin.mjs` l'enveloppe (détails en tête du script) :

| Commande | Effet |
|---|---|
| `etat` | chaos actif ou non, couches armées, auto-off restant |
| `config` | configuration complète |
| `armer '<json>'` | applique une configuration partielle (les champs absents gardent leur valeur) |
| `actif on` / `actif off` | réveille ou endort le gobelin sans toucher à sa configuration |
| `auto-off <minutes>` | endort le gobelin après ce délai |
| `historique [n]` | les n derniers assauts : cible, type, source, latence réellement subie |
| `compteurs` / `oublier` | assauts par type et par source ; vider l'historique et les compteurs |
| `tout-couper` | coupe tout : chaos inactif, assauts désarmés, auto-off annulé |
| `sauver <f>` / `restaurer <f>` | exporter / réimporter la configuration |
| `sonde <METHODE> <chemin> [n] [json]` | n requêtes, puis la répartition des statuts, les latences p50/p95 et un exemple de réponse par statut |

Les champs de `armer` :

```json
{
  "layers": ["SERVICE"],
  "latencyEnabled": true, "latency": { "minMilliseconds": 1800, "maxMilliseconds": 2000 },
  "exceptionEnabled": false,
  "clientLatencyEnabled": false, "clientExceptionEnabled": false,
  "httpStatusEnabled": false, "httpStatus": { "code": 503, "message": "Service Unavailable" },
  "level": 100
}
```

`level` est le pourcentage de requêtes attaquées. Il n'y a **qu'un seul niveau et qu'une seule plage de latence**,
partagés par toutes les couches. `armer` ne réveille pas le gobelin : vérifie `etat`, et fais `actif on` si besoin.

### Les couches : où frappe le gobelin

Une requête entrante ne subit qu'**une seule couche** : parmi les couches armées dont le tirage passe, la plus profonde
gagne (`DATABASE`, puis `MESSAGING`, puis `SERVICE`, puis `HTTP_IN`). `HTTP_OUT` est tiré à part, à chaque appel sortant.

| Couche | Où la panne arrive | Ce que la résilience voit |
|---|---|---|
| `HTTP_IN` | à l'entrée de la ressource REST, avant le code métier | rien : `@Retry`, `@Timeout` et les autres ne la voient jamais |
| `SERVICE` | au premier appel d'un bean de service, **à l'intérieur** de Fault Tolerance | `@Timeout`, `@Retry`, `@CircuitBreaker`, `@Fallback` réagissent pour de vrai |
| `DATABASE` | quand le code demande une connexion JDBC au pool | la résilience du service qui appelle le repository |
| `HTTP_OUT` | sur les appels du REST Client, vers la guilde des marchands (`clientLatencyEnabled`, `clientExceptionEnabled`) | la résilience du service qui fait l'appel |

Seul l'appel de bean le plus extérieur d'une requête est attaqué ; chaque nouvelle tentative d'un `@Retry` retire au
sort. Les packages `exception`, `observability`, `bootstrap` et `salle` sont exclus : rien de ce qu'ils déclenchent
n'est attaqué.

## Observer

Par ordre de fiabilité :

1. **Les réponses HTTP** que tu reçois (`sonde`). Les erreurs suivent la RFC 9457 (`application/problem+json`), avec
   `type`, `title`, `status`, `detail`, un `traceId`, et `faultTolerance` quand une annotation de résilience a tranché.
2. **L'historique du gobelin** : ce qu'il a réellement injecté, où, et combien de temps.
3. **Les logs** : `target/logs/the-falling-whale.log`, ou Loki dans Grafana (http://localhost:3000). Chaque ligne porte
   un `traceId`.
4. **Les métriques** : http://localhost:8080/q/metrics, échantillonnées par Prometheus toutes les 5 s environ, donc en
   décalé.
5. **La main courante** : `GET /exploitation/incidents/en-cours` dit si la taverne se voit en incident, et
   `GET /exploitation/incidents/{id}/post-mortem` en fait le bilan chiffré (commandes perdues, replis servis, assauts
   du gobelin). Un incident ne se clôt qu'après 30 s de calme : attends sa clôture avant de lire le bilan. Un
   rechargement du code pendant un incident le marque `INTERROMPU`.

La salle (http://localhost:8080/salle) montre au public ce que vivent les clients. Tu n'en as pas besoin pour conclure.

## Les promesses de la taverne

Voici ce que la taverne promet sous la panne. Chaque promesse est une hypothèse à vérifier, pas une vérité : mesure.

| # | Situation | Promesse |
|---|---|---|
| P1 | La base de données ne répond plus | La carte (`GET /grimoire/recettes`) reste servie en `200`, depuis l'ardoise, la dernière carte connue. |
| P2 | La cuisine est trop lente (> 1,5 s) | `POST /commandes` répond `504` en 1,5 s environ, et **l'aventurier n'est pas débité**. |
| P3 | La base de données ne répond plus | Une commande échoue proprement (`5xx` en `problem+json`), et rien n'est débité. |
| P4 | La guilde des marchands ne répond plus | `POST /cave/stocks/{id}/reapprovisionnement` répond `200` avec `status: MARCHAND_ABSENT` et `delivered: 0`. |
| P5 | La guilde reste injoignable | Après quelques échecs, le disjoncteur s'ouvre : le repli devient **immédiat, en moins de 300 ms**, sans nouvelles tentatives, pendant 10 s. |
| P6 | Trop de commandes d'un coup | Au-delà de 60 commandes en 10 s, le comptoir refuse proprement (`429`), sans erreur serveur. |
| P7 | Toute panne | Aucune réponse ne fuit une trace de pile ou un message technique interne ; aucune erreur `4xx` ne devient `5xx`. |
| P8 | Après la panne | Une fois le gobelin endormi, la taverne revient à la normale sans redémarrage. |

Quelques requêtes utiles :

```bash
curl -s localhost:8080/grimoire/recettes
curl -s -X POST localhost:8080/commandes -H 'Content-Type: application/json' -d '{"adventurerId":2,"recipeId":1,"quantity":1}'
curl -s localhost:8080/aventuriers/2                     # sa bourse, avant et après
curl -s -X POST localhost:8080/cave/stocks/1/reapprovisionnement -H 'Content-Type: application/json' -d '{"quantity":5}'
```

Tu peux aussi lire le code (`src/main/java`) pour concevoir tes attaques : les annotations `@Timeout`, `@Retry`,
`@CircuitBreaker`, `@Fallback` et `@RateLimit` disent où la taverne croit être protégée.

## La boucle

Pour chaque promesse, ou chaque faiblesse que tu soupçonnes en lisant le code :

1. **Hypothèse** : « si j'arme telle couche avec telle panne, alors telle promesse tient ».
2. **Préparation** : `auto-off 5`, `oublier`, et une mesure de référence sans chaos.
3. **Assaut** : `armer`, `actif on`, puis `sonde` ou `trafic.sh`.
4. **Observation** : réponses, historique, logs. Compare à la référence.
5. **Retour au calme** : `tout-couper`, et vérifie que la taverne va bien (P8).
6. **Conclusion** : la promesse tient, ou pas. Si elle ne tient pas, cherche la cause dans le code. Propose un correctif,
   applique-le si c'est simple, relance `./mvnw test`, puis rejoue la même expérience pour prouver le correctif.

## Le rapport

Termine par un rapport court :

- pour chaque expérience : l'hypothèse, la configuration exacte du gobelin, ce que tu as mesuré (statuts, latences,
  extrait d'historique), et le verdict ;
- pour chaque promesse non tenue : la cause, le correctif proposé ou appliqué (fichier et ligne), et la preuve qu'il
  marche ;
- ce qui t'a manqué ou t'a trompé dans ce fichier ou dans les outils : c'est ce qui permettra d'améliorer Goblin ;
- l'état final : gobelin coupé et configuration restaurée, changements laissés non commités.
