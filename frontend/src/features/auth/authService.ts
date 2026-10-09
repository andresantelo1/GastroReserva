import { apiFetch } from '../../api/apiClient.ts'
import { booleano, contratoInvalido, numero, objeto, texto } from '../../api/contracts.ts'
import type { Rol, Sesion, Usuario } from './session.ts'
export function leerUsuario(value: unknown): Usuario {
  const v = objeto(value)
  if (!['ADMINISTRADOR','HOST','MESERO','CLIENTE'].includes(String(v.rol))) throw contratoInvalido()
  return { id: numero(v.id), nombre: texto(v.nombre), email: texto(v.email), rol: v.rol as Rol, activo: booleano(v.activo) }
}
export const authService = {
  async login(email: string, password: string, signal?: AbortSignal): Promise<Sesion> {
    const v = objeto(await apiFetch<unknown>('/auth/login', { method: 'POST', body: JSON.stringify({ email: email.trim(), password }), anonymous: true, signal }))
    const expiresAt = texto(v.expiresAt), accessToken = texto(v.accessToken)
    if (v.tokenType !== 'Bearer' || !accessToken || !Number.isFinite(Date.parse(expiresAt)) || Date.parse(expiresAt) <= Date.now()) throw contratoInvalido()
    return { accessToken, expiresAt, tokenType: 'Bearer', usuario: leerUsuario(v.usuario) }
  },
}

