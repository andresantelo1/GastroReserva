export interface ReservaFormData { clienteId: string; fecha: string; turnoId: string; mesaId: string; cantidadPersonas: string; observaciones: string }
export interface ReservaOperativaPayload { clienteId: number; fecha: string; turnoId: number; mesaId: number; cantidadPersonas: number; observaciones: string | null }
export interface TurnoOpcion { id: number; nombre: string; activo: boolean }
export const initialReservaForm: ReservaFormData = { clienteId: '', fecha: '', turnoId: '', mesaId: '', cantidadPersonas: '', observaciones: '' }

