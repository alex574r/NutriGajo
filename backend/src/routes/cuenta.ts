import { tokenDeServicio } from "../auth/google";
import type { Env } from "../index";
import { avisoDeBaja } from "../lib/aviso";
import { esReciente, pasosDeBorrado, type Paso, type Rol } from "../lib/borrado";
import { actualizar, borrar, borrarColeccion, borrarCuentaDeAuth, consultar, enviarPush, ID, leer, texto } from "../lib/firestore";

interface Contexto {
  env: Env;
  token: string;
  uid: string;
  rol: Rol;
  // el nombre de la cuenta que se borra, para los avisos
  nombre: string;
  // milisegundos
  ahora: number;
}

const entero = (n: number) => ({ integerValue: String(n) });

// Borra la cuenta de quien lo pide: `POST /cuenta/eliminar` con su sesión de Firebase, que tiene que haberse confirmado hace cinco minutos
// o menos (en la app, con el enlace que Firebase manda al correo de la cuenta). El Worker hace con su cuenta de servicio lo que las reglas
// no dejan hacer desde la app: cerrar los vínculos (no se borra el expediente, los planes ni el chat: se conservan cerrados, solo para leer),
// quitar citas, consultorio y lo demás del usuario, y al final su perfil y su cuenta de Auth. Los pasos de cada rol están en lib/borrado.ts.
// Se puede repetir: cada paso tolera lo que ya se hizo y, si el perfil ya no existe, solo falta la cuenta. Si un paso falla se contesta 502
// sin tocar lo que sigue (ni el perfil ni la cuenta), para reintentar.
// ponytail: un aviso por vínculo activo que se cierra; las citas de un paciente sin vínculo con un nutriólogo que se va no avisan, solo se cancelan.
export async function eliminarCuenta(_req: Request, env: Env, sesion: { uid: string; authTime: number }): Promise<Response> {
  if (!esReciente(sesion.authTime, Math.floor(Date.now() / 1000))) return new Response("Confirma tu identidad otra vez", { status: 401 });
  if (!env.FIREBASE_SERVICE_ACCOUNT) return new Response("Borrado sin configurar", { status: 503 });
  if (!ID.test(sesion.uid)) return new Response("Cuenta no válida", { status: 400 });

  try {
    const token = await tokenDeServicio(env);
    const perfil = await leer(env, token, `usuarios/${sesion.uid}`);
    const rol = texto(perfil, "rol") as Rol | undefined;
    if (rol === "ADMIN") return new Response("Esta cuenta no se borra desde la app", { status: 403 });
    // sin perfil: un intento anterior ya borró lo de adentro y solo falta la cuenta de Auth
    const pasos: Paso[] = perfil && rol ? pasosDeBorrado(rol) : ["auth"];
    const contexto: Contexto = { env, token, uid: sesion.uid, rol: rol ?? "PACIENTE", nombre: texto(perfil, "nombre") ?? "Alguien", ahora: Date.now() };
    for (const paso of pasos) await PASOS[paso](contexto);
    return Response.json({ borrado: true });
  } catch (e) {
    if (e instanceof Error && e.message === "Esta cuenta no se borra desde la app") return new Response(e.message, { status: 403 });
    return new Response("No se pudo eliminar la cuenta", { status: 502 });
  }
}

const PASOS: Record<Paso, (c: Contexto) => Promise<void>> = {
  "cerrar-vinculos": cerrarVinculos,
  citas,
  consultorio: async (c) => {
    await borrarColeccion(c.env, c.token, `directorio/${c.uid}/ocupados`);
    await borrar(c.env, c.token, `directorio/${c.uid}`);
  },
  codigos: async (c) => {
    // solo los que nadie ha canjeado: los usados son parte del registro de quien los canjeó
    for (const { ruta, doc } of await consultar(c.env, c.token, "codigosVinculacion", "nutriologoId", c.uid)) {
      if (doc.fields?.usado?.booleanValue !== true) await borrar(c.env, c.token, ruta);
    }
  },
  cedula: async (c) => {
    // la reserva de la cédula (una cédula, una cuenta) se libera para que otra cuenta pueda usarla
    const cedula = texto(await leer(c.env, c.token, `verificaciones/${c.uid}`), "cedula");
    if (cedula && ID.test(cedula)) await borrar(c.env, c.token, `cedulas/${cedula}`);
  },
  verificacion: (c) => borrar(c.env, c.token, `verificaciones/${c.uid}`),
  autor,
  comentarios: async (c) => {
    for (const { ruta } of await consultar(c.env, c.token, "comentarios", "usuarioId", c.uid)) await borrar(c.env, c.token, ruta);
  },
  subcolecciones: async (c) => {
    for (const nombre of ["entrenamientos", "carreras", "modoCarrera", "banco"]) await borrarColeccion(c.env, c.token, `usuarios/${c.uid}/${nombre}`);
  },
  "r2-perfil": async (c) => {
    await c.env.FOTOS.delete([`avatares/${c.uid}`, `portadas/${c.uid}`]);
  },
  usuario: (c) => borrar(c.env, c.token, `usuarios/${c.uid}`),
  auth: (c) => borrarCuentaDeAuth(c.env, c.token, c.uid),
};

