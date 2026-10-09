import type { Cliente } from '../models/Cliente'

// Sólo datos ficticios para la guía 03: no se envían a PostgreSQL.
export const clientesMock: Cliente[] = [
  { id: 1, nombre: 'Ana Rojas', email: 'ana.rojas@example.com', telefono: '70010001', activo: true },
  { id: 2, nombre: 'Carlos Méndez', email: 'carlos.mendez@example.com', telefono: '70010002', activo: true },
  { id: 3, nombre: 'Lucía Vargas', email: 'lucia.vargas@example.com', telefono: null, activo: false },
  { id: 4, nombre: 'Diego Suárez', email: 'diego.suarez@example.com', telefono: '70010004', activo: true },
  // Práctica: dos clientes adicionales, uno activo y otro inactivo.
  { id: 5, nombre: 'Valentina Flores', email: 'valentina.flores@example.com', telefono: '70010005', activo: true },
  { id: 6, nombre: 'Mateo Cruz', email: 'mateo.cruz@example.com', telefono: '70010006', activo: false },
]
