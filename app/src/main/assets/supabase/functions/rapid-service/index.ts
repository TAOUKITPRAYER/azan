// ── rapid-service — Supabase Edge Function ────────────────────────────────────
// Cinq modes d'utilisation :
//
//   1. Webhook Supabase (Database → mosques UPDATE, trigger on_mosque_update) :
//      Body = { record: { mosque_id, mosque_name, ... } }
//      → notif VISIBLE "Mise à jour des horaires" (journalisée type='notifier')
//      ATTENTION : ce trigger se déclenche sur TOUTE UPDATE de la ligne
//      mosques, y compris celles écrites par le mode 3 ci-dessous.
//
//   2. Appel direct (message personnalisé de l'admin) :
//      Body = { type: "custom_message", mosque_id, mosque_name, message, notif_type }
//      notif_type : 'message' (défaut) ou 'janaza'.
//      → notif avec le message de l'admin en titre/corps
//
//   3. Administration à distance -- CONFIG (téléphone du responsable, PIN requis) :
//      Body = { type: "remote_config_update", mosque_id, pin, config: {...} }
//      Écrit dans mosques (horaires/Coran), toujours suivi d'un push silencieux
//      + (via le trigger du mode 1) d'une notif visible "horaires mis à jour".
//      Voir custom.js _installRemoteMosqueAdmin (onglet Paramétrage).
//
//   4. Administration à distance -- ACTION (téléphone du responsable, PIN requis) :
//      Body = { type: "remote_action", mosque_id, pin, action, target? }
//      action: "quran_play" | "quran_stop" | "quran_toggle" | "azan_play" |
//              "azan_preview" | "azan_stop" | "audio_refresh" | "light_on" |
//              "light_off" | "lights_refresh" | "reload" | "send_debug" | "update_app"
//      target (lumières uniquement): "ampliExt" | "ampliInt" | "minaret" | "mihrab"
//      AUCUNE écriture dans mosques (commande ponctuelle, pas un changement de
//      configuration) -- donc PAS de notif visible "horaires mis à jour", juste
//      le push silencieux qui fait exécuter l'action côté box. Voir custom.js
//      _installRemoteMosqueAdmin (onglet Actions) et le listener 'ucRemoteAction'.
//
//   4bis. Test du PIN seul (bouton "Vérifier l'accès" du téléphone) :
//      Body = { type: "verify_pin", mosque_id, pin }
//      → 200 si le PIN correspond à mosques.pin_hash, 401 sinon, 403 si aucun
//      PIN défini. Aucune action / écriture / push ; échec NON journalisé.
//
//   5. Alerte de surveillance des modules Shelly (box → admin uniquement) :
//      Body = { type: "light_watchdog_alert", mosque_id, mosque_name?, module,
//               ip, checkpoint /* "70" | "30" */, prayer /* "FAJR"... */ }
//      AUCUN PIN (même posture de confiance que le mode 2 : la box est la seule
//      source légitime sur son propre LAN ; ce mode est strictement MOINS
//      abusable que custom_message puisqu'il ne cible QUE les téléphones déjà
//      porteurs du tag mosque_admin_<id>, pas tous les abonnés). Émis par
//      custom.js (_ucLightWatchdogTick / _ucLwdSendAlert, box uniquement),
//      1h10 puis 30min avant l'azan de chaque prière, quand un module Shelly
//      activé ne répond pas.
//      → notif VISIBLE ciblée sur le(s) téléphone(s) administrateur(s).
//
// Le PIN n'est JAMAIS fait confiance côté client pour les modes 3/4
// (contrairement aux modes 1/2/5 et à l'écriture directe PostgREST sur mosques,
// qui reposent sur le modèle de confiance "à l'échelle personnelle" documenté
// dans le CLAUDE.md racine du projet) : on vérifie le hash SHA-256 du PIN
// contre mosques.pin_hash via service_role (jamais exposé au client), AVANT
// toute action. data.silent=true DISTINGUE ces push du push VISIBLE du mode 1
// (même type "config_update" pour le mode 3) : côté Android, seul silent=true
// supprime la bannière système (cf. MainActivity.kt, addForegroundLifecycleListener).
//
// Ciblage OneSignal :
//   - modes 1/2/3/4 : tag mosque_sub_<id_sanitizé> EXISTS — posé par
//     addMosqueSubscriptionTag (MobileJsBridge.kt) pour CHAQUE mosquée de
//     l'historique de l'utilisateur (non mise en sourdine).
//   - mode 5 : tag mosque_admin_<id_sanitizé> EXISTS — posé par
//     addMosqueAdminTag (MobileJsBridge.kt) UNIQUEMENT sur les téléphones dont
//     le PIN d'administration à distance a été validé côté serveur
//     (custom.js _ucArmMosqueAdminPush). Plusieurs téléphones admin possibles.
//
// Variables d'environnement (Supabase Dashboard → Edge Functions → Secrets) :
//   ONESIGNAL_APP_ID / ONESIGNAL_API_KEY / SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY
// ─────────────────────────────────────────────────────────────────────────────

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const ONESIGNAL_APP_ID  = Deno.env.get("ONESIGNAL_APP_ID")  ?? "a7656f67-9573-4593-97a8-871ac6550731";
const ONESIGNAL_API_KEY = Deno.env.get("ONESIGNAL_API_KEY") ?? "";

