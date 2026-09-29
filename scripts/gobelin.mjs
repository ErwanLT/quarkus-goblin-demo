#!/usr/bin/env node
// Pilote le gobelin (Quarkus Goblin) depuis un terminal, par le JSON-RPC de la Dev UI : pensé pour les agents IA
// comme pour les humains. Requiert Node 22 ou plus (WebSocket et fetch natifs) et l'application en dev mode.
//
//   node scripts/gobelin.mjs etat                         état, couches armées, auto-off restant
//   node scripts/gobelin.mjs config                       configuration complète
//   node scripts/gobelin.mjs armer '<json>'               applique une configuration (partielle), voir AGENTS.md
//   node scripts/gobelin.mjs actif on|off                 réveille ou endort le gobelin, sans toucher à sa configuration
//   node scripts/gobelin.mjs auto-off <minutes>           endort le gobelin tout seul après ce délai (1 à 1440)
//   node scripts/gobelin.mjs historique [n]               les n derniers assauts (20 par défaut)
//   node scripts/gobelin.mjs compteurs                    assauts par type et par source
//   node scripts/gobelin.mjs oublier                      vide l'historique et remet les compteurs à zéro
//   node scripts/gobelin.mjs tout-couper                  coupe tout : chaos inactif, assauts désarmés, auto-off annulé
//   node scripts/gobelin.mjs sauver <fichier>             enregistre la configuration courante (export de la Dev UI)
//   node scripts/gobelin.mjs restaurer <fichier>          réapplique une configuration enregistrée (import de la Dev UI)
//   node scripts/gobelin.mjs rapport                      rapport Markdown du gobelin
//   node scripts/gobelin.mjs sonde <METHODE> <chemin> [n] [json]
//                                                         envoie n requêtes (10 par défaut) et résume statuts et latences
//
// L'URL de la taverne se change avec TAVERN_URL (http://localhost:8080 par défaut).
import { readFileSync, writeFileSync } from 'node:fs';

const BASE = process.env.TAVERN_URL ?? 'http://localhost:8080';
const [command, ...args] = process.argv.slice(2);

function usage() {
  console.error('usage : node scripts/gobelin.mjs etat|config|armer|actif|auto-off|historique|compteurs|oublier|'
    + 'tout-couper|sauver|restaurer|rapport|sonde (détails en tête du script)');
  process.exit(2);
}

async function rpc(method, params = {}) {
  const ws = new WebSocket(BASE.replace(/^http/, 'ws') + '/q/dev-ui/json-rpc-ws');
  await new Promise((ok, ko) => {
    ws.addEventListener('open', ok, { once: true });
    ws.addEventListener('error', () => ko(new Error(`Dev UI injoignable sur ${BASE} : l'application tourne-t-elle en dev mode ?`)), { once: true });
  });
  try {
    return await new Promise((ok, ko) => {
      const timer = setTimeout(() => ko(new Error(`pas de réponse à ${method}`)), 10_000);
      ws.addEventListener('message', (event) => {
        const message = JSON.parse(event.data);
        if (message.id !== 1) {
          return;
        }
        clearTimeout(timer);
        if (message.error) {
          ko(new Error(`${method} : ${JSON.stringify(message.error)}`));
        } else {
          // la Dev UI enveloppe la valeur de retour dans { messageType, object }
          ok(message.result?.object ?? message.result);
        }
      });
      ws.send(JSON.stringify({ jsonrpc: '2.0', id: 1, method: `quarkus-goblin_${method}`, params }));
    });
  } finally {
    ws.close();
  }
}

function print(value) {
  console.log(typeof value === 'string' ? value : JSON.stringify(value, null, 2));
}

function percentile(sorted, p) {
  return sorted[Math.min(sorted.length - 1, Math.floor((p / 100) * sorted.length))];
}

async function sonde(method = 'GET', path, count = '10', body) {
  if (!path) {
    usage();
  }
  const statuses = {};
  const durations = [];
  const samples = {};
  for (let i = 0; i < Number(count); i++) {
    const start = performance.now();
    let status;
    let text = '';
    try {
      const response = await fetch(BASE + path, {
        method: method.toUpperCase(),
        headers: body ? { 'Content-Type': 'application/json' } : {},
        body,
        signal: AbortSignal.timeout(15_000),
      });
      status = String(response.status);
      text = await response.text();
    } catch (error) {
      status = error.name === 'TimeoutError' ? 'délai client (15 s)' : 'injoignable';
    }
    durations.push(Math.round(performance.now() - start));
    statuses[status] = (statuses[status] ?? 0) + 1;
    samples[status] ??= text.slice(0, 300);
  }
  durations.sort((a, b) => a - b);
  print({
    requete: `${method.toUpperCase()} ${path}`,
    envois: Number(count),
    statuts: statuses,
    latenceMs: { min: durations[0], p50: percentile(durations, 50), p95: percentile(durations, 95), max: durations.at(-1) },
    exemples: samples,
  });
}

try {
  switch (command) {
    case 'etat':
      print(await rpc('getStatus'));
      break;
    case 'config':
      print(await rpc('getConfig'));
      break;
    case 'armer':
      if (!args[0]) {
        usage();
      }
      print(await rpc('applyConfig', { config: JSON.parse(args[0]) }));
      break;
    case 'actif':
      if (args[0] !== 'on' && args[0] !== 'off') {
        usage();
      }
      print(await rpc('setActive', { active: args[0] === 'on' }));
      break;
    case 'auto-off':
      print(await rpc('startAutoOff', { minutes: Number(args[0] ?? 5) }));
      break;
    case 'historique': {
      const history = await rpc('getHistory');
      print(history.slice(-Number(args[0] ?? 20)));
      break;
    }
    case 'compteurs':
      print(await rpc('getCounters'));
      break;
    case 'oublier':
      await rpc('clearHistory');
      print(await rpc('resetCounters'));
      break;
    case 'tout-couper':
      print(await rpc('disableAll'));
      break;
    case 'sauver': {
      if (!args[0]) {
        usage();
      }
      const config = await rpc('getConfig');
      delete config.ok;
      writeFileSync(args[0], JSON.stringify(config, null, 2));
      print(`configuration enregistrée dans ${args[0]}`);
      break;
    }
    case 'restaurer':
      if (!args[0]) {
        usage();
      }
      print(await rpc('applyConfig', { config: JSON.parse(readFileSync(args[0], 'utf8')) }));
      break;
    case 'rapport':
      print((await rpc('getMarkdownReport')).markdown);
      break;
    case 'sonde':
      await sonde(...args);
      break;
    default:
      usage();
  }
} catch (error) {
  console.error(error.message);
  process.exit(1);
}
