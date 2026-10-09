import type { Cliente } from '../models/Cliente'

interface ClienteCardProps {
  cliente: Cliente
}

export function ClienteCard({ cliente }: ClienteCardProps) {
  return (
    <article className="module-card">
      <p className="module-caption"><span>01</span> CLIENTES</p>
      <h3>{cliente.nombre}</h3>
      <p className="card-description">Las personas detrás de cada visita.</p>
      <dl className="card-details">
        <div><dt>Correo</dt><dd>{cliente.email}</dd></div>
        <div><dt>Perfil</dt><dd>Ejemplo local</dd></div>
      </dl>
    </article>
  )
}
