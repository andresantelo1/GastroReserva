// Proyección de ClienteResponse para el listado; no contiene credenciales.
// GastroReserva usa nombre completo y no expone apellido ni documento separados.
export interface Cliente {
  id: number
  nombre: string
  email: string
  telefono: string | null
  activo: boolean
}
