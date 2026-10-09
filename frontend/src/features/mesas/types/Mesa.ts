export type EstadoMesa = 'DISPONIBLE' | 'OCUPADA'

export interface ZonaResumen {
  id: number
  nombre: string
  activa: boolean
}

export interface Mesa {
  id: number
  numero: number
  capacidad: number
  estado: EstadoMesa
  activa: boolean
  zona: ZonaResumen
}