// Cierra los vínculos (`pacientes/{id}`) de la cuenta: los de un paciente por su `usuarioId` y los de un nutriólogo por su `nutriologoId`.
// Nada se borra: queda `activo = false` con `cerradoPor = CUENTA_ELIMINADA`. Al registro de un paciente que se va se le quita lo que lo
// ata a una cuenta que ya no existe (`usuarioId`, `correoVinculacion`, `codigoVinculacion`) y se queda con el nombre. Solo los vínculos
// que seguían activos avisan a la otra parte, así un reintento no repite avisos.
async function cerrarVinculos(c: Contexto) {
  const delPaciente = c.rol === "PACIENTE";
  for (const { ruta, doc } of await consultar(c.env, c.token, "pacientes", delPaciente ? "usuarioId" : "nutriologoId", c.uid)) {
    const estabaActivo = doc.fields?.activo?.booleanValue !== false;
    if (estabaActivo) {
      await actualizar(c.env, c.token, ruta, ["activo", "cerradoEn", "cerradoPor", "ultimaModificacion"], {
        activo: { booleanValue: false },
        cerradoEn: entero(c.ahora),
        cerradoPor: { stringValue: "CUENTA_ELIMINADA" },
        ultimaModificacion: entero(c.ahora),
      });
    }
    if (delPaciente) await actualizar(c.env, c.token, ruta, ["usuarioId", "correoVinculacion", "codigoVinculacion"], {});
    if (estabaActivo) await avisarCierre(c, delPaciente ? texto(doc, "nutriologoId") : texto(doc, "usuarioId"));
  }
}

// El aviso es de cortesía: si no hay dispositivo o FCM falla, el vínculo ya quedó cerrado y el borrado sigue.
async function avisarCierre(c: Contexto, destino: string | undefined) {
  if (!destino || !ID.test(destino)) return;
  try {
    const dispositivo = texto(await leer(c.env, c.token, `usuarios/${destino}`), "fcmToken");
    if (!dispositivo) return;
    const aviso = avisoDeBaja(c.nombre, "CUENTA_ELIMINADA", c.rol === "PACIENTE" ? "PACIENTE" : "NUTRIOLOGO");
    await enviarPush(c.env, c.token, { dispositivo, canal: "citas", titulo: aviso.titulo, cuerpo: aviso.cuerpo, datos: { tipo: "VINCULO_CERRADO" } });
  } catch {
    // sin aviso
  }
}

// Citas. Las de un paciente se borran con su cuenta, con la hora que ocupaban (si es de esa cita: una hora que otro tomó después no se
// toca) y su candado. Las de un nutriólogo que se va se cierran —pendiente: rechazada; confirmada: cancelada; solo las que aún no llegan—
// y se libera el candado del paciente que las pidió.
async function citas(c: Contexto) {
  if (c.rol === "PACIENTE") {
    for (const { ruta, doc } of await consultar(c.env, c.token, "citas", "usuarioId", c.uid)) {
      const nutriologo = texto(doc, "nutriologoId");
      const inicio = doc.fields?.inicio?.integerValue;
      if (nutriologo && ID.test(nutriologo) && inicio) {
        const hora = `directorio/${nutriologo}/ocupados/${inicio}`;
        if (texto(await leer(c.env, c.token, hora), "citaId") === ruta.split("/")[1]) await borrar(c.env, c.token, hora);
      }
      await borrar(c.env, c.token, ruta);
    }
    await borrar(c.env, c.token, `citasActivas/${c.uid}`);
    return;
  }
  for (const { ruta, doc } of await consultar(c.env, c.token, "citas", "nutriologoId", c.uid)) {
    const estado = texto(doc, "estado");
    const inicio = Number(doc.fields?.inicio?.integerValue);
    if ((estado !== "PENDIENTE" && estado !== "CONFIRMADA") || !(inicio > c.ahora)) continue;
    await actualizar(c.env, c.token, ruta, ["estado", "actualizadaEn"], {
      estado: { stringValue: estado === "PENDIENTE" ? "RECHAZADA" : "CANCELADA" },
      actualizadaEn: entero(c.ahora),
    });
    const paciente = texto(doc, "usuarioId");
    if (paciente && ID.test(paciente)) {
      const candado = `citasActivas/${paciente}`;
      if (texto(await leer(c.env, c.token, candado), "citaId") === ruta.split("/")[1]) await borrar(c.env, c.token, candado);
    }
  }
}

// Lo que la cuenta publicó en los bancos globales (ejercicios y platillos). Lo publicado se queda, con autor «Cuenta eliminada» y el mismo
// `autorId` (las fotos y clips en R2 viven bajo el uid del autor). Lo que no se publicó (borrador, en revisión, rechazado o retirado) se
// borra con sus archivos.
// ponytail: una sola página de archivos por documento (hasta 1000); sobra para fotos y un clip con versiones.
async function autor(c: Contexto) {
  for (const coleccion of ["ejercicios", "platillos"]) {
    for (const { ruta, doc } of await consultar(c.env, c.token, coleccion, "autorId", c.uid)) {
      if (texto(doc, "estado") === "PUBLICADO") {
        await actualizar(c.env, c.token, ruta, ["autorNombre"], { autorNombre: { stringValue: "Cuenta eliminada" } });
        continue;
      }
      const id = ruta.split("/")[1];
      const base = `${coleccion}/${c.uid}/${id}`;
      // las versiones (`{base}/{versión}` y `{base}/clip-{versión}`) y lo subido antes de que hubiera versiones (`{base}`)
      const claves = (await c.env.FOTOS.list({ prefix: `${base}/` })).objects.map((o) => o.key);
      await c.env.FOTOS.delete([base, ...claves]);
      await borrar(c.env, c.token, ruta);
    }
  }
}
