// Texto de los avisos que recibe el paciente sobre su cita: la confirma su nutriólogo, se la agenda o se la cancela. Sin dependencias,
// para probarlos con `npm run test:aviso`.

// ponytail: si el teléfono del paciente no manda su zona horaria, se usa la del centro del país.
export const ZONA_POR_DEFECTO = "America/Mexico_City";

const conMayuscula = (t: string) => t.charAt(0).toUpperCase() + t.slice(1);

// La zona que mandó el teléfono, o la de por defecto si falta o no existe.
export function zonaValida(zona: unknown): string {
  if (typeof zona === "string") {
    try {
      new Intl.DateTimeFormat("es-MX", { timeZone: zona });
      return zona;
    } catch {
      // zona desconocida
    }
  }
  return ZONA_POR_DEFECTO;
}

// «Lunes 26 de octubre, 10:00»
export function cuando(inicio: number, zona: string): string {
  const partes = Object.fromEntries(
    new Intl.DateTimeFormat("es-MX", {
      timeZone: zona,
      weekday: "long",
      day: "numeric",
      month: "long",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    })
      .formatToParts(inicio)
      .map((p) => [p.type, p.value]),
  );
  return `${conMayuscula(partes.weekday)} ${partes.day} de ${partes.month}, ${partes.hour}:${partes.minute}`;
}

const consultorio = (direccion?: string) => (direccion ? ` Consultorio: ${direccion}.` : "");

export function avisoDeConfirmacion(nutriologo: string, inicio: number, zona: string, direccion?: string) {
  return { titulo: "Cita confirmada", cuerpo: `${nutriologo} confirmó tu cita: ${cuando(inicio, zona)}.` + consultorio(direccion) };
}

// La cita la creó el nutriólogo: el paciente no la pidió, así que el aviso dice que se la agendaron.
export function avisoDeAgendada(nutriologo: string, inicio: number, zona: string, direccion?: string) {
  return { titulo: "Cita agendada", cuerpo: `${nutriologo} te agendó una cita: ${cuando(inicio, zona)}.` + consultorio(direccion) };
}

// Se cerró el vínculo entre un paciente y su nutriólogo: el aviso es para la otra persona y `quien` es el nombre de quien lo cerró. Con
// `CUENTA_ELIMINADA`, `cuentaDe` dice de quién era la cuenta: la de un paciente «eliminó su cuenta»; la de un nutriólogo «cerró el vínculo».
export function avisoDeBaja(quien: string, por: "PACIENTE" | "NUTRIOLOGO" | "CUENTA_ELIMINADA", cuentaDe?: "PACIENTE" | "NUTRIOLOGO") {
  const cuerpo =
    por === "PACIENTE"
      ? `${quien} dejó de ser tu paciente.`
      : por === "CUENTA_ELIMINADA" && cuentaDe === "PACIENTE"
        ? `${quien} eliminó su cuenta.`
        : `${quien} cerró el vínculo contigo.`;
  return { titulo: "Vínculo cerrado", cuerpo };
}

export function avisoDeCancelacion(nutriologo: string, inicio: number, zona: string) {
  return { titulo: "Cita cancelada", cuerpo: `${nutriologo} canceló tu cita del ${cuando(inicio, zona)}.` };
}
