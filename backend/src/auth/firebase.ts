import { createRemoteJWKSet, jwtVerify } from "jose";

// Claves públicas con las que Firebase firma los ID tokens; jose las descarga y las guarda en caché.
const claves = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

// uid del token `Authorization: Bearer <ID token de Firebase Auth>` y la hora (en segundos) en que esa persona confirmó su identidad por
// última vez (`auth_time`: sube al iniciar sesión y al volver a confirmar con la contraseña o el enlace del correo), o null si el token falta,
// caducó o no es de este proyecto.
export async function verificarSesionConHora(req: Request, proyecto: string): Promise<{ uid: string; authTime: number } | null> {
  const token = req.headers.get("Authorization")?.match(/^Bearer (.+)$/)?.[1];
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, claves, {
      issuer: `https://securetoken.google.com/${proyecto}`,
      audience: proyecto,
    });
    if (!payload.sub) return null;
    return { uid: payload.sub, authTime: typeof payload.auth_time === "number" ? payload.auth_time : 0 };
  } catch {
    return null;
  }
}

// uid del token `Authorization: Bearer <ID token de Firebase Auth>`, o null si falta, caducó o no es de este proyecto.
export async function verificarSesion(req: Request, proyecto: string): Promise<string | null> {
  return (await verificarSesionConHora(req, proyecto))?.uid ?? null;
}
