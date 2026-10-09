import type { Mesa } from '../../mesas/types/Mesa'
import type { TurnoOpcion } from '../types/ReservaFormData'

// Opciones ficticias. No son una consulta de disponibilidad para la fecha elegida.
export const mesasFormularioMock: Mesa[] = [
  { id: 1, numero: 1, capacidad: 4, estado: 'DISPONIBLE', activa: true, zona: { id: 1, nombre: 'Terraza', activa: true } },
  { id: 3, numero: 2, capacidad: 4, estado: 'DISPONIBLE', activa: true, zona: { id: 1, nombre: 'Terraza', activa: true } },
  { id: 5, numero: 3, capacidad: 6, estado: 'DISPONIBLE', activa: true, zona: { id: 2, nombre: 'Salón', activa: true } },
]

export const turnosFormularioMock: TurnoOpcion[] = [
  { id: 1, nombre: 'Almuerzo · 12:00–15:30', activo: true },
  { id: 2, nombre: 'Cena · 19:00–22:00', activo: true },
]
