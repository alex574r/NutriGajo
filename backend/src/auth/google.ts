import { importPKCS8, SignJWT } from "jose";
import type { Env } from "../index";

interface CuentaDeServicio {
  client_email: string;
  private_key: string;
}

let guardado: { token: string; vence: number } | null = null;

// Token OAuth de la cuenta de servicio de Firebase (el secreto FIREBASE_SERVICE_ACCOUNT es el JSON de su clave).
// Con él el Worker lee Firestore sin pasar por las reglas y manda mensajes de FCM. Se guarda en memoria hasta poco antes de vencer.
export async function tokenDeServicio(env: Env): Promise<string> {
  if (guardado && guardado.vence > Date.now() + 60_000) return guardado.token;

  // trim también quita el BOM (﻿) con el que PowerShell guarda los archivos UTF-8: pegado en el secreto rompía el JSON
  const cuenta = JSON.parse((env.FIREBASE_SERVICE_ACCOUNT ?? "").trim()) as CuentaDeServicio;
  const llave = await importPKCS8(cuenta.private_key, "RS256");
  const ahora = Math.floor(Date.now() / 1000);
  const asercion = await new SignJWT({ scope: "https://www.googleapis.com/auth/cloud-platform" })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(cuenta.client_email)
    .setSubject(cuenta.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(ahora)
    .setExpirationTime(ahora + 3600)
    .sign(llave);

  const respuesta = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: asercion }),
  });
  if (!respuesta.ok) throw new Error(`Google respondió ${respuesta.status}`);
  const { access_token, expires_in } = (await respuesta.json()) as { access_token: string; expires_in: number };
  guardado = { token: access_token, vence: Date.now() + expires_in * 1000 };
  return access_token;
}
