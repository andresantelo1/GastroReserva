export class ApiError extends Error {
  status: number
  code: string
  fieldErrors: Record<string, string>
  constructor(status: number, message: string, code = 'HTTP_ERROR', fieldErrors: Record<string, string> = {}) {
    super(message); this.name = 'ApiError'; this.status = status; this.code = code; this.fieldErrors = fieldErrors
  }
}
export const esCancelacion = (error: unknown) => error instanceof Error && error.name === 'AbortError'
export const mensajeError = (error: unknown) => error instanceof ApiError && error.status >= 400
  ? `HTTP ${error.status}: ${error.message}` : error instanceof Error ? error.message : 'Ocurrió un error inesperado.'
interface HttpConfig {
  baseUrl?: string
  getToken?: () => string | undefined
  onUnauthorized?: (token: string) => void
  fetchImpl?: typeof fetch
  timeoutMs?: number
}
type RequestOptions = RequestInit & { anonymous?: boolean }
export function createApiClient(config: HttpConfig) {
  return async function apiFetch<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
    const { anonymous = false, signal, ...init } = options
    const base = config.baseUrl?.trim().replace(/\/+$/, '')
    if (!base) throw new ApiError(0, 'Falta VITE_API_URL. Revisá .env.development y reiniciá Vite.', 'CONFIG')
    let url: URL
    try { url = new URL(base) } catch { throw new ApiError(0, 'VITE_API_URL debe ser una URL HTTP o HTTPS válida.', 'CONFIG') }
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash ||
        !endpoint.startsWith('/') || endpoint.startsWith('//') || endpoint.includes('..') || endpoint.includes('\\')) {
      throw new ApiError(0, 'La configuración de la ruta de API no es válida.', 'CONFIG')
    }
    const headers = new Headers(init.headers)
    headers.set('Accept', 'application/json')
    if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
    const token = anonymous ? undefined : config.getToken?.()
    if (token) headers.set('Authorization', `Bearer ${token}`)
    const controller = new AbortController()
    let timedOut = false
    const abort = () => controller.abort()
    if (signal?.aborted) controller.abort()
    else signal?.addEventListener('abort', abort, { once: true })
    const timer = setTimeout(() => { timedOut = true; controller.abort() }, config.timeoutMs ?? 15000)
    const method = init.method ?? 'GET'
    const uncertain = method === 'GET' || anonymous ? '' : ' La solicitud pudo haberse guardado: actualizá la lista antes de repetirla.'
    try {
      const response = await (config.fetchImpl ?? fetch)(base + endpoint, {
        ...init, headers, signal: controller.signal, credentials: 'omit', redirect: 'error',
      })
      if (response.status === 401 && token) config.onUnauthorized?.(token)
      const raw = response.status === 204 ? '' : await response.text()
      let body: unknown
      try { body = raw ? JSON.parse(raw) : undefined } catch { body = undefined }
      if (!response.ok) {
        const object = body && typeof body === 'object' ? body as Record<string, unknown> : {}
        const fields = object.fieldErrors && typeof object.fieldErrors === 'object'
          ? Object.fromEntries(Object.entries(object.fieldErrors).filter((entry): entry is [string, string] => typeof entry[1] === 'string')) : {}
        const fallback: Record<number, string> = {
          401: 'Sesión ausente o vencida. Iniciá sesión nuevamente.', 403: 'Tu usuario no tiene permiso para esta operación.',
          404: 'No se encontró la ruta o el recurso solicitado.', 409: 'Ya existe un registro con esos datos.',
          422: 'La operación no cumple las reglas del restaurante.',
        }
        const message = response.status >= 500 ? 'El servidor tuvo un problema. Revisá los logs de Spring Boot.'
          : typeof object.message === 'string' ? object.message : fallback[response.status] ?? 'La API rechazó la solicitud.'
        throw new ApiError(response.status, message, typeof object.code === 'string' ? object.code : 'HTTP_ERROR', fields)
      }
      if (response.status === 204) return undefined as T
      if (body === undefined) throw new ApiError(response.status, 'La API no devolvió el JSON esperado.', 'INVALID_RESPONSE')
      return body as T
    } catch (error) {
      if (signal?.aborted) throw new DOMException('Solicitud cancelada', 'AbortError')
      if (timedOut) throw new ApiError(0, 'El servidor tardó demasiado en responder.' + uncertain, 'TIMEOUT')
      if (error instanceof ApiError) throw error
      if (esCancelacion(error)) throw error
      throw new ApiError(0, 'No se pudo conectar con la API. Comprobá Spring Boot, la URL y CORS.' + uncertain, 'NETWORK')
    } finally { clearTimeout(timer); signal?.removeEventListener('abort', abort) }
  }
}

