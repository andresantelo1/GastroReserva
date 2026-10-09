import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <section className="not-found" aria-labelledby="not-found-title">
      <p className="page-heading__eyebrow">Error 404 / Ruta no encontrada</p>
      <span className="not-found__number" aria-hidden="true">404</span>
      <h2 id="not-found-title">La página solicitada no existe</h2>
      <p>Revisá la dirección o volvé al inicio. El menú sigue disponible para explorar GastroReserva.</p>
      <Link className="button-link" to="/">Volver al inicio <span aria-hidden="true">→</span></Link>
    </section>
  )
}
