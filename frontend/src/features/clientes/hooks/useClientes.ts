import { useAsyncList } from '../../../hooks/useAsyncList'
import { useAsyncDetail } from '../../../hooks/useAsyncDetail'
import { clienteService } from '../services/clienteService'

export function useClientes() { return useAsyncList(clienteService.listar) }
export function useClienteDetalle(id: number | null) { return useAsyncDetail(id, clienteService.obtenerPorId) }