const SUPABASE_URL         = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

const CORS = {
  "Access-Control-Allow-Origin":  "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function sanitizeMosqueId(id: string): string {
  return id.replace(/[^a-zA-Z0-9_]/g, "_");
}

function mosqueFilters(mosque_id: string) {
  return [
    { field: "tag", key: `mosque_sub_${sanitizeMosqueId(mosque_id)}`, relation: "exists" },
  ];
}

// Ciblage mode 5 : uniquement les téléphones administrateurs (PIN validé serveur).
function mosqueAdminFilters(mosque_id: string) {
  return [
    { field: "tag", key: `mosque_admin_${sanitizeMosqueId(mosque_id)}`, relation: "exists" },
  ];
}

// SHA-256 hex — DOIT rester strictement identique à _ucSha256Hex() côté custom.js.
async function sha256Hex(s: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(s));
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
}

async function _logNotification(mosque_id: string, type: string, body: string, ok: boolean) {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_KEY || !mosque_id) return;
  try {
    const res = await fetch(`${SUPABASE_URL}/rest/v1/mosque_notifications`, {
      method: "POST",
      headers: {
        "Content-Type":  "application/json",
        "apikey":         SUPABASE_SERVICE_KEY,
        "Authorization": `Bearer ${SUPABASE_SERVICE_KEY}`,
        "Prefer":         "return=minimal",
      },
      body: JSON.stringify({ mosque_id, type, body, ok }),
    });
    if (!res.ok) {
      console.error("Journalisation mosque_notifications échouée :", res.status, await res.text());
    }
  } catch (e) {
    console.error("Journalisation mosque_notifications — erreur réseau :", e);
  }
}

// Valide mosque_id+pin contre mosques.pin_hash (service_role, jamais exposé).
// Retourne une Response d'erreur prête à renvoyer si invalide, ou null si OK.
// logFailure=false : ne journalise PAS la tentative ratée (bouton "Vérifier
// l'accès" — l'imam peut se tromper de chiffre, inutile de polluer l'historique).
async function _validateRemotePin(mosque_id: string, pin: string, logFailure = true): Promise<Response | null> {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_KEY) {
    console.error("SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY non disponibles");
    return new Response("Config manquante", { status: 500, headers: CORS });
  }
  let row: Record<string, unknown> | undefined;
  try {
    const rowRes = await fetch(
      `${SUPABASE_URL}/rest/v1/mosques?mosque_id=eq.${encodeURIComponent(mosque_id)}&select=pin_hash`,
      { headers: { apikey: SUPABASE_SERVICE_KEY, Authorization: `Bearer ${SUPABASE_SERVICE_KEY}` } }
    );
    const rows = await rowRes.json();
    row = Array.isArray(rows) ? rows[0] : undefined;
  } catch (e) {
    console.error("Lecture mosques.pin_hash échouée :", e);
    return new Response("Erreur serveur", { status: 500, headers: CORS });
  }
  if (!row || !row.pin_hash) {
    return new Response(
      JSON.stringify({ error: "Administration à distance non configurée pour cette mosquée" }),
      { status: 403, headers: { ...CORS, "Content-Type": "application/json" } }
    );
  }
  const pinHex = await sha256Hex(pin);
  if (pinHex !== row.pin_hash) {
    if (logFailure) {
      await _logNotification(mosque_id, "config_update", "PIN invalide (tentative distante)", false);
    }
    return new Response(JSON.stringify({ error: "PIN invalide" }),
      { status: 401, headers: { ...CORS, "Content-Type": "application/json" } });
  }
  return null;
}

