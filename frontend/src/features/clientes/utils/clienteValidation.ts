import type { ClienteCreatePayload, ClienteFormData } from '../types/ClienteFormData'

export type ClienteFormErrors = Partial<Record<keyof ClienteFormData, string>>

export function validarCliente(data: ClienteFormData): ClienteFormErrors {
  const errors: ClienteFormErrors = {}
  if (!data.nombre.trim()) errors.nombre = 'El nombre es obligatorio.'
  else if (data.nombre.trim().length > 120) errors.nombre = 'El nombre no puede superar 120 caracteres.'

  const email = data.email.trim()
  if (!email) errors.email = 'El email es obligatorio.'
  else if (email.length > 255 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.email = 'Ingresá un email válido de hasta 255 caracteres.'
  }
  const telefono = data.telefono.trim()
  if (telefono && (telefono.replace(/\D/g, '').length < 7 || telefono.length > 30)) {
    errors.telefono = 'El teléfono debe tener al menos 7 dígitos y hasta 30 caracteres.'
  }
  return errors
}

// Llamar sólo después de validar; nunca expandir todo formData en el DTO.
export function prepararClientePayload(data: ClienteFormData): ClienteCreatePayload {
  return { nombre: data.nombre.trim(), email: data.email.trim().toLowerCase(), telefono: data.telefono.trim() || null }
}
