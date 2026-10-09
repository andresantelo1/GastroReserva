import type { ClienteCreatePayload } from './ClienteFormData'

export type ClienteCreateRequest = ClienteCreatePayload

// PUT exige la representación completa, incluyendo activo.
export interface ClienteUpdateRequest extends ClienteCreatePayload {
  activo: boolean
}
