import { useEffect } from 'react'
import { Link } from 'react-router'
import { useSession } from '../../hooks/useSession'
import { invalidarToken, setSession } from '../../features/auth/session'
export default function Header() {
  const session = useSession()
  useEffect(() => {
    if (!session) return
    const timer = setTimeout(() => invalidarToken(session.accessToken), Math.max(0, Math.min(Date.parse(session.expiresAt) - Date.now(), 2147483647)))
    return () => clearTimeout(timer)
  }, [session])
  return <header className="topbar"><div><p className="topbar__eyebrow">Frontend administrativo</p><h1>GastroReserva</h1></div>
    <div className="session-actions">{session ? <><span className="topbar__status">{session.usuario.nombre} · {session.usuario.rol}</span><button className="btn-secondary" onClick={() => setSession(null)}>Cerrar sesión</button></> : <Link className="button-link" to="/login">Iniciar sesión</Link>}</div>
  </header>
}

