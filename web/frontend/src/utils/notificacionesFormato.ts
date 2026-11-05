export function texto(valor: unknown): string {
  return typeof valor === 'string' ? valor.trim() : ''
}

export function interpretarFecha(valor: unknown): Date | null {
  const contenido = texto(valor)

  if (!contenido) return null

  const fecha = new Date(contenido)

  return Number.isNaN(fecha.getTime()) ? null : fecha
}

export function mostrarFecha(fecha: Date): string {
  return new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fecha)
}