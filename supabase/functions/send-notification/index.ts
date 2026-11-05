import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import { SignJWT, importPKCS8 } from "https://deno.land/x/jose@v4.14.4/index.ts"

serve(async (req) => {
  try {
    const webhookSecret = req.headers.get("x-webhook-secret")
    const expectedSecret = Deno.env.get("WEBHOOK_SECRET")
    if (!webhookSecret || webhookSecret !== expectedSecret) {
      return new Response("Unauthorized", { status: 401 })
    }

    const payload = await req.json()
    if (payload.type !== "INSERT" || payload.table !== "notifications") {
      return new Response("Ignored", { status: 200 })
    }

    const record = payload.record
    const userId = record.user_id
    const title = record.title || "GoEvent"
    const message = record.message || ""
    const notifType = record.type || "general"

    const supabaseAdmin = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    )

    // Check user notification preferences
    const { data: prefs } = await supabaseAdmin
      .from("notification_preferences")
      .select("*")
      .eq("user_id", userId)
      .single()

    if (prefs) {
      let enabled = true
      if (notifType.includes("event") && !prefs.events_enabled) enabled = false
      if ((notifType.includes("reward") || notifType.includes("point")) && !prefs.rewards_enabled) enabled = false
      if ((notifType.includes("booking") || notifType.includes("ticket")) && !prefs.bookings_enabled) enabled = false
      if (!enabled) {
        return new Response("Notification disabled by user preference", { status: 200 })
      }
    }

    // Get device tokens for user
    const { data: tokens } = await supabaseAdmin
      .from("device_tokens")
      .select("token")
      .eq("user_id", userId)

    if (!tokens || tokens.length === 0) {
      return new Response("No device tokens found", { status: 200 })
    }

    const projectId = Deno.env.get("FIREBASE_PROJECT_ID")
    const saJsonRaw = Deno.env.get("FIREBASE_SERVICE_ACCOUNT")
    if (!projectId || !saJsonRaw) {
      return new Response("Firebase config missing", { status: 500 })
    }

    const sa = JSON.parse(saJsonRaw)
    const privateKey = await importPKCS8(sa.private_key, "RS256")
    const now = Math.floor(Date.now() / 1000)

    const jwt = await new SignJWT({
      iss: sa.client_email,
      sub: sa.client_email,
      aud: "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    })
      .setProtectedHeader({ alg: "RS256", typ: "JWT" })
      .sign(privateKey)

    const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion: jwt,
      }),
    })

    const tokenData = await tokenRes.json()
    const accessToken = tokenData.access_token
    if (!accessToken) {
      return new Response("Failed to obtain Google OAuth access token", { status: 500 })
    }

    let sentCount = 0
    let removedCount = 0

    for (const row of tokens) {
      const fcmRes = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: row.token,
            notification: { title, body },
            android: { notification: { channel_id: "goevent_updates" } },
          },
        }),
      })

      if (fcmRes.ok) {
        sentCount++
      } else {
        const errText = await fcmRes.text()
        if (fcmRes.status === 404 || errText.includes("UNREGISTERED") || errText.includes("INVALID_ARGUMENT")) {
          await supabaseAdmin.from("device_tokens").delete().eq("token", row.token)
          removedCount++
        }
      }
    }

    return new Response(JSON.stringify({ success: true, sent: sentCount, removed: removedCount }), {
      headers: { "Content-Type": "application/json" },
      status: 200,
    })
  } catch (err) {
    return new Response(err.message, { status: 500 })
  }
})
