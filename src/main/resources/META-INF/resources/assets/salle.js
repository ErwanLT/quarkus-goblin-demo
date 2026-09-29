// La salle de The Falling Whale : écoute le flux SSE de /salle/evenements et anime l'écran.
(() => {
  const MAX_COMMANDES = 14;
  const MAX_LIVRAISONS = 6;
  const SEUIL_LENTE_MS = 800;

  const $ = (id) => document.getElementById(id);
  const compteurs = { servies: 0, refusees: 0, perdues: 0 };
  const durees = [];
  const orInitial = new Map();

  document.querySelectorAll('.aventurier').forEach((el) => orInitial.set(el.dataset.id, Number(el.dataset.or)));
  const orMax = Math.max(1, ...orInitial.values());
  document.querySelectorAll('.aventurier').forEach((el) => majJauge(el, Number(el.dataset.or)));

  const heure = (iso) => new Date(iso).toLocaleTimeString('fr-FR');
  // « chariot d'Elwen », « chariot de Borin »
  const de = (nom) => (/^[aeiouyéèêàâîïôœh]/i.test(nom) ? `d'${nom}` : `de ${nom}`);

  function ajouter(liste, element, max) {
    const vide = liste.querySelector('.vide');
    if (vide) vide.remove();
    liste.prepend(element);
    while (liste.children.length > max) liste.lastElementChild.remove();
  }

  function ligne(classe, contenu) {
    const li = document.createElement('li');
    li.className = classe;
    contenu.forEach(([tag, cls, texte]) => {
      const el = document.createElement(tag);
      el.className = cls;
      el.textContent = texte;
      li.appendChild(el);
    });
    return li;
  }

  // ------------------------------------------------ le comptoir
  const ETIQUETTES = { SERVIE: 'Servie', REFUSEE: 'Refusée', REFOULEE: 'Refoulée', ABANDONNEE: 'Abandonnée', PERDUE: 'Perdue' };

  function commande(at, d) {
    const statut = d.statut;
    let qui;
    let detail;
    if (statut === 'SERVIE') {
      qui = d.aventurier;
      detail = `${d.quantite} × ${d.recette} · ${d.total} po`;
      compteurs.servies++;
      durees.push(d.dureeMs);
      if (durees.length > 20) durees.shift();
      bourse(d.aventurierId, d.bourse, `A payé ${d.total} po pour ${d.quantite} × ${d.recette}`, 'paye');
    } else {
      qui = d.titre || `Erreur ${d.status}`;
      detail = d.detail || '';
      // pour le public : ce qui compte, c'est que personne ne paie une commande jamais servie
      if (statut === 'ABANDONNEE') detail = 'Coupée par le délai du comptoir : rien n\'est débité.';
      if (statut === 'PERDUE') detail = 'Perdue en cuisine : rien n\'est débité.';
      if (statut === 'ABANDONNEE' || statut === 'PERDUE') compteurs.perdues++;
      else compteurs.refusees++;
    }
    const li = document.createElement('li');
    li.className = `commande ${statut.toLowerCase()}${d.dureeMs >= SEUIL_LENTE_MS ? ' lente' : ''}`;
    const heureEl = document.createElement('span');
    heureEl.className = 'heure';
    heureEl.textContent = heure(at);
    const quoi = document.createElement('span');
    quoi.className = 'quoi';
    const quiEl = document.createElement('span');
    quiEl.className = 'qui';
    quiEl.textContent = qui;
    const detailEl = document.createElement('span');
    detailEl.className = 'detail';
    detailEl.textContent = detail;
    quoi.append(quiEl, detailEl);
    const duree = document.createElement('span');
    duree.className = 'duree';
    duree.textContent = `${d.dureeMs} ms`;
    const etiquette = document.createElement('span');
    etiquette.className = 'etiquette';
    etiquette.textContent = ETIQUETTES[statut] || statut;
    li.append(heureEl, quoi, duree, etiquette);
    ajouter($('commandes'), li, MAX_COMMANDES);
    majCompteurs();
  }

  function majCompteurs() {
    $('nb-servies').textContent = compteurs.servies;
    $('nb-refusees').textContent = compteurs.refusees;
    $('nb-perdues').textContent = compteurs.perdues;
    $('temps-moyen').textContent = durees.length ? Math.round(durees.reduce((a, b) => a + b, 0) / durees.length) : '-';
  }

  // ------------------------------------------------ les bourses
  function majJauge(el, or) {
    el.querySelector('.jauge span').style.width = `${Math.max(0, Math.min(100, (or / orMax) * 100))}%`;
  }

  function bourse(id, or, texte, effet) {
    const el = document.querySelector(`.aventurier[data-id="${id}"]`);
    if (!el || or === undefined) return;
    el.querySelector('.or').textContent = or;
    el.querySelector('.aventurier-derniere').textContent = texte;
    majJauge(el, or);
    el.classList.remove('paye', 'epargne');
    void el.offsetWidth;
    el.classList.add(effet);
    setTimeout(() => el.classList.remove(effet), 900);
  }

  // ------------------------------------------------ la carte
  function carte(d) {
    const ardoise = d.source === 'ARDOISE';
    $('carte').classList.toggle('ardoise', ardoise);
    $('bandeau-carte').textContent = ardoise
      ? `La cave ne répond plus : carte servie depuis l'ardoise, depuis ${heure(d.depuis)} (${d.ardoisesServies} fois ce soir).`
      : 'Carte fraîche, tout droit du grimoire.';
  }

  // ------------------------------------------------ la porte de derrière
  const DISJONCTEUR = {
    CLOSED: ['Disjoncteur fermé', 'Les coursiers partent vers la guilde des marchands.'],
    OPEN: ['Disjoncteur ouvert', "Trop d'échecs : plus aucun coursier pendant 10 secondes. Le repli répond tout de suite."],
    HALF_OPEN: ['Disjoncteur à moitié ouvert', "Un coursier d'essai part voir si la guilde répond de nouveau."],
  };

  function incident(data) {
    const zone = $('incident');
    zone.replaceChildren();
    zone.classList.toggle('en-cours', data.id !== undefined);
    if (data.id !== undefined) {
      zone.textContent = `Incident ${data.id} en cours depuis ${heure(data.depuis)} : ${data.declencheur}`;
    } else if (data.dernier !== undefined) {
      const lien = document.createElement('a');
      lien.href = `/exploitation/incidents/${data.dernier}/post-mortem`;
      lien.target = '_blank';
      lien.rel = 'noopener';
      lien.textContent = `post-mortem de l'incident ${data.dernier}`;
      zone.append('Main courante : ', lien);
    }
  }

  function disjoncteur(etat) {
    const [titre, detail] = DISJONCTEUR[etat] || [etat, ''];
    $('disjoncteur').dataset.etat = etat;
    $('disjoncteur-titre').textContent = titre;
    $('disjoncteur-detail').textContent = detail;
  }

  function livraison(at, d) {
    const texte = d.livree
      ? `${d.quantite} ${d.ingredient}, chariot ${de(d.chariot)} · étagère à ${d.stock}`
      : `${d.ingredient} : le marchand n'est pas venu`;
    ajouter($('livraisons'), ligne(`livraison${d.livree ? '' : ' absent'}`, [['span', 'heure', heure(at)], ['span', 'quoi', texte]]), MAX_LIVRAISONS);
  }

  // ------------------------------------------------ le gobelin
  function gobelin(g) {
    $('gobelin').classList.toggle('rode', g.actif);
    if (!g.actif) {
      $('gobelin-titre').textContent = 'Le gobelin dort';
      $('gobelin-detail').textContent = 'Aucune panne injectée.';
      return;
    }
    const assauts = g.assauts.length ? g.assauts.join(', ') : 'rien pour l\'instant';
    $('gobelin-titre').textContent = 'Le gobelin rôde';
    $('gobelin-detail').textContent = `${assauts} · ${g.niveau} % des requêtes · ${g.total} assauts ce soir`;
  }

  // ------------------------------------------------ le flux
  function connecter() {
    const source = new EventSource('/salle/evenements');
    source.onopen = () => { $('connexion').textContent = 'En direct'; $('connexion').classList.add('ok'); };
    source.onerror = () => { $('connexion').textContent = 'Connexion perdue, nouvelle tentative…'; $('connexion').classList.remove('ok'); };
    source.onmessage = (message) => {
      const e = JSON.parse(message.data);
      switch (e.type) {
        case 'commande': commande(e.at, e.data); break;
        case 'carte': carte(e.data); break;
        case 'livraison': livraison(e.at, e.data); break;
        case 'disjoncteur': disjoncteur(e.data.etat); break;
        case 'etat':
          gobelin(e.data.gobelin);
          disjoncteur(e.data.disjoncteur);
          carte(e.data.carte);
          incident(e.data.incident);
          break;
        default: break;
      }
    };
  }

  connecter();
})();
