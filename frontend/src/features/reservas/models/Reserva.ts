export type EstadoReserva =
  | 'SOLICITADA'
  | 'CONFIRMADA'
  | 'SENTADA'
  | 'FINALIZADA'
  | 'CANCELADA'
  | 'NO_SHOW'

// Modelo de vista, no el DTO HTTP completo ni una entidad JPA.
// Al integrar la API: clienteId <- respuesta.cliente.id;
// mesaNumero <- respuesta.mesa.numero. Las fechas ISO locales no llevan zona UTC.
export interface Reserva {
  id: number
  clienteId: number
  clienteNombre?: string
  mesaNumero: number
  fecha: string
  inicio: string
  fin: string
  cantidadPersonas: number
  estado: EstadoReserva
  observaciones: string | null
  // Opcional sólo para los mocks históricos. La respuesta HTTP real debe incluirla.
  version?: number
}
