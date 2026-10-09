import type { Cliente } from '../models/Cliente'
import type { ClienteFilter } from './clienteCrud'
import { matchesText } from '../../../utils/search.ts'

export function searchClientes(clientes: Cliente[], search: string, status: ClienteFilter) {
  return clientes.filter((cliente) => matchesText([cliente.nombre, cliente.email, cliente.telefono, cliente.id], search)
    && (status === 'todos' || (status === 'activos' ? cliente.activo : !cliente.activo)))
}
