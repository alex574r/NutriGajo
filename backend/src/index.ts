import { verificarSesion, verificarSesionConHora } from "./auth/firebase";
import { leerApk, leerUltima } from "./routes/app";
import { avisarMensaje, leerFotoChat, subirFotoChat } from "./routes/chat";
import { avisarConfirmacion } from "./routes/cita";
import { leerCatalogo } from "./routes/catalogo";
import { eliminarCuenta } from "./routes/cuenta";
import { leerClipBanco, subirClipBanco } from "./routes/clipBanco";
import { leerFotoBanco, subirFotoBanco, type Carpeta } from "./routes/fotoBanco";
import { leerFoto, subirFoto, type Tipo } from "./routes/foto";
import { avisarPlan, leerPlan, subirPlan } from "./routes/plan";
import { avisarBaja } from "./routes/vinculo";

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

    // fotos de los bancos globales (ejercicios y platillos): subir exige sesión y se guarda bajo el uid de quien sube; leer es público
    // con el uid del autor (la app lo sabe por el ejercicio o el platillo)
    const carpetas: Record<string, Carpeta> = { ejercicio: "ejercicios", platillo: "platillos" };
    const bancoSubida = pathname.match(/^\/(ejercicio|platillo)\/([A-Za-z0-9_-]{1,128})$/);
    if (bancoSubida && req.method === "PUT") {
      const uid = await verificarSesion(req, env.FIREBASE_PROJECT_ID);
      if (!uid) return new Response("No autorizado", { status: 401 });
      return subirFotoBanco(req, env, uid, bancoSubida[2], carpetas[bancoSubida[1]]);
    }
    const bancoLectura = pathname.match(/^\/(ejercicio|platillo)\/([A-Za-z0-9]{20,128})\/([A-Za-z0-9_-]{1,128})$/);
    if (bancoLectura && req.method === "GET") return leerFotoBanco(req, env, bancoLectura[2], bancoLectura[3], carpetas[bancoLectura[1]]);

    // clips animados de los ejercicios: subir exige sesión (se guarda bajo el uid de quien sube); leer es público y por rangos
    const clipSubida = pathname.match(/^\/ejercicio\/([A-Za-z0-9_-]{1,128})\/clip$/);
    if (clipSubida && req.method === "PUT") {
      const uid = await verificarSesion(req, env.FIREBASE_PROJECT_ID);
      if (!uid) return new Response("No autorizado", { status: 401 });
      return subirClipBanco(req, env, uid, clipSubida[1]);
    }
    const clipLectura = pathname.match(/^\/ejercicio\/([A-Za-z0-9]{20,128})\/([A-Za-z0-9_-]{1,128})\/clip$/);
    if (clipLectura && req.method === "GET") return leerClipBanco(req, env, clipLectura[1], clipLectura[2]);

    // aviso al paciente de que su cita quedó confirmada: lo pide el nutriólogo con su sesión
    if (req.method === "POST" && pathname === "/citas/notificar") {
      const uid = await verificarSesion(req, env.FIREBASE_PROJECT_ID);
      if (!uid) return new Response("No autorizado", { status: 401 });
      return avisarConfirmacion(req, env, uid);
    }

    // borrar la cuenta de quien pide, con una confirmación de identidad reciente (ver routes/cuenta.ts)
    if (req.method === "POST" && pathname === "/cuenta/eliminar") {
      const sesion = await verificarSesionConHora(req, env.FIREBASE_PROJECT_ID);
      if (!sesion) return new Response("No autorizado", { status: 401 });
      return eliminarCuenta(req, env, sesion);
    }

    // aviso a la otra persona de que se cerró un vínculo: lo pide quien lo cerró, con su sesión
    if (req.method === "POST" && pathname === "/vinculos/notificar") {
      const uid = await verificarSesion(req, env.FIREBASE_PROJECT_ID);
      if (!uid) return new Response("No autorizado", { status: 401 });
      return avisarBaja(req, env, uid);
    }

    // planes de alimentación y ejercicio (PDF): siempre con sesión; las reglas de quién puede cada cosa están en routes/plan.ts
    const plan = pathname.match(/^\/plan\/([A-Za-z0-9_-]{1,128})\/([A-Za-z0-9_-]{1,128})$/);
    if (plan && (req.method === "GET" || req.method === "PUT")) {
      const uid = await verificarSesion(req, env.FIREBASE_PROJECT_ID);
      if (!uid) return new Response("No autorizado", { status: 401 });
      return req.method === "PUT" ? subirPlan(req, env, uid, plan[1], plan[2]) : leerPlan(env, uid, plan[1], plan[2]);
    }
    if (req.method === "POST" && pathname === "/planes/notificar") {
      const uid = await verificarSesion(req, env.FIREBASE_PROJECT_ID);
      if (!uid) return new Response("No autorizado", { status: 401 });
      return avisarPlan(req, env, uid);
    }

    // chat entre nutriólogo y paciente: la foto de un mensaje (solo las dos personas del chat) y el aviso a la otra persona
    const chat = pathname.match(/^\/chat\/([A-Za-z0-9_-]{1,128})\/([A-Za-z0-9_-]{1,128})$/);
    if (chat && (req.method === "GET" || req.method === "PUT")) {
      const uid = await verificarSesion(req, env.FIREBASE_PROJECT_ID);
      if (!uid) return new Response("No autorizado", { status: 401 });
      return req.method === "PUT" ? subirFotoChat(req, env, uid, chat[1], chat[2]) : leerFotoChat(env, uid, chat[1], chat[2]);
    }
    if (req.method === "POST" && pathname === "/mensajes/notificar") {
      const uid = await verificarSesion(req, env.FIREBASE_PROJECT_ID);
      if (!uid) return new Response("No autorizado", { status: 401 });
      return avisarMensaje(req, env, uid);
    }

    // el catálogo de ejercicios (fichas, fotos y clips con licencia): solo de lectura y solo con sesión
    const catalogo = pathname.match(/^\/catalogo\/(.+)$/);
    if (catalogo && req.method === "GET") {
      const uid = await verificarSesion(req, env.FIREBASE_PROJECT_ID);
      if (!uid) return new Response("No autorizado", { status: 401 });
      return leerCatalogo(req, env, catalogo[1]);
    }

    // actualizaciones de la app: el APK publicado y su manifiesto. Públicas y de solo lectura: no hay ruta para subirlos (los sube el script de la computadora)
    if (req.method === "GET" && pathname === "/app/ultima") return leerUltima(env);
    const apk = pathname.match(/^\/app\/apk\/([0-9]{1,9})$/);
    if (req.method === "GET" && apk) return leerApk(req, env, apk[1]);

    return new Response("No encontrado", { status: 404 });
  },
};
