import type { Cliente } from '../models/Cliente'
import Button from '../../../components/ui/Button'
import EmptyState from '../../../components/ui/EmptyState'
import StatusBadge from '../../../components/ui/StatusBadge'

interface ClienteTableProps {
  clientes: Cliente[]
  onEdit: (id: number) => void
  onDelete: (cliente: Cliente) => void
  disabled?: boolean
  deletingId?: number | null
}

export default function ClienteTable({ clientes, onEdit, onDelete, disabled = false, deletingId }: ClienteTableProps) {
  if (clientes.length === 0) {
    return <EmptyState title="No hay clientes en esta vista" description="Cambiá la búsqueda o el estado, limpiá los filtros o creá un cliente nuevo." />
  }
  return <div className="table-card">
    <div className="table-responsive" role="region" aria-label="Listado de clientes, desplazable horizontalmente" tabIndex={0}>
      <table className="data-table">
        <caption className="sr-only">Clientes, estado y acciones</caption>
        <thead><tr>
          <th scope="col">ID</th><th scope="col">Cliente</th><th scope="col">Teléfono</th>
          <th scope="col">Email</th><th scope="col">Estado</th><th scope="col">Acciones</th>
        </tr></thead>
        <tbody>{clientes.map((cliente) => <tr key={cliente.id}>
          <td className="id-cell">#{cliente.id}</td>
          <th scope="row">{cliente.nombre}</th>
          <td>{cliente.telefono ?? 'Sin teléfono'}</td>
          <td>{cliente.email}</td>
          <td><StatusBadge tone={cliente.activo ? 'success' : 'danger'} label={cliente.activo ? 'Activo' : 'Inactivo'} /></td>
          <td><div className="actions-cell">
            <Button variant="secondary" onClick={() => onEdit(cliente.id)} disabled={disabled} aria-label={`Editar cliente #${cliente.id}`}>Editar</Button>
            <Button variant="danger" onClick={() => onDelete(cliente)} disabled={disabled || !cliente.activo} loading={deletingId === cliente.id} loadingText="Eliminando…"
              aria-label={`Eliminar cliente #${cliente.id} (baja lógica)`}>{cliente.activo ? 'Eliminar' : 'Ya inactivo'}</Button>
          </div></td>
        </tr>)}</tbody>
      </table>
    </div>
  </div>
}