// ── Mode 3 : administration à distance -- CONFIG ─────────────────────
const REMOTE_ADMIN_ALLOWED_FIELDS = [
  "azan_offsets", "iqama_delay", "iqama_fixed", "jumua", "eid",
  "primary_azan", "dohr_before_asr_min", "quran_settings",
];

async function handleRemoteConfigUpdate(body: Record<string, unknown>): Promise<Response> {
  const mosque_id = ((body.mosque_id as string) ?? "").trim();
  const pin       = ((body.pin as string) ?? "").trim();
  const config     = (body.config as Record<string, unknown>) ?? {};

  if (!mosque_id || !pin) {
    return new Response(JSON.stringify({ error: "mosque_id et pin requis" }),
      { status: 400, headers: { ...CORS, "Content-Type": "application/json" } });
  }

  const pinErr = await _validateRemotePin(mosque_id, pin);
  if (pinErr) return pinErr;

  const patch: Record<string, unknown> = {};
  for (const k of REMOTE_ADMIN_ALLOWED_FIELDS) {
    if (k in config) patch[k] = config[k];
  }
  // "Recharger la box" sans changer de valeur (config vide) : on force quand
  // même une écriture pour que updated_at avance (filet de sécurité polling).
  if (Object.keys(patch).length === 0) {
    patch.updated_at = new Date().toISOString();
  }

  let upd: Response;
  try {
    upd = await fetch(
      `${SUPABASE_URL}/rest/v1/mosques?mosque_id=eq.${encodeURIComponent(mosque_id)}`,
      {
        method: "PATCH",
        headers: {
          "Content-Type":  "application/json",
          "apikey":         SUPABASE_SERVICE_KEY,
          "Authorization": `Bearer ${SUPABASE_SERVICE_KEY}`,
          "Prefer":         "return=minimal",
        },
        body: JSON.stringify(patch),
      }
    );
  } catch (e) {
    console.error("Ecriture mosques (remote_config_update) échouée :", e);
    return new Response("Erreur réseau", { status: 502, headers: CORS });
  }
  if (!upd.ok) {
    console.error("PATCH mosques échoué :", upd.status, await upd.text());
    return new Response(JSON.stringify({ error: "Échec de la mise à jour" }),
      { status: 500, headers: { ...CORS, "Content-Type": "application/json" } });
  }

  try {
    await fetch("https://onesignal.com/api/v1/notifications", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Basic ${ONESIGNAL_API_KEY}` },
      body: JSON.stringify({
        app_id: ONESIGNAL_APP_ID,
        filters: mosqueFilters(mosque_id),
        contents: { en: " " },
        content_available: true,
        data: { type: "config_update", mosque_id, silent: true },
        priority: 10,
      }),
    });
  } catch (e) {
    console.error("Push silencieux config_update échoué :", e);
  }

  await _logNotification(mosque_id, "config_update", `remote_admin: ${Object.keys(patch).join(",")}`, true);

  return new Response(JSON.stringify({ ok: true }),
    { headers: { ...CORS, "Content-Type": "application/json" }, status: 200 });
}

// ── Mode 4 : administration à distance -- ACTION (pas d'écriture mosques) ──
const REMOTE_ACTIONS = [
  "quran_play", "quran_stop", "quran_toggle",
  "azan_play", "azan_preview", "azan_stop", "audio_refresh",
  "light_on", "light_off", "lights_refresh",
  "reload", "send_debug", "update_app",
];
const REMOTE_ACTION_TARGETS = ["ampliExt", "ampliInt", "minaret", "mihrab"];

async function handleRemoteAction(body: Record<string, unknown>): Promise<Response> {
  const mosque_id = ((body.mosque_id as string) ?? "").trim();
  const pin       = ((body.pin as string) ?? "").trim();
  const action     = ((body.action as string) ?? "").trim();
  const target     = ((body.target as string) ?? "").trim();

  if (!mosque_id || !pin || !REMOTE_ACTIONS.includes(action)) {
    return new Response(JSON.stringify({ error: "mosque_id, pin et action valide requis" }),
      { status: 400, headers: { ...CORS, "Content-Type": "application/json" } });
  }
  if ((action === "light_on" || action === "light_off") && !REMOTE_ACTION_TARGETS.includes(target)) {
    return new Response(JSON.stringify({ error: "target invalide pour une action lumière" }),
      { status: 400, headers: { ...CORS, "Content-Type": "application/json" } });
  }

  const pinErr = await _validateRemotePin(mosque_id, pin);
  if (pinErr) return pinErr;

  // Aucune écriture dans mosques : commande ponctuelle, pas un changement de
  // config -- donc pas de notif visible "horaires mis à jour" via le trigger
  // du mode 1, juste le push silencieux qui fait exécuter l'action côté box.
  const data: Record<string, unknown> = { type: "remote_action", mosque_id, action, silent: true };
  if (target) data.target = target;

  try {
    await fetch("https://onesignal.com/api/v1/notifications", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Basic ${ONESIGNAL_API_KEY}` },
      body: JSON.stringify({
        app_id: ONESIGNAL_APP_ID,
        filters: mosqueFilters(mosque_id),
        contents: { en: " " },
        content_available: true,
        data,
        priority: 10,
      }),
    });
  } catch (e) {
    console.error("Push silencieux remote_action échoué :", e);
    return new Response(JSON.stringify({ error: "Erreur réseau OneSignal" }),
      { status: 502, headers: { ...CORS, "Content-Type": "application/json" } });
  }

  await _logNotification(mosque_id, "remote_action", `${action}${target ? ":" + target : ""}`, true);

  return new Response(JSON.stringify({ ok: true }),
    { headers: { ...CORS, "Content-Type": "application/json" }, status: 200 });
}

