/**
 * Utilidad compartida por los módulos del mapa de incidentes.
 *
 * Normaliza textos opcionales provenientes de la API: descarta valores que no
 * sean strings y convierte cadenas vacías o compuestas solo por espacios en
 * `undefined`, para que los consumidores puedan aplicar respaldos legibles.
 */
export function textoOpcional(valor: unknown): string | undefined {
  if (typeof valor !== 'string') {
    return undefined
  }

  const texto = valor.trim()
  return texto === '' ? undefined : texto
}
