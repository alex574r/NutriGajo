// Lo que decide, sin red, el borrado de una cuenta (`POST /cuenta/eliminar`): cuánto vale la confirmación de identidad y qué pasos se hacen
// según el rol. Sin dependencias, para probarlo con `npm run test:cuenta`.

export type Rol = "PACIENTE" | "NUTRIOLOGO" | "ENTRENADOR" | "ADMIN";

// La persona tiene que haber confirmado su identidad (contraseña o enlace del correo) hace cinco minutos o menos.
export const VENTANA_SEGUNDOS = 300;
// el reloj del teléfono y el de Firebase pueden diferir unos segundos
const TOLERANCIA_SEGUNDOS = 60;

export function esReciente(authTime: number, ahoraSeg: number): boolean {
  const antiguedad = ahoraSeg - authTime;
  return antiguedad >= -TOLERANCIA_SEGUNDOS && antiguedad <= VENTANA_SEGUNDOS;
}

// Los pasos del borrado, en orden: primero lo de adentro y al final el perfil (`usuario`) y la cuenta de Auth (`auth`). Así, si algo falla
// a la mitad, nada queda a medias sin remedio y el mismo pedido se puede repetir.
export type Paso =
  | "cerrar-vinculos"
  | "citas"
  | "consultorio"
  | "codigos"
  | "cedula"
  | "verificacion"
  | "autor"
  | "comentarios"
  | "subcolecciones"
  | "r2-perfil"
  | "usuario"
  | "auth";

export function pasosDeBorrado(rol: Rol): Paso[] {
  switch (rol) {
    case "PACIENTE":
      return ["cerrar-vinculos", "citas", "comentarios", "subcolecciones", "r2-perfil", "usuario", "auth"];
    case "NUTRIOLOGO":
      // la cédula se libera antes de borrar la solicitud, que es de donde sale su número
      return ["cerrar-vinculos", "citas", "consultorio", "codigos", "cedula", "verificacion", "autor", "comentarios", "subcolecciones", "r2-perfil", "usuario", "auth"];
    case "ENTRENADOR":
      return ["verificacion", "autor", "comentarios", "subcolecciones", "r2-perfil", "usuario", "auth"];
    default:
      throw new Error("Esta cuenta no se borra desde la app");
  }
}
