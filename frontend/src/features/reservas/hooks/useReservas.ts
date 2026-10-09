import { useCallback } from 'react'
import { useApiQuery } from '../../../hooks/useApiQuery'
import { useAsyncDetail } from '../../../hooks/useAsyncDetail'
import { clienteService } from '../../clientes/services/clienteService'
import type { Cliente } from '../../clientes/models/Cliente'
import { reservaService } from '../services/reservaService'
import type { Reserva } from '../models/Reserva'

interface Agenda { reservas: Reserva[]; clientes: Cliente[] }
export function useReservas(puedeGestionar: boolean) {
  const loader = useCallback(async (signal: AbortSignal): Promise<Agenda> => {
    const [reservas, clientes] = await Promise.all([
      reservaService.listar(signal),
      puedeGestionar ? clienteService.listar(signal) : Promise.resolve([]),
    ])
    return { reservas, clientes }
  }, [puedeGestionar])
  return useApiQuery<Agenda>(loader, { reservas: [], clientes: [] })
}

export function useReservaDetalle(id: number | null, incluirHistorial: boolean) {
  const loader = useCallback(async (id: number, signal: AbortSignal) => {
    const [reserva, eventos] = await Promise.all([
      reservaService.obtenerPorId(id, signal),
      incluirHistorial ? reservaService.historial(id, signal) : Promise.resolve([]),
    ])
    return { reserva, eventos }
  }, [incluirHistorial])
  return useAsyncDetail(id, loader)
}
