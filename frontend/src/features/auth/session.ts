export type Rol = 'ADMINISTRADOR' | 'HOST' | 'MESERO' | 'CLIENTE'
export interface Usuario { id: number; nombre: string; email: string; rol: Rol; activo: boolean }
export interface Sesion { accessToken: string; tokenType: string; expiresAt: string; usuario: Usuario }
let session: Sesion | null = null
const listeners = new Set<() => void>()
export const getSession = () => session
export function setSession(next: Sesion | null) { session = next; listeners.forEach((notify) => notify()) }
export function subscribeSession(notify: () => void) { listeners.add(notify); return () => { listeners.delete(notify) } }
export function tokenActual() { return session && Date.parse(session.expiresAt) > Date.now() ? session.accessToken : undefined }
export function invalidarToken(token: string) {
  // Una respuesta vieja no puede cerrar una sesión nueva.
  if (session?.accessToken === token) setSession(null)
}

