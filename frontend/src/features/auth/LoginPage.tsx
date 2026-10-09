import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { API_URL } from '../../api/apiClient'
import { useSubmission } from '../../hooks/useSubmission'
import { useSession } from '../../hooks/useSession'
import { authService } from './authService'
import { setSession } from './session'
export default function LoginPage() {
  const [email, setEmail] = useState(''), [password, setPassword] = useState('')
  const navigate = useNavigate(), session = useSession()
  const { busy, error, run } = useSubmission()
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void run((signal) => authService.login(email, password, signal), (next) => {
      setPassword(''); setSession(next)
      navigate(next.usuario.rol === 'MESERO' ? '/reservas' : next.usuario.rol === 'CLIENTE' ? '/' : '/clientes', { replace: true })
    })
  }
  if (session) return <section className="state-card"><h2>Ya iniciaste sesión</h2><p>{session.usuario.nombre} · {session.usuario.rol}</p><button className="btn-secondary" onClick={() => setSession(null)}>Cerrar sesión</button></section>
  return <section className="feature-page"><div className="page-heading"><p className="page-heading__eyebrow">Acceso / API protegida</p><h2>Iniciar sesión</h2><p className="page-heading__description">Usá el email y la contraseña de un usuario de GastroReserva.</p></div>
    <form className="entity-form login-form" onSubmit={submit} aria-busy={busy}>
      <p>Administrador y host pueden crear clientes y reservas. No uses la contraseña de tu base de datos.</p>
      <fieldset className="form-fields" disabled={busy}><legend className="sr-only">Credenciales de GastroReserva</legend><div className="form-grid">
        <label className="form-field form-span-2" htmlFor="login-email">Email<input id="login-email" type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.currentTarget.value)} required maxLength={255} /></label>
        <label className="form-field form-span-2" htmlFor="login-password">Contraseña<input id="login-password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.currentTarget.value)} required /></label>
      </div></fieldset>
      {error && <p className="form-error-summary" role="alert">{error}</p>}
      <div className="form-actions"><button className="btn-primary" disabled={busy} type="submit">{busy ? 'Ingresando…' : 'Ingresar'}</button></div>
      <p className="table-help">La sesión se mantiene sólo en memoria. Si recargás la página, tendrás que ingresar otra vez. No guardamos la contraseña ni el token en localStorage.</p>
      <p className="api-address">API configurada: {API_URL || 'Falta VITE_API_URL'}</p>
    </form>
  </section>
}

