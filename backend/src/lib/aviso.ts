// Texto del aviso que recibe el paciente cuando su nutriólogo confirma la cita. Sin dependencias, para probarlo con `npm test`.

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

export function avisoDeConfirmacion(nutriologo: string, inicio: number, zona: string, direccion?: string) {
  return {
    titulo: "Cita confirmada",
    cuerpo: `${nutriologo} confirmó tu cita: ${cuando(inicio, zona)}.` + (direccion ? ` Consultorio: ${direccion}.` : ""),
  };
}
