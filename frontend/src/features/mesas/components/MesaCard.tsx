import type { Mesa } from '../types/Mesa'

interface MesaCardProps {
  mesa: Mesa
}

export function MesaCard({ mesa }: MesaCardProps) {
  return (
    <article className="module-card">
      <p className="module-caption"><span>02</span> MESAS</p>
      <h3>Mesa {mesa.numero}</h3>
      <p className="card-description">Un lugar para compartir.</p>
      <dl className="card-details">
        <div><dt>Zona</dt><dd>{mesa.zona.nombre}</dd></div>
        <div><dt>Capacidad</dt><dd>{mesa.capacidad} personas</dd></div>
        <div><dt>Estado</dt><dd><span className="status">{mesa.estado === 'DISPONIBLE' ? 'Disponible' : 'Ocupada'}</span></dd></div>
      </dl>
    </article>
  )
}
