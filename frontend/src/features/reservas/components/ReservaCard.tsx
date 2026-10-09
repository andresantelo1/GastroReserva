import type { Reserva } from '../models/Reserva'

interface ReservaCardProps {
  reserva: Reserva
}

export function ReservaCard({ reserva }: ReservaCardProps) {
  return (
    <article className="module-card">
      <p className="module-caption"><span>03</span> RESERVAS</p>
      <h3>Reserva #{reserva.id}</h3>
      <p className="card-description">Cada visita, en su momento.</p>
      <dl className="card-details">
        <div><dt>Fecha</dt><dd>{reserva.fecha}</dd></div>
        <div><dt>Personas</dt><dd>{reserva.cantidadPersonas}</dd></div>
        <div><dt>Estado</dt><dd><span className="status status-neutral">{reserva.estado.replaceAll('_', ' ')}</span></dd></div>
      </dl>
    </article>
  )
}
