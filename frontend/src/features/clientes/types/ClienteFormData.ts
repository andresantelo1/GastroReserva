export interface ClienteFormData { nombre: string; email: string; telefono: string }
export interface ClienteCreatePayload { nombre: string; email: string; telefono: string | null }
export const initialClienteForm: ClienteFormData = { nombre: '', email: '', telefono: '' }

