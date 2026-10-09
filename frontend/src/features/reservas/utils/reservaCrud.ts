import type { Cliente } from '../../clientes/models/Cliente.ts'
import type { Reserva } from '../models/Reserva.ts'
import type { ReservaClienteFormData, ReservaClienteRequest } from '../types/ReservaClienteRequest.ts'

export const puedeCorregir = (r: Reserva) => r.estado === 'SOLICITADA'
export const puedeCancelar = (r: Reserva) => r.estado === 'SOLICITADA' || r.estado === 'CONFIRMADA'
export const formularioDeReserva = (r: Reserva): ReservaClienteFormData => ({ clienteId: String(r.clienteId), observaciones: r.observaciones ?? '', motivo: '' })
export function validarCorreccion(data: ReservaClienteFormData, clientes: Cliente[]) {
  const errors: Partial<Record<keyof ReservaClienteFormData, string>> = {}
  const id = Number(data.clienteId)
  if (!data.clienteId.trim() || !Number.isSafeInteger(id) || id <= 0 || !clientes.some((c) => c.id === id && c.activo)) errors.clienteId = 'Seleccioná un cliente válido y activo.'
  if (data.observaciones.length > 500) errors.observaciones = 'Máximo 500 caracteres.'
  if (!data.motivo.trim() || data.motivo.length > 500) errors.motivo = 'Indicá un motivo de 1 a 500 caracteres.'
  return errors
}
export function prepararCorreccion(data: ReservaClienteFormData, version: number): ReservaClienteRequest {
  return { clienteId: Number(data.clienteId), observaciones: data.observaciones.trim() || null, motivo: data.motivo.trim(), version }
}
export function guardarReserva(lista: Reserva[], saved: Reserva, mode: 'create' | 'edit') {
  const next = mode === 'create' ? [...lista.filter((r) => r.id !== saved.id), saved] : lista.map((r) => r.id === saved.id ? saved : r)
  return next.toSorted((a, b) => a.inicio.localeCompare(b.inicio) || a.id - b.id)
}
export function filtrarPorCliente(reservas: Reserva[], filtro: string) {
  return filtro === '' ? reservas : reservas.filter((r) => r.clienteId === Number(filtro))
}
export function nombreDeCliente(reserva: Reserva, mapa: ReadonlyMap<number, Cliente>) {
  return mapa.get(reserva.clienteId)?.nombre ?? reserva.clienteNombre ?? `Cliente #${reserva.clienteId}`
}
