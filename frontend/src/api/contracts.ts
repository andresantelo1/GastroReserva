import { ApiError } from './httpClient.ts'
export function contratoInvalido() { return new ApiError(200, 'La respuesta no coincide con el contrato de GastroReserva. Revisá la URL y la versión del backend.', 'INVALID_RESPONSE') }
export function objeto(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw contratoInvalido()
  return value as Record<string, unknown>
}
export function texto(value: unknown): string { if (typeof value !== 'string') throw contratoInvalido(); return value }
export function numero(value: unknown): number { if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 1) throw contratoInvalido(); return value }
export function booleano(value: unknown): boolean { if (typeof value !== 'boolean') throw contratoInvalido(); return value }
export function nullableTexto(value: unknown): string | null { return value === null ? null : texto(value) }
export function lista<T>(value: unknown, parse: (item: unknown) => T): T[] { if (!Array.isArray(value)) throw contratoInvalido(); return value.map(parse) }

