// ── notify-mosque — Supabase Edge Function ────────────────────────────────────
// Deux modes d'utilisation :
//
//   1. Webhook Supabase (Database → mosques UPDATE) :
//      Body = { record: { mosque_id, mosque_name, ... } }
//      → notif "Mise à jour des horaires"
//
//   2. Appel direct (message personnalisé de l'admin) :
//      Body = { type: "custom_message", mosque_id, mosque_name, message }
//      → notif avec le message de l'admin en titre/corps
//
// Ciblage OneSignal (abonnement multi-mosquée cumulatif) :
//   - tag mosque_sub_<id_sanitizé> EXISTS — posé par addMosqueSubscriptionTag
//     pour CHAQUE mosquée de l'historique de l'utilisateur (non mise en
//     sourdine), même si une autre mosquée est active actuellement.
//   - Le legacy "mosque_id = <id>" (posé par setMosqueId) n'est PLUS utilisé
//     pour le ciblage (retiré du filtre — phase de test, pas encore en prod) ;
//     il reste posé côté client à titre informatif uniquement.
//   - Un opt-out par mosquée (panneau "Mes notifications" côté app) retire le
//     tag mosque_sub_<id> via removeMosqueSubscriptionTag, ce qui exclut
//     l'appareil de cette notification sans toucher aux autres mosquées.
//
// Variables d'environnement (Supabase Dashboard → Edge Functions → Secrets) :
//   ONESIGNAL_APP_ID   — UUID de l'app OneSignal
//   ONESIGNAL_API_KEY  — REST API Key OneSignal
// ─────────────────────────────────────────────────────────────────────────────

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const ONESIGNAL_APP_ID  = Deno.env.get("ONESIGNAL_APP_ID")  ?? "a7656f67-9573-4593-97a8-871ac6550731";
const ONESIGNAL_API_KEY = Deno.env.get("ONESIGNAL_API_KEY") ?? "";

const CORS = {
  "Access-Control-Allow-Origin":  "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

// Sanitisation du mosque_id pour construire la clé de tag mosque_sub_<id>.
// DOIT être identique à celle utilisée côté Android (MobileJsBridge.kt
// addMosqueSubscriptionTag) pour que les filtres OneSignal correspondent.
function sanitizeMosqueId(id: string): string {
  return id.replace(/[^a-zA-Z0-9_]/g, "_");
}

// Filtre OneSignal : cible uniquement les appareils ayant le tag
// mosque_sub_<id> (posé pour chaque mosquée de l'historique de l'utilisateur,
// retiré si l'utilisateur coupe les notifications de cette mosquée).
// NOTE : le legacy "mosque_id = <id>" a été retiré du ciblage (phase de test,
// pas encore en prod) — mosque_id reste posé côté client à titre informatif
// uniquement, il ne conditionne plus la réception des notifications.
function mosqueFilters(mosque_id: string) {
  return [
    { field: "tag", key: `mosque_sub_${sanitizeMosqueId(mosque_id)}`, relation: "exists" },
  ];
}

serve(async (req: Request) => {

  // Preflight CORS (envoyé par le navigateur avant chaque POST cross-origin)
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

  // ── Routing : custom_message vs webhook config_update ──────────────────────
  const isCustomMsg = (body.type as string) === "custom_message";

  let mosque_id:   string;
  let mosque_name: string;
  let notification: Record<string, unknown>;

  if (isCustomMsg) {
    // ── Mode 2 : message personnalisé envoyé directement par l'admin ──────────
    mosque_id   = (body.mosque_id   as string) ?? "";
    mosque_name = (body.mosque_name as string) ?? mosque_id;
    const message = ((body.message as string) ?? "").trim();

    if (!mosque_id || !message) {
      return new Response(
        JSON.stringify({ error: "mosque_id et message requis" }),
        { status: 400, headers: { ...CORS, "Content-Type": "application/json" } }
      );
    }

    console.log(`custom_message → ${mosque_id} : "${message}"`);

    notification = {
      app_id: ONESIGNAL_APP_ID,
      filters: mosqueFilters(mosque_id),
      // Titre = nom de la mosquée, corps = message de l'admin
      headings: { ar: mosque_name, fr: mosque_name, en: mosque_name },
      contents: { ar: message,    fr: message,    en: message },
      data: { type: "custom_message", mosque_id },
      android_visibility: 1,
      priority: 10,
    };

  } else {
    // ── Mode 1 : webhook Supabase → mise à jour des horaires ──────────────────
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
  }

  // ── Appel API OneSignal ─────────────────────────────────────────────────────
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
    return new Response("Erreur réseau OneSignal", { status: 502, headers: CORS });
  }

  const osResult = await osResponse.json();
  console.log("OneSignal response:", JSON.stringify(osResult));

  return new Response(JSON.stringify(osResult), {
    headers: { ...CORS, "Content-Type": "application/json" },
    status:  osResponse.ok ? 200 : 500,
  });
});
