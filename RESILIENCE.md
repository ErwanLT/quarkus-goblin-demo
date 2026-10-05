# Les promesses de la taverne

Ce que The Falling Whale garantit quand le gobelin frappe. Le gobelin sait où sont les défenses : son inventaire liste
chaque `@Timeout`, `@Retry`, `@CircuitBreaker`, `@Fallback` et `@RateLimit`. Il ne sait pas ce qu'elles promettent.
C'est l'objet de ce fichier. Chaque promesse est une hypothèse à mesurer, pas une vérité.

## Une taverne en bonne santé

- `POST /commandes` : une commande servie en moins de 1,5 s, au plus 60 commandes par fenêtre de 10 s.
- `GET /grimoire/recettes` : la carte répond toujours `200`, même quand la cave ne répond plus.
- `POST /cave/stocks/{id}/reapprovisionnement` : répond toujours `200`, livré ou non.
- Les erreurs sont en `application/problem+json`, sans trace de pile ni message technique interne.
- Les health checks `database` et `tavern-readiness` (carte intègre) restent `UP` tant que la cave répond.
- Budget d'erreur : pas encore défini.

## Les dépendances et ce qui les protège

| Dépendance | Appelée depuis | Protection | Ce que voit le client quand elle tombe |
|---|---|---|---|
| La cave (PostgreSQL) | `GrimoireService.consulterLeGrimoire` | `@Retry(maxRetries = 2, delay = 100)` + `@Fallback(lireLArdoise)` | la carte en `200`, depuis l'ardoise, la dernière carte connue |
| La cave (PostgreSQL) | `OrderService.passerCommande` | `@Timeout(1500)` + `@RateLimit(60 / 10 s)`, sans repli | un `5xx` en `problem+json`, transaction annulée, aventurier non débité |
| La guilde des marchands (REST Client) | `MerchantService.commander` | `@Timeout(2000)` + `@Retry(maxRetries = 2, delay = 200, abortOn = CircuitBreakerOpenException)` + `@CircuitBreaker(6, 0.5, 10 s)` + `@Fallback(marchandAbsent)` | `200` avec `status: MARCHAND_ABSENT` et `delivered: 0` |
| La cuisine (temps de préparation) | `OrderService.passerCommande` | `@Timeout(1500)` | `504` « Service trop lent » en 1,5 s environ, aventurier non débité |

## Les promesses à vérifier

| # | Quand | Alors |
|---|---|---|
| P1 | la cave ne répond plus | `GET /grimoire/recettes` répond `200` depuis l'ardoise |
| P2 | la cuisine dépasse 1,5 s | `POST /commandes` répond `504` en 1,5 s environ, et rien n'est débité |
| P3 | la cave ne répond plus | une commande échoue en `5xx` `problem+json`, et rien n'est débité |
| P4 | la guilde ne répond plus | le réapprovisionnement répond `200`, `MARCHAND_ABSENT`, `delivered: 0` |
| P5 | la guilde reste injoignable | le disjoncteur s'ouvre, et le repli répond en moins de 300 ms, sans nouvelle tentative, pendant 10 s |
| P6 | plus de 60 commandes en 10 s | le comptoir répond `429` avec `Retry-After: 10`, sans erreur serveur |
| P7 | toute panne | aucune réponse ne fuit une trace de pile ; aucune erreur `4xx` ne devient `5xx` |
| P8 | après la panne | une fois le gobelin endormi, la taverne revient à la normale sans redémarrage |

## Les points ouverts

Relevés lors d'une épreuve menée par un agent IA. Ce ne sont pas des promesses : ce sont des questions à trancher.

- `CellarService.reapprovisionner` n'a aucune protection : une panne à cet endroit donne un `500` au client, avant même
  d'atteindre la guilde.
- `GET /grimoire/recettes/{id}` n'a pas d'ardoise : quand la cave tombe, il répond un `500` générique.
- Le pire cas d'un réapprovisionnement quand la guilde traîne est de 6,5 s : trois tentatives de 2 s, sans budget de
  temps global. Acceptable ou pas ?
- Les commandes n'ont pas de repli quand la cave tombe : elles sont perdues en `5xx`.

## Hors limites

- La salle (`/salle`), la main courante (`/exploitation`), l'observabilité et le bootstrap : leurs packages sont exclus
  du ciblage du gobelin, ne les visez pas.
- La couche `MESSAGING` : la taverne n'a pas de file de messages.
- Les données et l'infrastructure : pas de `docker compose down -v`, pas de suppression de volume, pas de modification
  de `docker-compose.yml` ni de `observability/`.
- La main courante est conservée d'un redémarrage à l'autre : ne purgez pas les incidents ouverts par une expérience.
- Les outils Dev MCP qui arment le chaos sont activés par le développeur, et désactivés après l'expérience.
