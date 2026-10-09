import MetricCard from '../../../components/ui/MetricCard'
import EmptyState from '../../../components/ui/EmptyState'
import { RESERVA_LABELS } from '../../reservas/utils/reservaSearch'
import type { EstadoReserva } from '../../reservas/models/Reserva'
import type { dashboardMetrics } from '../utils/dashboardMetrics'

export default function DashboardSummary({ metrics }: { metrics: ReturnType<typeof dashboardMetrics> }) {
  return <>
    <div className="stats-grid dashboard-metrics" aria-label="Indicadores del dashboard">
      <MetricCard label="Clientes registrados" value={metrics.clientesTotal} helper="Incluye perfiles activos e inactivos." />
      <MetricCard label="Clientes activos" value={metrics.clientesActivos} helper="Perfiles habilitados para nuevas reservas." />
      <MetricCard label="Reservas registradas" value={metrics.reservasTotal} helper="Todos los estados y fechas de la lista cargada." />
      <MetricCard label="Reservas activas" value={metrics.reservasActivas} helper="Solicitadas + confirmadas + sentadas; no es ocupación actual." />
    </div>
    {metrics.clientesTotal === 0 && metrics.reservasTotal === 0
      ? <EmptyState title="Todavía no hay clientes ni reservas" description="Los indicadores están en cero. Podés comenzar desde Clientes; para reservar también necesitás mesa y turno activos." />
      : <section className="dashboard-states" aria-labelledby="dashboard-states-title">
        <h3 id="dashboard-states-title">Reservas por estado</h3>
        <dl>{(Object.keys(RESERVA_LABELS) as EstadoReserva[]).map((estado) =>
          <div key={estado}><dt>{RESERVA_LABELS[estado]}</dt><dd>{metrics.porEstado[estado]}</dd></div>)}</dl>
      </section>}
  </>
}
