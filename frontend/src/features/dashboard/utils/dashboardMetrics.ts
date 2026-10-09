import type { Cliente } from '../../clientes/models/Cliente'
import type { EstadoReserva, Reserva } from '../../reservas/models/Reserva'

export function dashboardMetrics(clientes: readonly Cliente[], reservas: readonly Reserva[]) {
  const porEstado: Record<EstadoReserva, number> = {
    SOLICITADA: 0, CONFIRMADA: 0, SENTADA: 0, FINALIZADA: 0, CANCELADA: 0, NO_SHOW: 0,
  }
  for (const reserva of reservas) porEstado[reserva.estado]++
  return {
    clientesTotal: clientes.length,
    clientesActivos: clientes.filter((cliente) => cliente.activo).length,
    reservasTotal: reservas.length,
    reservasActivas: porEstado.SOLICITADA + porEstado.CONFIRMADA + porEstado.SENTADA,
    porEstado,
  }
}
