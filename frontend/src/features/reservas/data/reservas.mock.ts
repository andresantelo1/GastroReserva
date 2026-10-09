import type { Reserva } from '../models/Reserva'

// Escenarios ficticios, no una agenda actual ni una consulta de disponibilidad.
export const reservasMock: Reserva[] = [
  {
    id: 1, clienteId: 1, mesaNumero: 1, fecha: '2026-10-08',
    inicio: '2026-10-08T12:00:00', fin: '2026-10-08T15:30:00',
    cantidadPersonas: 4, estado: 'CONFIRMADA', observaciones: 'Mesa junto a la ventana',
  },
  {
    id: 2, clienteId: 1, mesaNumero: 2, fecha: '2026-10-09',
    inicio: '2026-10-09T19:00:00', fin: '2026-10-09T22:00:00',
    cantidadPersonas: 2, estado: 'SOLICITADA', observaciones: null,
  },
  {
    id: 3, clienteId: 2, mesaNumero: 3, fecha: '2026-10-08',
    inicio: '2026-10-08T12:00:00', fin: '2026-10-08T15:30:00',
    cantidadPersonas: 6, estado: 'SENTADA', observaciones: 'Visita familiar',
  },
  {
    id: 4, clienteId: 4, mesaNumero: 4, fecha: '2026-10-07',
    inicio: '2026-10-07T19:00:00', fin: '2026-10-07T22:00:00',
    cantidadPersonas: 4, estado: 'CANCELADA', observaciones: 'Cambio de planes',
  },
  // Práctica: tres reservas adicionales con clientes existentes.
  {
    id: 5, clienteId: 3, mesaNumero: 1, fecha: '2026-10-06',
    inicio: '2026-10-06T12:00:00', fin: '2026-10-06T15:30:00',
    cantidadPersonas: 2, estado: 'FINALIZADA', observaciones: null,
  },
  {
    id: 6, clienteId: 4, mesaNumero: 2, fecha: '2026-10-06',
    inicio: '2026-10-06T19:00:00', fin: '2026-10-06T22:00:00',
    cantidadPersonas: 2, estado: 'NO_SHOW', observaciones: null,
  },
  {
    id: 7, clienteId: 5, mesaNumero: 4, fecha: '2026-10-10',
    inicio: '2026-10-10T19:00:00', fin: '2026-10-10T22:00:00',
    cantidadPersonas: 4, estado: 'CONFIRMADA', observaciones: 'Celebración de cumpleaños',
  },
  // Caso intencional de la guía: prueba el fallback visual de find().
  // No representa un registro válido para crear en el backend.
  {
    id: 8, clienteId: 999, mesaNumero: 5, fecha: '2026-10-10',
    inicio: '2026-10-10T12:00:00', fin: '2026-10-10T15:30:00',
    cantidadPersonas: 2, estado: 'SOLICITADA', observaciones: 'Prueba de cliente inexistente',
  },
]
