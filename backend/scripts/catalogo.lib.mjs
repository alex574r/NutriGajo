// Reglas puras del importador del catálogo (openGym → nutriGajo): cómo una entrada del repo de openGym pasa a la ficha que la app lee.
// Lo prueba `test/catalogoImportar.mjs` (mapeo) y `test/catalogoSubir.mjs` (plan de subida); los CLI son `importar-catalogo.mjs` y `subir-catalogo.mjs`.
import fs from "node:fs";
import path from "node:path";

// La parte del cuerpo de openGym (11) en las siete zonas de la app. Lo que no se conoce cae en cuerpo completo para no romper la importación.
const ZONAS = {
  chest: "PECHO",
  back: "ESPALDA",
  shoulders: "HOMBROS",
  waist: "ABDOMEN",
  "upper arms": "BRAZOS",
  "lower arms": "BRAZOS",
  "upper legs": "PIERNAS",
  "lower legs": "PIERNAS",
  "full body": "CUERPO_COMPLETO",
  cardio: "CUERPO_COMPLETO",
  neck: "CUERPO_COMPLETO",
};
export const zonaDe = (bodyPart) => ZONAS[bodyPart] ?? "CUERPO_COMPLETO";

// El equipo de openGym (35 valores) en los cinco de la app; lo demás (cables, máquinas, pelotas…) y lo desconocido, en máquina.
const SIN_EQUIPO = new Set(["body weight", "assisted"]);
const MANCUERNAS = new Set(["dumbbell", "kettlebell"]);
const BANDAS = new Set(["band", "resistance band"]);
const BARRA = new Set(["barbell", "ez barbell", "olympic barbell", "trap bar", "landmine", "weight plate"]);
export function equipoDe(equipment) {
  if (SIN_EQUIPO.has(equipment)) return "SIN_EQUIPO";
  if (MANCUERNAS.has(equipment)) return "MANCUERNAS";
  if (BANDAS.has(equipment)) return "BANDAS";
  if (BARRA.has(equipment)) return "BARRA";
  return "MAQUINA";
}

// La categoría de openGym en los cuatro tipos de la app; lo demás (fuerza, calistenia, pliometría, pilates…) es fuerza.
export function tipoDe(category) {
  if (category === "cardio") return "CARDIO";
  if (category === "stretching" || category === "mobility") return "FLEXIBILIDAD";
  if (category === "yoga") return "YOGA";
  return "FUERZA";
}

// Calorías por minuto por tipo: openGym no trae calorías, así que se estiman (lo mismo que ya hace la app con los ejercicios de entrenadores sin dato).
const KCAL_POR_MINUTO = { FUERZA: 5, CARDIO: 9, FLEXIBILIDAD: 3, YOGA: 3 };
export const kcalPorMinuto = (tipo) => KCAL_POR_MINUTO[tipo];

export const capitalizar = (texto) => {
  const t = (texto ?? "").trim();
  return t.charAt(0).toUpperCase() + t.slice(1);
};

const DURACION_MIN = 5;

// La ficha de una entrada de openGym, o null si es una figura alterna (`variantOf`: la versión femenina de otro ejercicio, que no es uno aparte).
// `es` es `i18n/es.json` (id → { name, instructions? }): el título sale de ahí y, si no está, del inglés; los pasos salen del español si existen.
export function fichaDe(entrada, es = {}) {
  if (entrada.variantOf) return null;
  const traduccion = es[entrada.id] ?? {};
  const tipo = tipoDe(entrada.category);
  const pasosFuente = Array.isArray(traduccion.instructions) && traduccion.instructions.length ? traduccion.instructions : entrada.instructions ?? [];
  return {
    id: entrada.id,
    titulo: capitalizar(traduccion.name || entrada.name),
    zona: zonaDe(entrada.bodyPart),
    tipo,
    equipo: equipoDe(entrada.equipment),
    musculos: [entrada.target, ...(entrada.secondaryMuscles ?? [])].filter(Boolean),
    descripcion: (entrada.description ?? "").trim(),
    pasos: pasosFuente.map((p) => String(p).trim()).filter(Boolean),
    duracionMin: DURACION_MIN,
    kcal: DURACION_MIN * kcalPorMinuto(tipo),
  };
}

// El índice que baja la app una vez: [id, título, zona, tipo, equipo], por título (en español) y, a igual título, por id.
export const crearIndice = (fichas) =>
  fichas
    .map((f) => [f.id, f.titulo, f.zona, f.tipo, f.equipo])
    .sort((a, b) => a[1].localeCompare(b[1], "es") || a[0].localeCompare(b[0]));

// Lo que se sube a R2 de la carpeta que escribe el importador: solo las cinco formas del catálogo (version.json, indice-N.json, ficha/*.json,
// foto/*.webp, clip/*.mp4); lo demás (notas, el registro de lo ya subido…) se ignora. Orden: primero fichas, fotos y clips, luego el índice y
// **al final version.json**: con él el catálogo «existe» para los teléfonos, así una subida a medias nunca lo deja a la vista.
const FORMAS_DE_SUBIDA = [
  { patron: /^ficha\/[0-9]{4,5}\.json$/, tipo: "application/json", orden: 0 },
  { patron: /^foto\/[0-9]{4,5}\.webp$/, tipo: "image/webp", orden: 0 },
  { patron: /^clip\/[0-9]{4,5}\.mp4$/, tipo: "video/mp4", orden: 0 },
  { patron: /^indice-[0-9]{1,9}\.json$/, tipo: "application/json", orden: 1 },
  { patron: /^version\.json$/, tipo: "application/json", orden: 2 },
];
export function planDeSubida(carpeta) {
  const encontrados = [];
  const recorrer = (dir, prefijo) => {
    for (const entrada of fs.readdirSync(dir, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
      const relativa = prefijo ? `${prefijo}/${entrada.name}` : entrada.name;
      if (entrada.isDirectory()) recorrer(path.join(dir, entrada.name), relativa);
      else {
        const forma = FORMAS_DE_SUBIDA.find((f) => f.patron.test(relativa));
        if (forma) encontrados.push({ archivo: path.join(dir, entrada.name), clave: `catalogo/${relativa}`, tipo: forma.tipo, orden: forma.orden });
      }
    }
  };
  recorrer(carpeta, "");
  return encontrados.sort((a, b) => a.orden - b.orden).map(({ archivo, clave, tipo }) => ({ archivo, clave, tipo }));
}
