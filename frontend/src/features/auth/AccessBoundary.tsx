import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { useSession } from '../../hooks/useSession'
import type { Rol } from './session'
export default function AccessBoundary({ roles, children }: { roles: Rol[]; children: ReactNode }) {
  const session = useSession()
  if (!session) return <section className="state-card"><h2>Iniciá sesión para continuar</h2><p>Los datos vienen de una API protegida. Usá una cuenta de GastroReserva; no las credenciales de PostgreSQL.</p><Link className="button-link" to="/login">Iniciar sesión</Link></section>
  if (!roles.includes(session.usuario.rol)) return <section className="state-card error" role="alert"><h2>Acceso no disponible para tu rol</h2><p>Esta pantalla administrativa está habilitada para {roles.join(' o ')}. Tu rol actual es {session.usuario.rol}. La experiencia de cliente se integrará en otra etapa.</p></section>
  return children
}

