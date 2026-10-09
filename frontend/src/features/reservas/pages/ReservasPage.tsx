import { useRef, useState } from 'react'
import Button from '../../../components/ui/Button'
import SearchInput from '../../../components/ui/SearchInput'
import SelectFilter from '../../../components/ui/SelectFilter'
import Pagination from '../../../components/ui/Pagination'
import { usePagination } from '../../../hooks/usePagination'
import { searchReservas, RESERVA_LABELS, type ReservaStatusFilter } from '../utils/reservaSearch'
import QueryState from '../../../components/common/QueryState'
import { useReservas, useReservaDetalle } from '../hooks/useReservas'
import { useSuccessMessage } from '../../../hooks/useSuccessMessage'
import { useSession } from '../../../hooks/useSession'
import type { Reserva } from '../models/Reserva'
import ReservaCreatePanel from '../components/ReservaCreatePanel'
import ReservaClienteForm from '../components/ReservaClienteForm'
import ReservaCancelPanel from '../components/ReservaCancelPanel'
import ReservaDetail from '../components/ReservaDetail'
import ReservaTable from '../components/ReservaTable'
import { guardarReserva, puedeCorregir, puedeCancelar } from '../utils/reservaCrud'

type Panel = { kind: 'closed' } | { kind: 'create' } | { kind: 'cancel'; reserva: Reserva } | { kind: 'edit' | 'detail'; id: number }
export default function ReservasPage() {
  const createButton = useRef<HTMLButtonElement>(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<ReservaStatusFilter>('')
  const session = useSession()
  const puedeGestionar = session?.usuario.rol === 'ADMINISTRADOR' || session?.usuario.rol === 'HOST'
  const { data, setData, loading, error, reload } = useReservas(puedeGestionar)
  const [panel, setPanel] = useState<Panel>({ kind: 'closed' })
  const [busy, setBusy] = useState(false), [clienteFiltro, setClienteFiltro] = useState('')
  const { message: mensaje, showSuccess: setMensaje } = useSuccessMessage()
  const detail = useReservaDetalle(panel.kind === 'edit' || panel.kind === 'detail' ? panel.id : null, panel.kind === 'detail')
  const saving = busy && (panel.kind === 'create' || panel.kind === 'edit')
  const cancellingId = busy && panel.kind === 'cancel' ? panel.reserva.id : null
  const blocked = loading || detail.loading || saving || cancellingId !== null
  const noEditable = panel.kind === 'edit' && detail.data !== null && !puedeCorregir(detail.data.reserva)
  const abierto = panel.kind !== 'closed'
  const opciones = new Map(data.clientes.map((c) => [c.id, `${c.nombre} · #${c.id}${c.activo ? '' : ' (inactivo)'}`]))
  // El mesero usa la proyección de la agenda, sin pedir un catálogo de clientes no autorizado.
  for (const r of data.reservas) if (!opciones.has(r.clienteId)) opciones.set(r.clienteId, `${r.clienteNombre ?? 'Cliente'} · #${r.clienteId}`)
  const abrir = (id: number, kind: 'edit' | 'detail') => {
    if (blocked || abierto || (kind === 'edit' && !puedeGestionar)) return
    setMensaje(''); setPanel({ kind, id })
  }
  const saved = (reserva: Reserva, mode: 'create' | 'edit', action: string) => {
    setData((prev) => ({ ...prev, reservas: guardarReserva(prev.reservas, reserva, mode) }))
    setPanel({ kind: 'closed' }); setClienteFiltro(String(reserva.clienteId))
    setSearch(''); setStatus(''); pagination.resetPage()
    setMensaje(`Reserva #${reserva.id} ${action}. Estado: ${reserva.estado}.`)
  }
  const filtered = searchReservas(data.reservas, data.clientes, search, clienteFiltro, status)
  const pagination = usePagination(filtered.length)
  const visibles = filtered.slice(pagination.start, pagination.end)
  return <section className="feature-page" aria-labelledby="reservas-title">
    <div className="page-heading"><p className="page-heading__eyebrow">Guía 09 / Búsqueda y gestión de reservas</p><h2 id="reservas-title">Reservas</h2><p className="page-heading__description">Consultá la agenda, corregí el cliente de una solicitud y cancelá sin perder su historial.</p>
      <div className="form-page-actions">
        {puedeGestionar && <Button ref={createButton} disabled={blocked || Boolean(error) || panel.kind === 'cancel'} onClick={() => { setPanel(abierto ? { kind: 'closed' } : { kind: 'create' }); setMensaje('') }} aria-expanded={abierto}>{abierto ? 'Cerrar panel' : '+ Nueva reserva'}</Button>}
        <Button variant="secondary" disabled={blocked || abierto} onClick={() => { setMensaje(''); reload() }}>Actualizar agenda</Button>
      </div>
    </div>
    <QueryState loading={loading} error={error} onRetry={reload} label="reservas y clientes autorizados" />
    {(panel.kind === 'edit' || panel.kind === 'detail') && <>
      <QueryState loading={detail.loading} error={detail.error} onRetry={detail.reload} label="detalle de la reserva" />
      {noEditable && <p role="alert" className="form-error-summary">La reserva ya no está SOLICITADA. Cerrá el panel y actualizá la agenda.</p>}
      {(detail.loading || detail.error || noEditable) && <Button variant="secondary" onClick={() => setPanel({ kind: 'closed' })}>Cancelar consulta</Button>}
    </>}
    {mensaje && <p role="status" className="form-success">{mensaje}</p>}
    {!loading && !error && <>
      {panel.kind === 'create' && puedeGestionar && <ReservaCreatePanel clientes={data.clientes} onBusyChange={setBusy} onCreated={(r) => saved(r, 'create', 'creada')} />}
      {panel.kind === 'edit' && puedeGestionar && detail.data && !detail.loading && !detail.error && !noEditable && <ReservaClienteForm key={panel.id} reserva={detail.data.reserva} clientes={data.clientes} onBusyChange={setBusy} onCancel={() => setPanel({ kind: 'closed' })} onSaved={(r) => saved(r, 'edit', 'actualizada')} />}
      {panel.kind === 'cancel' && puedeGestionar && <ReservaCancelPanel key={panel.reserva.id} reserva={panel.reserva} fallbackFocusRef={createButton} onBusyChange={setBusy} onClose={() => setPanel({ kind: 'closed' })} onSaved={(r) => saved(r, 'edit', 'cancelada; historial conservado')} />}
      {panel.kind === 'detail' && detail.data && !detail.loading && !detail.error && <ReservaDetail {...detail.data} onClose={() => setPanel({ kind: 'closed' })} />}
      <div className="stats-grid" aria-label="Resumen de reservas"><article className="stat-card"><span>Total de reservas</span><strong>{data.reservas.length}</strong></article><article className="stat-card"><span>Resultados del filtro</span><strong>{filtered.length}</strong></article><article className="stat-card"><span>Canceladas (conservadas)</span><strong>{data.reservas.filter((r) => r.estado === 'CANCELADA').length}</strong></article></div>
      <div className="section-heading table-heading"><h3>Agenda del restaurante</h3></div>
      <div className="ui-toolbar" role="search" aria-label="Buscar y filtrar reservas">
        <SearchInput label="Buscar reservas" placeholder="ID, cliente, mesa, fecha u observaciones" value={search} disabled={blocked || abierto}
          onChange={(value) => { setSearch(value); pagination.resetPage() }} />
        <SelectFilter label="Filtrar por cliente" value={clienteFiltro} disabled={blocked || abierto}
          options={[{ value: '', label: 'Todos los clientes' }, ...[...opciones].map(([id, label]) => ({ value: String(id), label }))]}
          onChange={(value) => { setClienteFiltro(value); pagination.resetPage() }} />
        <SelectFilter label="Estado de la reserva" value={status} disabled={blocked || abierto}
          options={[{ value: '', label: 'Todos los estados' }, ...Object.entries(RESERVA_LABELS).map(([value, label]) => ({ value, label }))]}
          onChange={(value) => { if (value === '' || Object.hasOwn(RESERVA_LABELS, value)) { setStatus(value as ReservaStatusFilter); pagination.resetPage() } }} />
        <Button variant="ghost" disabled={blocked || abierto} onClick={() => { setSearch(''); setStatus(''); setClienteFiltro(''); pagination.resetPage() }}>Limpiar filtros</Button>
      </div>
      <p className="table-help">Filtro local: no hace peticiones por fila. Editar sólo corrige cliente/observaciones de SOLICITADA. Cancelar admite SOLICITADA o CONFIRMADA y conserva la fila. Los permisos y estados se comprueban nuevamente en Spring.</p>
      <ReservaTable reservas={visibles} clientes={data.clientes} disabled={blocked || abierto} onDetail={(id) => void abrir(id, 'detail')} onEdit={puedeGestionar ? (id) => void abrir(id, 'edit') : undefined}
        onCancel={puedeGestionar ? (reserva) => { if (blocked || abierto || !puedeCancelar(reserva)) return; setMensaje(''); setPanel({ kind: 'cancel', reserva }) } : undefined} />
      <Pagination {...pagination} disabled={blocked || abierto} label="Paginación de reservas" />
    </>}
  </section>
}

