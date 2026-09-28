#!/usr/bin/env bash
# Une soirée à la taverne : génère un trafic réaliste pour animer les dashboards Grafana.
#
#   ./scripts/trafic.sh            # 5 minutes, 5 requêtes par seconde environ
#   ./scripts/trafic.sh 600 10     # 10 minutes, 10 requêtes par seconde environ
#
# Requiert curl. L'application doit tourner sur http://localhost:8080 (./mvnw quarkus:dev).
set -u

DURATION="${1:-300}"
RATE="${2:-5}"
BASE_URL="${TAVERN_URL:-http://localhost:8080}"
PAUSE=$(awk "BEGIN { printf \"%.3f\", 1 / $RATE }")
END=$(( $(date +%s) + DURATION ))

call() {
  curl -s -o /dev/null -w "%{http_code}" -H "Content-Type: application/json" "$@"
}

commander() {
  local adventurer=$(( RANDOM % 6 + 1 )) recipe=$(( RANDOM % 5 + 1 )) quantity=$(( RANDOM % 3 + 1 ))
  call -X POST "$BASE_URL/commandes" -d "{\"adventurerId\":$adventurer,\"recipeId\":$recipe,\"quantity\":$quantity}"
}

reapprovisionner() {
  local ingredient=$(( RANDOM % 8 + 1 ))
  call -X POST "$BASE_URL/cave/stocks/$ingredient/reapprovisionnement" -d "{\"quantity\":$(( RANDOM % 20 + 10 ))}"
}

echo "La taverne ouvre ses portes pour ${DURATION}s (~${RATE} req/s) sur $BASE_URL - Ctrl+C pour fermer"
count=0; ok=0; client=0; server=0
while [ "$(date +%s)" -lt "$END" ]; do
  roll=$(( RANDOM % 100 ))
  if   [ $roll -lt 35 ]; then code=$(commander)
  elif [ $roll -lt 55 ]; then code=$(call "$BASE_URL/grimoire/recettes")
  elif [ $roll -lt 65 ]; then code=$(call "$BASE_URL/grimoire/recettes/$(( RANDOM % 5 + 1 ))")
  elif [ $roll -lt 72 ]; then code=$(call "$BASE_URL/grimoire/recettes/recherche?titre=hydromel")
  elif [ $roll -lt 80 ]; then code=$(call "$BASE_URL/cave/stocks")
  elif [ $roll -lt 88 ]; then code=$(reapprovisionner)
  elif [ $roll -lt 94 ]; then code=$(call "$BASE_URL/aventuriers/$(( RANDOM % 6 + 1 ))")
  elif [ $roll -lt 97 ]; then code=$(call "$BASE_URL/commandes?limite=10")
  else                        code=$(call "$BASE_URL/grimoire/recettes/404")   # un aventurier perdu
  fi
  case "$code" in
    2*) ok=$(( ok + 1 )) ;;
    4*) client=$(( client + 1 )) ;;
    *)  server=$(( server + 1 )) ;;
  esac
  count=$(( count + 1 ))
  if [ $(( count % 50 )) -eq 0 ]; then
    echo "$(date +%H:%M:%S) - $count requêtes : $ok OK, $client refusées (4xx), $server en erreur (5xx ou injoignable)"
  fi
  sleep "$PAUSE"
done
echo "La taverne ferme : $count requêtes, $ok OK, $client refusées, $server en erreur."
