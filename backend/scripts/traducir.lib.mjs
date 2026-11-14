// Reglas puras de la traducción del catálogo (openGym → español): cómo se arman los pedidos a la Batches API de Claude y cómo se valida lo que
// vuelve. Lo prueba `test/catalogoTraducir.mjs`; el CLI es `traducir-catalogo.mjs`. Solo viaja texto (título como contexto, descripción y pasos).

export const TAMANO_LOTE = 15;

// USD por millón de tokens, precio normal (la Batches API cobra la mitad).
export const PRECIOS = {
  "claude-opus-5-5": { entrada: 4, salida: 20 },
  "claude-sonnet-5-5": { entrada: 2, salida: 10 },
  "claude-haiku-5-5": { entrada: 0.1, salida: 0.5 },
};

// Lo que costaría un lote en la Batches API; null si no se conoce el precio del modelo.
export function costoDeLote(tokensEntrada, tokensSalida, modelo) {
  const p = PRECIOS[modelo];
  return p ? (tokensEntrada * p.entrada + tokensSalida * p.salida) / 1e6 / 2 : null;
}

// Lo que se envía de cada ficha. Si sus pasos ya están en español (`conPasosEnEspanol`), `pasos` va vacío: no se envían ni se tocan.
export const itemsPorTraducir = (fichas, conPasosEnEspanol) =>
  fichas.map((f) => ({ id: f.id, titulo: f.titulo, descripcion: f.descripcion, pasos: conPasosEnEspanol.has(f.id) ? [] : f.pasos }));

export function crearLotes(items, tamano = TAMANO_LOTE) {
  const lotes = [];
  for (let i = 0; i < items.length; i += tamano) {
    lotes.push({ id: `lote-${String(lotes.length + 1).padStart(4, "0")}`, items: items.slice(i, i + tamano) });
  }
  return lotes;
}

export const tokensAprox = (items) => Math.ceil(JSON.stringify(items).length / 4);

export const ESQUEMA_SALIDA = {
  type: "object",
  properties: {
    traducciones: {
      type: "array",
      items: {
        type: "object",
        properties: { id: { type: "string" }, descripcion: { type: "string" }, pasos: { type: "array", items: { type: "string" } } },
        required: ["id", "descripcion", "pasos"],
        additionalProperties: false,
      },
    },
  },
  required: ["traducciones"],
  additionalProperties: false,
};

export const INSTRUCCIONES = `Traduces al español fichas de ejercicios de gimnasio para una app de nutrición y entrenamiento.

Recibes un arreglo JSON de ejercicios: { id, titulo, descripcion, pasos }. Devuelves { "traducciones": [...] } con un elemento por ejercicio recibido: { id, descripcion, pasos }.

Reglas:
- Traduce "descripcion" y cada elemento de "pasos" del inglés al español neutro de México, en imperativo de "tú" ("Túmbate", "Agarra", "Baja").
- Devuelve exactamente el mismo id, el mismo número de pasos y en el mismo orden. No agregues, quites ni unas pasos. Si "pasos" viene vacío, devuelve "pasos": [].
- "titulo" ya está en español: úsalo solo como contexto para que tus términos coincidan con él. No lo traduzcas ni lo devuelvas.
- Conserva números, unidades y signos. Una frase por paso, directa y sin adornos, como el original.
- Glosario: spotter → ayudante; rack → soporte; bench → banco; barbell → barra; dumbbell → mancuerna; kettlebell → pesa rusa; resistance band → banda elástica; cable → polea; lat pulldown → jalón al pecho; row → remo; lunge → zancada; squat → sentadilla; deadlift → peso muerto; plank → plancha; crunch → abdominal; grip → agarre.`;

// La petición de un lote para `client.messages.batches.create`. Traducir es mecánico: esfuerzo bajo.
export const pedidoDeLote = (lote, modelo) => ({
  custom_id: lote.id,
  params: {
    model: modelo,
    max_tokens: 16000,
    system: INSTRUCCIONES,
    messages: [{ role: "user", content: JSON.stringify(lote.items) }],
    output_config: { effort: "low", format: { type: "json_schema", schema: ESQUEMA_SALIDA } },
  },
});

// El texto de un archivo que escribió un subagente de Claude Code, con la forma de un renglón de `batches.results` para validarlo igual.
export const resultadoDeTexto = (idLote, texto) => ({
  custom_id: idLote,
  result: { type: "succeeded", message: { stop_reason: "end_turn", content: [{ type: "text", text: texto }] } },
});

// Un renglón de `batches.results` convertido en lo traducido y lo que falló, por ejercicio. Un lote que no se completó, se cortó, se rechazó o no se
// lee falla entero; si no, cada ejercicio se acepta solo si trae su descripción y exactamente los pasos que se enviaron (sin vacíos).
export function leerResultadoDeLote(resultado, lote) {
  const traducidas = new Map();
  const todosFallidos = () => ({ traducidas, fallidos: lote.items.map((i) => i.id) });
  if (resultado.result?.type !== "succeeded") return todosFallidos();
  const mensaje = resultado.result.message;
  if (mensaje?.stop_reason !== "end_turn") return todosFallidos();
  const texto = mensaje.content?.find((b) => b.type === "text")?.text;
  let filas;
  try {
    filas = JSON.parse(texto ?? "").traducciones;
  } catch {
    return todosFallidos();
  }
  if (!Array.isArray(filas)) return todosFallidos();
  const pedidos = new Map(lote.items.map((i) => [i.id, i]));
  for (const fila of filas) {
    const item = pedidos.get(fila?.id);
    if (!item || traducidas.has(item.id)) continue;
    const descripcionBien = typeof fila.descripcion === "string" && fila.descripcion.trim() !== "";
    const pasosBien = Array.isArray(fila.pasos) && fila.pasos.length === item.pasos.length && fila.pasos.every((p) => typeof p === "string" && p.trim() !== "");
    if (descripcionBien && pasosBien) traducidas.set(item.id, { descripcion: fila.descripcion, pasos: fila.pasos });
  }
  return { traducidas, fallidos: lote.items.map((i) => i.id).filter((id) => !traducidas.has(id)) };
}

// La ficha con su descripción y sus pasos traducidos; lo demás queda igual. Sin traducción la ficha no cambia; si no se enviaron pasos (ya estaban en
// español), se conservan los suyos.
export const aplicarTraduccion = (ficha, traduccion) =>
  traduccion
    ? { ...ficha, descripcion: traduccion.descripcion.trim(), pasos: traduccion.pasos.length ? traduccion.pasos.map((p) => p.trim()) : ficha.pasos }
    : ficha;
