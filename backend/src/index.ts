import { verificarSesion } from "./auth/firebase";
import { avisarConfirmacion } from "./routes/cita";
import { leerFoto, subirFoto, type Tipo } from "./routes/foto";

export interface Env {
  FOTOS: R2Bucket;
  FIREBASE_PROJECT_ID: string;
  // JSON de la clave de la cuenta de servicio de Firebase (`wrangler secret put FIREBASE_SERVICE_ACCOUNT`); sin ella los avisos responden 503
  FIREBASE_SERVICE_ACCOUNT?: string;
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    const { pathname } = new URL(req.url);

    // /portada y /avatar: `GET /{tipo}/{uid}` es público (la ve quien tenga la dirección, que lleva el uid y la versión);
    // `PUT /{tipo}` exige una sesión de Firebase y solo escribe en la clave de su dueño
    const ruta = pathname.match(/^\/(portada|avatar)(?:\/([A-Za-z0-9]{20,128}))?$/);
    if (ruta) {
      const tipo = ruta[1] as Tipo;
      const dueno = ruta[2];
      if (req.method === "GET" && dueno) return leerFoto(req, env, dueno, tipo);
      if (req.method === "PUT" && !dueno) {
        const uid = await verificarSesion(req, env.FIREBASE_PROJECT_ID);
        if (!uid) return new Response("No autorizado", { status: 401 });
        return subirFoto(req, env, uid, tipo);
      }
    }

    // aviso al paciente de que su cita quedó confirmada: lo pide el nutriólogo con su sesión
    if (req.method === "POST" && pathname === "/citas/notificar") {
      const uid = await verificarSesion(req, env.FIREBASE_PROJECT_ID);
      if (!uid) return new Response("No autorizado", { status: 401 });
      return avisarConfirmacion(req, env, uid);
    }

    return new Response("No encontrado", { status: 404 });
  },
};
