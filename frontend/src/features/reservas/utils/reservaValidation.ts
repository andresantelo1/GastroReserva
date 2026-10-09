import type { Cliente } from '../../clientes/models/Cliente'
import type { Mesa } from '../../mesas/types/Mesa'
import type { ReservaFormData, ReservaOperativaPayload, TurnoOpcion } from '../types/ReservaFormData'

export type ReservaFormErrors = Partial<Record<keyof ReservaFormData, string>>
export interface OpcionesReserva {
  clientes: Cliente[]
  mesas: Mesa[]
  turnos: TurnoOpcion[]
}

function enteroPositivo(value: string): boolean {
  return /^\d+$/.test(value.trim()) && Number.isSafeInteger(Number(value)) && Number(value) > 0
}

export function validarReserva(data: ReservaFormData, opciones: OpcionesReserva): ReservaFormErrors {
  const errors: ReservaFormErrors = {}
  if (!enteroPositivo(data.clienteId) || !opciones.clientes.some((c) => c.id === Number(data.clienteId) && c.activo)) {
    errors.clienteId = 'Seleccioná un cliente activo de la lista.'
  }
  if (!enteroPositivo(data.turnoId) || !opciones.turnos.some((t) => t.id === Number(data.turnoId) && t.activo)) {
    errors.turnoId = 'Seleccioná un turno de la lista.'
  }
  const mesa = opciones.mesas.find((m) => m.id === Number(data.mesaId) && m.activa && m.zona.activa)
  if (!enteroPositivo(data.mesaId) || !mesa) errors.mesaId = 'Seleccioná una mesa activa de la lista.'

  const fecha = data.fecha.trim()
  const fechaParseada = new Date(`${fecha}T00:00:00Z`)
  if (!/^\d{4}-\d{2}-\d{2}$/.test(fecha) || fecha.startsWith('0000') || Number.isNaN(fechaParseada.getTime()) || fechaParseada.toISOString().slice(0, 10) !== fecha) {
    errors.fecha = 'Ingresá una fecha válida.'
  }
  if (!enteroPositivo(data.cantidadPersonas)) {
    errors.cantidadPersonas = 'Ingresá una cantidad entera mayor que cero.'
  } else if (Number(data.cantidadPersonas) > 2147483647) {
    errors.cantidadPersonas = 'La cantidad supera el máximo admitido por la API.'
  } else if (mesa && Number(data.cantidadPersonas) > mesa.capacidad) {
    errors.cantidadPersonas = `La mesa admite hasta ${mesa.capacidad} personas.`
  }
  if (data.observaciones.trim().length > 500) errors.observaciones = 'Usá hasta 500 caracteres para las observaciones.'
  return errors
}

// Convertimos sólo después de validar. mesaId es el id, no el número visible de mesa.
export function prepararReservaPayload(data: ReservaFormData): ReservaOperativaPayload {
  return {
    clienteId: Number(data.clienteId), fecha: data.fecha.trim(), turnoId: Number(data.turnoId),
    mesaId: Number(data.mesaId), cantidadPersonas: Number(data.cantidadPersonas),
    observaciones: data.observaciones.trim() || null,
  }
}

