import type { Reserva } from '../models/Reserva'
import type { EventoReserva } from '../types/ReservaClienteRequest'
export default function ReservaDetail({ reserva, eventos, onClose }: { reserva: Reserva; eventos: EventoReserva[]; onClose: () => void }) {
  return <section className="entity-form" aria-labelledby="detail-title">
    <h3 id="detail-title">Detalle de reserva #{reserva.id}</h3>
    <p>{reserva.clienteNombre ?? `Cliente #${reserva.clienteId}`} · Cliente #{reserva.clienteId} · {reserva.estado}</p>
    <p>{reserva.fecha} · {reserva.inicio.slice(11,16)}–{reserva.fin.slice(11,16)} · Mesa {reserva.mesaNumero} · {reserva.cantidadPersonas} personas</p>
    <p>Observaciones: {reserva.observaciones || 'Sin observaciones'}</p>
    <h4>Historial de la reserva</h4>
    {eventos.length === 0 ? <p>Sin eventos registrados.</p> : <ol className="reserva-history">{eventos.map((evento) => <li key={evento.id}>
      <strong>{evento.tipoEvento.replaceAll('_',' ')}</strong> · {evento.estadoAnterior ?? 'Inicio'} → {evento.estadoNuevo}
      <p>{evento.motivo ?? 'Sin motivo adicional'} · {evento.cambiadoPorNombre} · <time dateTime={evento.creadoEn}>{new Date(evento.creadoEn).toLocaleString('es-BO')}</time></p>
      {evento.clienteAnteriorId !== null && <p>Cliente #{evento.clienteAnteriorId} → Cliente #{evento.clienteNuevoId}. Observaciones: «{evento.observacionesAnteriores ?? 'Sin observaciones'}» → «{evento.observacionesNuevas ?? 'Sin observaciones'}».</p>}
    </li>)}</ol>}
    <button type="button" className="btn-secondary" onClick={onClose}>Cerrar detalle</button>
  </section>
}