// ── Mode 5 : alerte de surveillance des modules Shelly (box → admin) ──────
const LIGHT_WATCHDOG_MODULES     = ["ampliInt", "ampliExt", "minaret", "mihrab", "roller"];
const LIGHT_WATCHDOG_CHECKPOINTS = ["70", "30"];

// Libellés lisibles (module + prière) pour le corps de la notification.
const LWD_MODULE_LABEL: Record<string, { ar: string; fr: string }> = {
  ampliInt: { ar: "مكبّر الإمام",        fr: "ampli imam (intérieur)" },
  ampliExt: { ar: "مكبّر الأذان الخارجي", fr: "ampli azan (extérieur)" },
  minaret:  { ar: "المئذنة",              fr: "minaret" },
  mihrab:   { ar: "المحراب",              fr: "mihrab" },
  roller:   { ar: "الستارة",              fr: "volet" },
};
const LWD_PRAYER_LABEL: Record<string, { ar: string; fr: string }> = {
  FAJR:  { ar: "الفجر",   fr: "Fajr" },
  DOHR:  { ar: "الظهر",   fr: "Dhuhr" },
  ASSR:  { ar: "العصر",   fr: "Asr" },
  MGRB:  { ar: "المغرب",  fr: "Maghreb" },
  ISHA:  { ar: "العشاء",  fr: "Isha" },
  JOMOA: { ar: "الجمعة",  fr: "Jumu'a" },
};

