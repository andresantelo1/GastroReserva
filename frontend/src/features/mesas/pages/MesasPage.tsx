import { MesaCard } from '../components/MesaCard'
import QueryState from '../../../components/common/QueryState'
import { useApiQuery } from '../../../hooks/useApiQuery'
import { reservaService } from '../../reservas/services/reservaService'
import type { Mesa } from '../types/Mesa'
export default function MesasPage() {
  const { data, loading, error, reload } = useApiQuery<Mesa[]>(reservaService.mesas, [])
  return <section aria-labelledby="mesas-title"><div className="page-heading"><p className="page-heading__eyebrow">Salón / Catálogo real</p><h2 id="mesas-title">Mesas activas</h2><p className="page-heading__description">Estado operativo del salón. No equivale a disponibilidad para una fecha y un turno.</p><button className="btn-secondary" disabled={loading} onClick={reload}>Actualizar mesas</button></div>
    <QueryState loading={loading} error={error} onRetry={reload} label="mesas" />
    {!loading && !error && (data.length ? <div className="module-grid">{data.map((mesa) => <MesaCard key={mesa.id} mesa={mesa} />)}</div> : <div className="empty-state">No hay mesas activas registradas.</div>)}
  </section>
}

