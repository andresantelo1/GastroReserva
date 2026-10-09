export interface ReservaClienteFormData {
  clienteId: string
  observaciones: string
  motivo: string
}
export interface ReservaClienteRequest {
  clienteId: number
  observaciones: string | null
  motivo: string
  version: number
}
export interface EventoReserva {
  id: number
  tipoEvento: string
  estadoAnterior: string | null
  estadoNuevo: string
  motivo: string | null
  cambiadoPorNombre: string
  creadoEn: string
  clienteAnteriorId: number | null
  clienteNuevoId: number | null
  observacionesAnteriores: string | null
  observacionesNuevas: string | null
}
