import { apiFetch } from '../../../api/apiClient.ts'
import { booleano, contratoInvalido, lista, nullableTexto, numero, objeto, texto } from '../../../api/contracts.ts'
import type { Mesa } from '../../mesas/types/Mesa'
import type { EstadoReserva, Reserva } from '../models/Reserva'
import type { ReservaOperativaPayload, TurnoOpcion } from '../types/ReservaFormData'
import type { EventoReserva, ReservaClienteRequest } from '../types/ReservaClienteRequest'
const estados: EstadoReserva[] = ['SOLICITADA','CONFIRMADA','SENTADA','FINALIZADA','CANCELADA','NO_SHOW']
export function leerReserva(value: unknown): Reserva {
  const v = objeto(value), cliente = objeto(v.cliente), mesa = objeto(v.mesa)
  if (!estados.includes(v.estado as EstadoReserva)) throw contratoInvalido()
  if (typeof v.version !== 'number' || !Number.isSafeInteger(v.version) || v.version < 0) throw contratoInvalido()
  return { id: numero(v.id), clienteId: numero(cliente.id), clienteNombre: texto(cliente.nombre),
    mesaNumero: numero(mesa.numero), fecha: texto(v.fecha), inicio: texto(v.inicio), fin: texto(v.fin),
    cantidadPersonas: numero(v.cantidadPersonas), estado: v.estado as EstadoReserva, observaciones: nullableTexto(v.observaciones), version: v.version }
}
export function leerMesa(value: unknown): Mesa {
  const v = objeto(value), zona = objeto(v.zona)
  if (v.estado !== 'DISPONIBLE' && v.estado !== 'OCUPADA') throw contratoInvalido()
  return { id: numero(v.id), numero: numero(v.numero), capacidad: numero(v.capacidad), estado: v.estado, activa: booleano(v.activa),
    zona: { id: numero(zona.id), nombre: texto(zona.nombre), activa: booleano(zona.activa) } }
}
export function leerTurno(value: unknown): TurnoOpcion {
  const v = objeto(value)
  return { id: numero(v.id), nombre: `${texto(v.nombre)} · ${texto(v.horaInicio).slice(0,5)}–${texto(v.horaFin).slice(0,5)}`, activo: booleano(v.activo) }
}
export function leerEvento(value: unknown): EventoReserva {
  const v = objeto(value)
  return { id: numero(v.id), tipoEvento: texto(v.tipoEvento), estadoAnterior: nullableTexto(v.estadoAnterior), estadoNuevo: texto(v.estadoNuevo),
    motivo: nullableTexto(v.motivo), cambiadoPorNombre: texto(v.cambiadoPorNombre), creadoEn: texto(v.creadoEn),
    clienteAnteriorId: v.clienteAnteriorId == null ? null : numero(v.clienteAnteriorId), clienteNuevoId: v.clienteNuevoId == null ? null : numero(v.clienteNuevoId),
    observacionesAnteriores: nullableTexto(v.observacionesAnteriores ?? null), observacionesNuevas: nullableTexto(v.observacionesNuevas ?? null) }
}
function ruta(id: number) { if (!Number.isSafeInteger(id) || id <= 0) throw new Error('Identificador de reserva inválido.'); return `/reservas/${id}` }
export function createReservaService(request: typeof apiFetch = apiFetch) {
  return {
    async listar(signal?: AbortSignal) { return lista(await request<unknown>('/reservas', { signal }), leerReserva) },
    async obtenerPorId(id: number, signal?: AbortSignal) { return leerReserva(await request<unknown>(ruta(id), { signal })) },
    async crear(data: ReservaOperativaPayload, signal?: AbortSignal) { return leerReserva(await request<unknown>('/reservas/operativas', { method: 'POST', body: JSON.stringify(data), signal })) },
    async actualizarCliente(id: number, data: ReservaClienteRequest, signal?: AbortSignal) { return leerReserva(await request<unknown>(`${ruta(id)}/datos-cliente`, { method: 'PUT', body: JSON.stringify(data), signal })) },
    async cancelar(id: number, motivo: string, signal?: AbortSignal) { return leerReserva(await request<unknown>(`${ruta(id)}/estado`, { method: 'PATCH', body: JSON.stringify({ estado: 'CANCELADA', motivo }), signal })) },
    async historial(id: number, signal?: AbortSignal) { return lista(await request<unknown>(`${ruta(id)}/historial`, { signal }), leerEvento) },
    async mesas(signal?: AbortSignal) { return lista(await request<unknown>('/mesas?activa=true', { signal }), leerMesa) },
    async turnos(signal?: AbortSignal) { return lista(await request<unknown>('/turnos?activo=true', { signal }), leerTurno) },
  }
}
export const reservaService = createReservaService()

