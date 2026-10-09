import { useRef, useState } from 'react'
import Button from '../../../components/ui/Button'
import SearchInput from '../../../components/ui/SearchInput'
import SelectFilter from '../../../components/ui/SelectFilter'
import Pagination from '../../../components/ui/Pagination'
import ConfirmDialog from '../../../components/ui/ConfirmDialog'
import { usePagination } from '../../../hooks/usePagination'
import { searchClientes } from '../utils/clienteSearch'
import QueryState from '../../../components/common/QueryState'
import { useClientes, useClienteDetalle } from '../hooks/useClientes'
import { useSuccessMessage } from '../../../hooks/useSuccessMessage'
import { useSubmission } from '../../../hooks/useSubmission'
import type { Cliente } from '../models/Cliente'
import { clienteService } from '../services/clienteService'
import ClienteForm from '../components/ClienteForm'
import ClienteTable from '../components/ClienteTable'
import { guardarEnLista, marcarClienteInactivo, type ClienteFilter, type ClienteSaveMode } from '../utils/clienteCrud'

type FormMode = { kind: 'closed' } | { kind: 'create' } | { kind: 'edit'; id: number }

export default function ClientesPage() {
  const createButton = useRef<HTMLButtonElement>(null)
  const [search, setSearch] = useState('')
  const [mode, setMode] = useState<FormMode>({ kind: 'closed' })
  const [saving, setSaving] = useState(false)
  const detail = useClienteDetalle(mode.kind === 'edit' ? mode.id : null)
  const { message: mensaje, showSuccess: setMensaje } = useSuccessMessage()
  const [filtro, setFiltro] = useState<ClienteFilter>('activos')
  const [pendingDelete, setPendingDelete] = useState<Cliente | null>(null)
  const { data: clientes, setData, loading, error, reload } = useClientes()
  const deletion = useSubmission()
  const deletingId = deletion.busy ? pendingDelete?.id ?? null : null
  const blocked = loading || detail.loading || saving || deletingId !== null || pendingDelete !== null
  const formOpen = mode.kind !== 'closed'
  const activos = clientes.filter((cliente) => cliente.activo).length
  const filtered = searchClientes(clientes, search, filtro)
  const pagination = usePagination(filtered.length)
  const visible = filtered.slice(pagination.start, pagination.end)
  const changeStatus = (value: string) => {
    if (value !== 'activos' && value !== 'inactivos' && value !== 'todos') return
    setFiltro(value); pagination.resetPage()
  }

  const edit = (id: number) => {
    if (blocked || formOpen) return
    setMensaje('')
    deletion.setError('')
    setMode({ kind: 'edit', id })
  }

  const saved = (cliente: Cliente, saveMode: ClienteSaveMode) => {
    setData((prev) => guardarEnLista(prev, cliente, saveMode))
    setMode({ kind: 'closed' })
    setFiltro(cliente.activo ? 'activos' : 'inactivos')
    setSearch(''); pagination.resetPage()
    setMensaje(`Cliente #${cliente.id} ${saveMode === 'create' ? 'creado' : 'actualizado'} correctamente. Estado: ${cliente.activo ? 'activo' : 'inactivo'}.`)
  }

  const requestDelete = (cliente: Cliente) => {
    if (blocked || formOpen || !cliente.activo) return
    setMensaje('')
    deletion.setError('')
    setPendingDelete(cliente)
  }

  const confirmDelete = () => {
    if (!pendingDelete || deletion.busy) return
    const cliente = pendingDelete
    void deletion.run((signal) => clienteService.eliminar(cliente.id, signal), () => {
      setData((prev) => marcarClienteInactivo(prev, cliente.id))
      setPendingDelete(null)
      setMensaje(`Cliente #${cliente.id} dado de baja. Sus reservas e historial se conservaron. Podés reactivarlo desde Inactivos → Editar.`)
    })
  }

  return <section className="feature-page" aria-labelledby="clientes-title">
    <div className="page-heading">
      <p className="page-heading__eyebrow">Personas / CRUD conectado</p>
      <h2 id="clientes-title">Clientes</h2>
      <p className="page-heading__description">Creá, editá o da de baja perfiles sin perder el historial de sus reservas.</p>
      <div className="form-page-actions">
        <Button ref={createButton} disabled={blocked || Boolean(error)} onClick={() => {
          setMode(formOpen ? { kind: 'closed' } : { kind: 'create' })
          setMensaje(''); deletion.setError('')
        }} aria-expanded={formOpen} aria-controls="cliente-form">{formOpen ? 'Cerrar formulario' : '+ Nuevo cliente'}</Button>
        <Button variant="secondary" disabled={blocked || formOpen} onClick={() => {
          setMensaje(''); deletion.setError(''); reload()
        }}>Actualizar lista</Button>
      </div>
    </div>
    <QueryState loading={loading} error={error} onRetry={reload} label="clientes" />
    {mode.kind === 'edit' && <>
      <QueryState loading={detail.loading} error={detail.error} onRetry={detail.reload} label="detalle del cliente" />
      {(detail.loading || detail.error) && <Button variant="secondary" onClick={() => setMode({ kind: 'closed' })}>Cancelar consulta</Button>}
    </>}
    {mensaje && <p className="form-success" role="status">{mensaje}</p>}
    {!loading && !error && <>
      <ConfirmDialog open={pendingDelete !== null} title={`¿Dar de baja a ${pendingDelete?.nombre ?? ''}?`}
        message={`Cliente #${pendingDelete?.id ?? ''}. Esta eliminación es lógica: queda inactivo y no podrá crear nuevas reservas. Las reservas existentes, su historial y su cuenta de acceso se conservan. Podés reactivarlo después.`}
        confirming={deletion.busy} error={deletion.error} onConfirm={confirmDelete}
        onCancel={() => { setPendingDelete(null); deletion.setError('') }} confirmLabel="Confirmar baja" cancelLabel="Cancelar baja" fallbackFocusRef={createButton} />
      {(mode.kind === 'create' || (mode.kind === 'edit' && detail.data && !detail.loading && !detail.error)) && <ClienteForm key={mode.kind === 'edit' ? `edit-${mode.id}` : 'create'}
        cliente={mode.kind === 'edit' ? detail.data : null} onSaved={saved} onBusyChange={setSaving}
        onCancelEdit={() => { setMode({ kind: 'create' }); setMensaje('') }} />}
      <div className="stats-grid" aria-label="Resumen de clientes">
        <article className="stat-card"><span>Total de clientes</span><strong>{clientes.length}</strong></article>
        <article className="stat-card"><span>Activos</span><strong>{activos}</strong></article>
        <article className="stat-card"><span>Inactivos</span><strong>{clientes.length - activos}</strong></article>
      </div>
      <div className="section-heading table-heading">
        <h3>Directorio de clientes</h3>
      </div>
      <div className="ui-toolbar" role="search" aria-label="Buscar y filtrar clientes">
        <SearchInput label="Buscar clientes" placeholder="Nombre, email, teléfono o ID" value={search} disabled={blocked || formOpen}
          onChange={(value) => { setSearch(value); pagination.resetPage() }} />
        <SelectFilter label="Estado del cliente" value={filtro} disabled={blocked || formOpen} onChange={changeStatus}
          options={[{ value: 'activos', label: 'Activos' }, { value: 'inactivos', label: 'Inactivos' }, { value: 'todos', label: 'Todos' }]} />
        <Button variant="ghost" disabled={blocked || formOpen} onClick={() => { setSearch(''); setFiltro('todos'); pagination.resetPage() }}>Limpiar filtros</Button>
      </div>
      <p className="table-help">Eliminar realiza una baja lógica mediante DELETE. Los inactivos siguen registrados y conservan su identificador.</p>
      <ClienteTable clientes={visible} onEdit={(id) => void edit(id)} onDelete={requestDelete}
        disabled={blocked || formOpen} deletingId={deletingId} />
      <Pagination {...pagination} disabled={blocked || formOpen} label="Paginación de clientes" />
    </>}
  </section>
}
