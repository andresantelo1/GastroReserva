import type { Cliente } from '../features/clientes/models/Cliente'
import type { Reserva, EstadoReserva } from '../features/reservas/models/Reserva'
export const clientes: Cliente[] = [
  { id: 1, nombre: 'Ana Torres', email: 'ana@example.test', telefono: null, activo: true },
  { id: 2, nombre: 'Bruno Pérez', email: 'bruno@example.test', telefono: '70000000', activo: false },
]
export function reserva(id: number, estado: EstadoReserva = 'SOLICITADA'): Reserva {
  return { id, clienteId: 1, mesaNumero: 9, fecha: '2030-01-20', inicio: '2030-01-20T12:00:00',
    fin: '2030-01-20T15:00:00', cantidadPersonas: 2, estado, observaciones: null, version: 0 }
}
