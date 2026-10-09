import type { Cliente } from '../../clientes/models/Cliente'
import type { EstadoReserva, Reserva } from '../models/Reserva'
import { nombreDeCliente } from './reservaCrud.ts'
import { matchesText } from '../../../utils/search.ts'

export const RESERVA_LABELS: Record<EstadoReserva, string> = {
  SOLICITADA: 'Solicitada', CONFIRMADA: 'Confirmada', SENTADA: 'Sentada', FINALIZADA: 'Finalizada', CANCELADA: 'Cancelada', NO_SHOW: 'No asistió',
}
export type ReservaStatusFilter = EstadoReserva | ''
export function searchReservas(reservas: Reserva[], clientes: Cliente[], search: string, clienteId: string, status: ReservaStatusFilter) {
  const byId = new Map(clientes.map((cliente) => [cliente.id, cliente]))
  return reservas.filter((reserva) => matchesText([reserva.id, nombreDeCliente(reserva, byId), reserva.mesaNumero,
    reserva.fecha, reserva.fecha.split('-').reverse().join('/'), reserva.observaciones], search)
    && (!clienteId || reserva.clienteId === Number(clienteId)) && (!status || reserva.estado === status))
}
