import type { Cliente } from '../../clientes/models/Cliente'
import type { EstadoReserva, Reserva } from '../models/Reserva'
import { nombreDeCliente, puedeCancelar, puedeCorregir } from '../utils/reservaCrud'
import Button from '../../../components/ui/Button'
import EmptyState from '../../../components/ui/EmptyState'
import StatusBadge, { type StatusTone } from '../../../components/ui/StatusBadge'
import { RESERVA_LABELS } from '../utils/reservaSearch'

interface ReservaTableProps {
  reservas: Reserva[]
  clientes: Cliente[]
  onDetail?: (id: number) => void
  onEdit?: (id: number) => void
  onCancel?: (reserva: Reserva) => void
  disabled?: boolean
}

const tonos: Record<EstadoReserva, StatusTone> = {
  SOLICITADA: 'warning', CONFIRMADA: 'success', SENTADA: 'info',
  FINALIZADA: 'neutral', CANCELADA: 'danger', NO_SHOW: 'danger',
}

export default function ReservaTable({ reservas, clientes, onDetail, onEdit, onCancel, disabled }: ReservaTableProps) {
  const clientePorId = new Map(clientes.map((c) => [c.id, c]))
  const conAcciones = Boolean(onDetail || onEdit || onCancel)

  if (reservas.length === 0) {
    return <EmptyState title="No hay reservas en esta vista" description="Revisá la búsqueda, el cliente y el estado seleccionados. Podés limpiar los filtros para ver toda la agenda." />
  }

  return (
    <div className="table-card">
      <div className="table-responsive" role="region" aria-label="Listado de reservas, desplazable horizontalmente" tabIndex={0}>
        <table className="data-table reservas-table">
          <caption className="sr-only">Reservas relacionadas con clientes</caption>
          <thead>
            <tr>
              <th scope="col">Reserva</th><th scope="col">Cliente</th><th scope="col">Fecha y horario</th>
              <th scope="col">Mesa</th><th scope="col">Personas</th><th scope="col">Estado</th>
              <th scope="col">Observaciones</th>{conAcciones && <th scope="col">Acciones</th>}
            </tr>
          </thead>
          <tbody>
            {reservas.map((reserva) => (
              <tr key={reserva.id}>
                <th scope="row" className="id-cell">#{reserva.id}</th>
                <td className="client-cell">{nombreDeCliente(reserva, clientePorId)}<small>Cliente #{reserva.clienteId}</small></td>
                <td className="date-cell">
                  <time dateTime={reserva.fecha}>{reserva.fecha.split('-').reverse().join('/')}</time>
                  <small>
                    <time dateTime={reserva.inicio}>{reserva.inicio.slice(11, 16)}</time>
                    {' – '}
                    <time dateTime={reserva.fin}>{reserva.fin.slice(11, 16)}</time>
                    {reserva.fin.slice(0, 10) !== reserva.fecha && ' (día siguiente)'}
                  </small>
                </td>
                <td>{reserva.mesaNumero}</td>
                <td>{reserva.cantidadPersonas}</td>
                <td><StatusBadge tone={tonos[reserva.estado]} label={RESERVA_LABELS[reserva.estado]} /></td>
                <td className="notes-cell">{reserva.observaciones ?? 'Sin observaciones'}</td>
                {conAcciones && <td className="actions-cell">
                  {onDetail && <Button variant="secondary" disabled={disabled} onClick={() => onDetail(reserva.id)} aria-label={`Detalle de reserva #${reserva.id}`}>Detalle</Button>}
                  {onEdit && <Button variant="secondary" disabled={disabled || !puedeCorregir(reserva)} onClick={() => onEdit(reserva.id)} aria-label={`Editar reserva #${reserva.id}`} title="Sólo SOLICITADA: cliente y observaciones">Editar</Button>}
                  {onCancel && <Button variant="danger" disabled={disabled || !puedeCancelar(reserva)} onClick={() => onCancel(reserva)} aria-label={`Cancelar reserva #${reserva.id}`} title="Sólo SOLICITADA o CONFIRMADA">Cancelar</Button>}
                </td>}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
