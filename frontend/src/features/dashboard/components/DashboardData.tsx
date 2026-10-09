import Button from '../../../components/ui/Button'
import QueryState from '../../../components/common/QueryState'
import { useReservas } from '../../reservas/hooks/useReservas'
import DashboardSummary from './DashboardSummary'
import { dashboardMetrics } from '../utils/dashboardMetrics'

export default function DashboardData() {
  // El hook de agenda ya carga ambas colecciones en paralelo. No duplicar GET clientes.
  const { data, loading, error, reload } = useReservas(true)
  return <section aria-labelledby="dashboard-data-title" className="dashboard-data">
    <div className="section-heading">
      <h3 id="dashboard-data-title">Resumen de clientes y reservas</h3>
      <Button variant="secondary" disabled={loading} onClick={reload}>Actualizar indicadores</Button>
    </div>
    <p className="table-help">Datos de la API al entrar o actualizar. Abarcan todas las fechas de la lista cargada; no son métricas en tiempo real ni un reporte de ocupación/no-show.</p>
    <QueryState loading={loading} error={error} onRetry={reload} label="indicadores" />
    {!loading && !error && <DashboardSummary metrics={dashboardMetrics(data.clientes, data.reservas)} />}
  </section>
}
