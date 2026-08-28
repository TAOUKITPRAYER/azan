/**
 * check-custom-js.mjs — garde-fou statique pour app/src/main/assets/spec/custom.js
 *
 * custom.js n'est PAS transpilé/minifié (minifyEnabled false) : aucune étape de
 * build ne le regarde. Ce script rattrape la classe de bug qui a cassé l'azan
 * sur le boîtier aboubaker en v14.0 :
 *
 *   Une IIFE (ex. _installAzanPerPrayerVoiceGate) appelait `_ucPrayerAtMinutes(...)`
 *   alors que cette fonction est déclarée DANS l'énorme IIFE `injectTechOptionsUI`
 *   et n'est donc pas visible en portée lexicale depuis les IIFE frères.
 *   -> `Uncaught ReferenceError: _ucPrayerAtMinutes is not defined` à chaque azan,
 *      invisible à l'écriture ET à la "compilation".
 *
 * Convention du fichier pour partager un helper hors de son IIFE :
 *   1. `window.leHelper = leHelper;` à la fin de l'IIFE qui le définit
 *   2. site d'appel : `window.leHelper` (jamais le nom nu), gardé par
 *      `typeof window.leHelper === 'function'` ou `window.leHelper && ...`
 *
 * Ce script signale toute référence NUE (ni `window.x`, ni `typeof x`) à un nom
 * qui n'existe qu'en portée de fonction et qui est utilisé hors de cette portée.
 *
 *   CRASH  : lecture / appel du nom      -> ReferenceError garanti à l'exécution
 *   LATENT : `x = ...` (nom nu en cible) -> en mode sloppy, crée un global
 *            silencieux (pas de crash) mais casse quasi toujours l'intention
 *
 * Sortie : code 0 si aucun CRASH, 1 si au moins un CRASH, 2 si le script ne peut
 * pas s'exécuter (dépendances / fichier absent). Les LATENT n'échouent pas.
 *
 * Dépendances (tools/package.json) : espree + eslint-scope.
 *   -> `npm --prefix tools install`  (une seule fois)
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const TARGET = path.resolve(HERE, "../app/src/main/assets/spec/custom.js");

let espree, eslintScope;
try {
  espree = await import("espree");
  eslintScope = await import("eslint-scope");
} catch {
  console.error(
    "[check-custom-js] dépendances manquantes.\n" +
      "  Lance une fois :  npm --prefix tools install\n"
  );
  process.exit(2);
}

if (!fs.existsSync(TARGET)) {
  console.error(`[check-custom-js] introuvable : ${TARGET}`);
  process.exit(2);
}

const src = fs.readFileSync(TARGET, "utf8");
const lines = src.split(/\r?\n/);

let ast;
try {
  ast = espree.parse(src, { ecmaVersion: 2022, loc: true, range: true });
} catch (e) {
  console.error(
    `[check-custom-js] ERREUR DE SYNTAXE custom.js:${e.lineNumber ?? "?"}  ${e.message}`
  );
  process.exit(1);
}

// parent de chaque noeud (eslint-scope ne l'expose pas)
(function attachParents(node, parent) {
  if (!node || typeof node.type !== "string") return;
  node.__parent = parent;
  for (const k of Object.keys(node)) {
    if (k === "__parent") continue;
    const c = node[k];
    if (Array.isArray(c)) c.forEach((x) => attachParents(x, node));
    else if (c && typeof c.type === "string") attachParents(c, node);
  }
})(ast, null);

const sm = eslintScope.analyze(ast, { ecmaVersion: 2022, sourceType: "script" });

// `typeof <name>` présent quelque part dans un sous-arbre ?
function subtreeHasTypeofGuard(node, name) {
  let found = false;
  (function scan(n) {
    if (found || !n || typeof n.type !== "string") return;
    if (
      n.type === "UnaryExpression" &&
      n.operator === "typeof" &&
      ((n.argument.type === "Identifier" && n.argument.name === name) ||
        (n.argument.type === "MemberExpression" &&
          !n.argument.computed &&
          n.argument.property.type === "Identifier" &&
          n.argument.property.name === name))
    ) {
      found = true;
      return;
    }
    for (const k of Object.keys(n)) {
      if (k === "__parent") continue;
      const c = n[k];
      if (Array.isArray(c)) c.forEach(scan);
      else if (c && typeof c.type === "string") scan(c);
    }
  })(node);
  return found;
}

// La référence `id` (nom `name`) est-elle dominée par une garde d'un ancêtre ?
function isGuardedByAncestor(id, name) {
  let cur = id;
  let parent = id.__parent;
  while (parent) {
    // ternaire / if : garde dans .test
    if (
      (parent.type === "ConditionalExpression" || parent.type === "IfStatement") &&
      parent.test &&
      cur !== parent.test &&
      subtreeHasTypeofGuard(parent.test, name)
    ) {
      return true;
    }
    // `A && B` : si `id` est dans B et A contient une garde (typeof ou `name`)
    if (parent.type === "LogicalExpression" && parent.operator === "&&" && cur === parent.right) {
      if (
        subtreeHasTypeofGuard(parent.left, name) ||
        (parent.left.type === "Identifier" && parent.left.name === name)
      ) {
        return true;
      }
    }
    cur = parent;
    parent = parent.__parent;
  }
  return false;
}

// --- 1. noms réellement globaux à l'exécution --------------------------
//   a) déclarations top-level (var / function) -> variables du globalScope
//   b) Annex B (mode sloppy navigateur) : `function NAME(){}` dans un bloc
//      dont le seul ancêtre "fonction" est le Program -> hissée en global
const runtimeGlobal = new Set();
for (const v of sm.globalScope.variables) runtimeGlobal.add(v.name);
(function markAnnexB(node, fnDepth) {
  if (!node || typeof node.type !== "string") return;
  if (node.type === "FunctionDeclaration" && node.id && fnDepth === 0) {
    runtimeGlobal.add(node.id.name);
  }
  const isFn = /^(FunctionDeclaration|FunctionExpression|ArrowFunctionExpression)$/.test(node.type);
  for (const k of Object.keys(node)) {
    if (k === "__parent") continue;
    const c = node[k];
    if (Array.isArray(c)) c.forEach((x) => markAnnexB(x, fnDepth + (isFn ? 1 : 0)));
    else if (c && typeof c.type === "string") markAnnexB(c, fnDepth + (isFn ? 1 : 0));
  }
})(ast, 0);

// --- 2. noms exposés sur window / globalThis --------------------------
const windowExport = new Set();
(function walk(n) {
  if (!n || typeof n.type !== "string") return;
  if (
    n.type === "AssignmentExpression" &&
    n.left.type === "MemberExpression" &&
    !n.left.computed &&
    n.left.object.type === "Identifier" &&
    (n.left.object.name === "window" || n.left.object.name === "globalThis") &&
    n.left.property.type === "Identifier"
  ) {
    windowExport.add(n.left.property.name);
  }
  for (const k of Object.keys(n)) {
    if (k === "__parent") continue;
    const c = n[k];
    if (Array.isArray(c)) c.forEach(walk);
    else if (c && typeof c.type === "string") walk(c);
  }
})(ast);

// --- 3. tout nom déclaré quelque part (n'importe quelle portée) -------
const declaredSomewhere = new Map(); // name -> [lignes]
(function visit(scope) {
  for (const v of scope.variables) {
    for (const def of v.defs) {
      const ln = def.node?.loc?.start.line ?? def.name?.loc?.start.line ?? 0;
      if (!declaredSomewhere.has(v.name)) declaredSomewhere.set(v.name, []);
      declaredSomewhere.get(v.name).push(ln);
    }
  }
  scope.childScopes.forEach(visit);
})(sm.globalScope);

// --- 4. références échappées de TOUTES les portées -------------------
const crashes = [];
const latent = [];
for (const ref of sm.globalScope.through) {
  const id = ref.identifier;
  const name = id.name;
  const ln = id.loc.start.line;

  if (runtimeGlobal.has(name)) continue; // résolu à l'exécution
  if (windowExport.has(name)) continue; // partagé via window
  if (!declaredSomewhere.has(name)) continue; // global cœur (m2body/m1prime) ou navigateur

  const p = id.__parent;
  // `typeof name` -> jamais de throw, même si non défini
  if (p && p.type === "UnaryExpression" && p.operator === "typeof" && p.argument === id) continue;

  // Référence protégée par une garde `typeof <name>` ou `<name> &&` portée par
  // un ancêtre (ternaire / if / &&) : `(typeof f === 'function') ? f() : ''`,
  // `f && f()`, `if (typeof f !== 'undefined') { ... f() ... }`. Pas de throw.
  if (isGuardedByAncestor(id, name)) continue;

  const entry = {
    name,
    line: ln,
    text: (lines[ln - 1] || "").trim(),
    declaredAt: (declaredSomewhere.get(name) || []).join(", "),
  };
  const isBareAssignTarget =
    p && p.type === "AssignmentExpression" && p.operator === "=" && p.left === id;
  if (isBareAssignTarget) latent.push(entry);
  else crashes.push(entry);
}

// --- 5. rapport -----------------------------------------------------
const rel = path.relative(path.resolve(HERE, ".."), TARGET).replace(/\\/g, "/");
const byLine = (a, b) => a.line - b.line;
function show(list, header) {
  if (!list.length) return;
  console.log(`\n${header}`);
  for (const e of list.sort(byLine)) {
    console.log(`  ${rel}:${e.line}  « ${e.name} »   (déclaré ligne ${e.declaredAt})`);
    console.log(`      ${e.text}`);
  }
}

if (!crashes.length && !latent.length) {
  console.log(`[check-custom-js] OK — ${rel} : aucune référence hors-portée.`);
  process.exit(0);
}

show(
  crashes,
  "✗ CRASH — référence NUE hors portée = ReferenceError à l'exécution.\n" +
    "  Corriger : `window.<nom> = <nom>;` à la fin de l'IIFE qui le définit,\n" +
    "  puis au site d'appel `window.<nom>` gardé par `typeof window.<nom> === 'function'` :"
);
show(
  latent,
  "⚠ LATENT — affectation à un nom nu hors portée (mode sloppy : crée un global\n" +
    "  silencieux, pas de crash, mais l'intention est presque toujours cassée) :"
);

console.log("");
if (crashes.length) {
  console.error(
    `[check-custom-js] ÉCHEC : ${crashes.length} référence(s) qui planteront à l'exécution.`
  );
  process.exit(1);
}
console.log(`[check-custom-js] ${latent.length} avertissement(s) LATENT — build non bloqué.`);
process.exit(0);
