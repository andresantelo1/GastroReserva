import type { Cliente } from '../models/Cliente'
import type { ClienteFormData } from '../types/ClienteFormData'
import type { ClienteUpdateRequest } from '../types/ClienteRequests'
import { prepararClientePayload } from './clienteValidation.ts'

export type ClienteSaveMode = 'create' | 'edit'
export type ClienteFilter = 'activos' | 'inactivos' | 'todos'

export function formularioDesdeCliente(cliente?: Cliente | null): ClienteFormData {
  return { nombre: cliente?.nombre ?? '', email: cliente?.email ?? '', telefono: cliente?.telefono ?? '' }
}

export function prepararClienteUpdate(data: ClienteFormData, activo: boolean): ClienteUpdateRequest {
  return { ...prepararClientePayload(data), activo }
}

export function guardarEnLista(clientes: Cliente[], saved: Cliente, mode: ClienteSaveMode): Cliente[] {
  if (mode === 'edit') return clientes.map((cliente) => cliente.id === saved.id ? saved : cliente)
  return [...clientes.filter((cliente) => cliente.id !== saved.id), saved]
}

export function marcarClienteInactivo(clientes: Cliente[], id: number): Cliente[] {
  return clientes.map((cliente) => cliente.id === id ? { ...cliente, activo: false } : cliente)
}

export function filtrarClientes(clientes: Cliente[], filtro: ClienteFilter): Cliente[] {
  return clientes.filter((cliente) => filtro === 'todos' || cliente.activo === (filtro === 'activos'))
}
