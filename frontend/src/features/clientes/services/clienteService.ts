import { apiFetch } from '../../../api/apiClient.ts'
import { booleano, lista, nullableTexto, numero, objeto, texto } from '../../../api/contracts.ts'
import type { Cliente } from '../models/Cliente'
import type { ClienteCreateRequest, ClienteUpdateRequest } from '../types/ClienteRequests'

export function leerCliente(value: unknown): Cliente {
  const v = objeto(value)
  return { id: numero(v.id), nombre: texto(v.nombre), email: texto(v.email), telefono: nullableTexto(v.telefono), activo: booleano(v.activo) }
}

function rutaCliente(id: number): string {
  if (!Number.isSafeInteger(id) || id <= 0) throw new Error('El id del cliente debe ser un entero positivo válido.')
  return `/clientes/${id}`
}

// Inyección del transporte para probar métodos, rutas y contratos sin una API encendida.
export function createClienteService(request: typeof apiFetch = apiFetch) {
  return {
    async listar(signal?: AbortSignal): Promise<Cliente[]> {
      return lista(await request<unknown>('/clientes', { signal }), leerCliente)
    },
    async obtenerPorId(id: number, signal?: AbortSignal): Promise<Cliente> {
      return leerCliente(await request<unknown>(rutaCliente(id), { signal }))
    },
    async crear(data: ClienteCreateRequest, signal?: AbortSignal): Promise<Cliente> {
      return leerCliente(await request<unknown>('/clientes', { method: 'POST', body: JSON.stringify(data), signal }))
    },
    async actualizar(id: number, data: ClienteUpdateRequest, signal?: AbortSignal): Promise<Cliente> {
      return leerCliente(await request<unknown>(rutaCliente(id), { method: 'PUT', body: JSON.stringify(data), signal }))
    },
    async eliminar(id: number, signal?: AbortSignal): Promise<void> {
      await request<void>(rutaCliente(id), { method: 'DELETE', signal })
    },
  }
}

export const clienteService = createClienteService()