async function handleLightWatchdogAlert(body: Record<string, unknown>): Promise<Response> {
  const mosque_id   = ((body.mosque_id   as string) ?? "").trim();
  const mosque_name = ((body.mosque_name as string) ?? mosque_id).trim() || mosque_id;
  const moduleId    = ((body.module      as string) ?? "").trim();
  const ip          = ((body.ip          as string) ?? "").trim().replace(/^https?:\/\//, "");
  const checkpoint  = ((body.checkpoint  as string) ?? "").trim();
  const prayer      = ((body.prayer      as string) ?? "").trim().toUpperCase();

  if (!mosque_id || !LIGHT_WATCHDOG_MODULES.includes(moduleId) || !LIGHT_WATCHDOG_CHECKPOINTS.includes(checkpoint)) {
    return new Response(JSON.stringify({ error: "mosque_id, module et checkpoint valides requis" }),
      { status: 400, headers: { ...CORS, "Content-Type": "application/json" } });
  }

  const modL = LWD_MODULE_LABEL[moduleId] || { ar: moduleId, fr: moduleId };
  const prL  = LWD_PRAYER_LABEL[prayer]   || { ar: prayer,   fr: prayer };
  const whenFr = checkpoint === "70" ? "1 h 10" : "30 min";
  const whenAr = checkpoint === "70" ? "ساعة و10 دقائق" : "30 دقيقة";

  const bodyFr = `⚠️ ${modL.fr}${ip ? " (" + ip + ")" : ""} ne répond pas — ${whenFr} avant l'azan de ${prL.fr}`;
  const bodyAr = `⚠️ ${modL.ar}${ip ? " (" + ip + ")" : ""} لا يستجيب — قبل أذان ${prL.ar} بـ${whenAr}`;

  const notification = {
    app_id: ONESIGNAL_APP_ID,
    filters: mosqueAdminFilters(mosque_id),
    headings: { ar: mosque_name, fr: mosque_name, en: mosque_name },
    contents: { ar: bodyAr, fr: bodyFr, en: bodyFr },
    data: { type: "light_watchdog", mosque_id, module: moduleId, ip, checkpoint, prayer },
    android_visibility: 1,
    priority: 10,
  };

  let osResponse: Response;
  try {
    osResponse = await fetch("https://onesignal.com/api/v1/notifications", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Basic ${ONESIGNAL_API_KEY}` },
      body: JSON.stringify(notification),
    });
  } catch (e) {
    console.error("Fetch OneSignal (light_watchdog_alert) échoué :", e);
    await _logNotification(mosque_id, "light_watchdog", `${moduleId}:${ip}@${checkpoint}min/${prayer}`, false);
    return new Response("Erreur réseau OneSignal", { status: 502, headers: CORS });
  }

  const osResult = await osResponse.json();
  console.log(`light_watchdog_alert → ${mosque_id} ${moduleId}@${checkpoint}min/${prayer} :`, JSON.stringify(osResult));

  await _logNotification(mosque_id, "light_watchdog", `${moduleId}:${ip}@${checkpoint}min/${prayer}`, osResponse.ok);

  return new Response(JSON.stringify({ ok: osResponse.ok, onesignal: osResult }),
    { headers: { ...CORS, "Content-Type": "application/json" }, status: osResponse.ok ? 200 : 500 });
}

serve(async (req: Request) => {

  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: CORS });
  }

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return new Response("Invalid JSON", { status: 400, headers: CORS });
  }

  if (!ONESIGNAL_APP_ID || !ONESIGNAL_API_KEY) {
    console.error("Variables ONESIGNAL_APP_ID / ONESIGNAL_API_KEY non configurées");
    return new Response("Config manquante", { status: 500, headers: CORS });
  }

  const msgType = body.type as string;

  if (msgType === "remote_config_update") {
    return handleRemoteConfigUpdate(body);
  }
  if (msgType === "remote_action") {
    return handleRemoteAction(body);
  }
  if (msgType === "verify_pin") {
    // Test du PIN uniquement (bouton "Vérifier l'accès" du téléphone) : 200 si
    // le PIN correspond à mosques.pin_hash, 401 sinon, 403 si aucun PIN défini.
    // Aucune action, aucune écriture, aucun push, échec non journalisé.
    const mid = ((body.mosque_id as string) ?? "").trim();
    const pin = ((body.pin as string) ?? "").trim();
    if (!mid || !pin) {
      return new Response(JSON.stringify({ error: "mosque_id et pin requis" }),
        { status: 400, headers: { ...CORS, "Content-Type": "application/json" } });
    }
    const pinErr = await _validateRemotePin(mid, pin, false);
    if (pinErr) return pinErr;
    return new Response(JSON.stringify({ ok: true }),
      { status: 200, headers: { ...CORS, "Content-Type": "application/json" } });
  }
  if (msgType === "light_watchdog_alert") {
    return handleLightWatchdogAlert(body);
  }

  // ── Routing : custom_message vs webhook config_update ──────────────────────
  const isCustomMsg = msgType === "custom_message";

  let mosque_id:   string;
  let mosque_name: string;
  let notification: Record<string, unknown>;
  let logType: string;
  let logBody: string;

  if (isCustomMsg) {
    mosque_id   = (body.mosque_id   as string) ?? "";
    mosque_name = (body.mosque_name as string) ?? mosque_id;
    const message = ((body.message as string) ?? "").trim();
    const notifType = (body.notif_type as string) === "janaza" ? "janaza" : "message";

    if (!mosque_id || !message) {
      return new Response(
        JSON.stringify({ error: "mosque_id et message requis" }),
        { status: 400, headers: { ...CORS, "Content-Type": "application/json" } }
      );
    }

    console.log(`custom_message (${notifType}) → ${mosque_id} : "${message}"`);

    notification = {
      app_id: ONESIGNAL_APP_ID,
      filters: mosqueFilters(mosque_id),
      headings: { ar: mosque_name, fr: mosque_name, en: mosque_name },
      contents: { ar: message,    fr: message,    en: message },
      data: { type: "custom_message", mosque_id },
      android_visibility: 1,
      priority: 10,
    };
    logType = notifType;
    logBody = message;

  } else {
    const record    = (body.record as Record<string, unknown>) ?? {};
    mosque_id   = (record.mosque_id   as string) ?? "";
    mosque_name = (record.mosque_name as string) ?? mosque_id;

    if (!mosque_id) {
      return new Response(
        JSON.stringify({ error: "mosque_id manquant" }),
        { status: 400, headers: { ...CORS, "Content-Type": "application/json" } }
      );
    }

    console.log(`config_update → ${mosque_id}`);

    notification = {
      app_id: ONESIGNAL_APP_ID,
      filters: mosqueFilters(mosque_id),
      headings: {
        ar: "تحديث أوقات الصلاة",
        fr: "Mise à jour des horaires",
        en: "Prayer times updated",
      },
      contents: { ar: mosque_name, fr: mosque_name, en: mosque_name },
      data: { type: "config_update", mosque_id },
      android_visibility: 1,
      priority: 10,
    };
    logType = "notifier";
    logBody = "";
  }

  let osResponse: Response;
  try {
    osResponse = await fetch("https://onesignal.com/api/v1/notifications", {
      method: "POST",
      headers: {
        "Content-Type":  "application/json",
        "Authorization": `Basic ${ONESIGNAL_API_KEY}`,
      },
      body: JSON.stringify(notification),
    });
  } catch (e) {
    console.error("Fetch OneSignal échoué :", e);
    await _logNotification(mosque_id, logType, logBody, false);
    return new Response("Erreur réseau OneSignal", { status: 502, headers: CORS });
  }

  const osResult = await osResponse.json();
  console.log("OneSignal response:", JSON.stringify(osResult));

  await _logNotification(mosque_id, logType, logBody, osResponse.ok);

  return new Response(JSON.stringify(osResult), {
    headers: { ...CORS, "Content-Type": "application/json" },
    status:  osResponse.ok ? 200 : 500,
  });
});
