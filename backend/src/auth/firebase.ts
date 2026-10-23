import { createRemoteJWKSet, jwtVerify } from "jose";

// Claves públicas con las que Firebase firma los ID tokens; jose las descarga y las guarda en caché.
const claves = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

// uid del token `Authorization: Bearer <ID token de Firebase Auth>`, o null si falta, caducó o no es de este proyecto.
export async function verificarSesion(req: Request, proyecto: string): Promise<string | null> {
  const token = req.headers.get("Authorization")?.match(/^Bearer (.+)$/)?.[1];
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, claves, {
      issuer: `https://securetoken.google.com/${proyecto}`,
      audience: proyecto,
    });
    return payload.sub || null;
  } catch {
    return null;
  }
}
