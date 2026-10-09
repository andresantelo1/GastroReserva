import { createApiClient } from './httpClient.ts'
import { invalidarToken, tokenActual } from '../features/auth/session.ts'
// Única lectura de configuración pública; ningún secreto en variables VITE_.
export const API_URL = import.meta.env?.VITE_API_URL
export const apiFetch = createApiClient({ baseUrl: API_URL, getToken: tokenActual, onUnauthorized: invalidarToken })

