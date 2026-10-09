import { useState, type ChangeEvent, type FormEvent } from 'react'
import FormField from '../../../components/common/FormField'
import { useSubmission } from '../../../hooks/useSubmission'
import type { Cliente } from '../models/Cliente'
import { clienteService } from '../services/clienteService'
import { initialClienteForm, type ClienteFormData } from '../types/ClienteFormData'
import { prepararClientePayload, validarCliente, type ClienteFormErrors } from '../utils/clienteValidation'
import { formularioDesdeCliente, prepararClienteUpdate, type ClienteSaveMode } from '../utils/clienteCrud'

const campos = [
  { name: 'nombre', label: 'Nombre completo', type: 'text', max: 120 },
  { name: 'email', label: 'Email', type: 'email', max: 255 },
  { name: 'telefono', label: 'Teléfono (opcional)', type: 'tel', max: 30 },
] as const

interface Props {
  cliente?: Cliente | null
  onSaved: (cliente: Cliente, mode: ClienteSaveMode) => void
  onCancelEdit: () => void
  onBusyChange: (busy: boolean) => void
}

export default function ClienteForm({ cliente, onSaved, onCancelEdit, onBusyChange }: Props) {
  // La página usa key por modo/id: cada selección comienza con una copia fresca del GET.
  const [formData, setFormData] = useState<ClienteFormData>(() => formularioDesdeCliente(cliente))
  const [activo, setActivo] = useState(cliente?.activo ?? true)
  const [errors, setErrors] = useState<ClienteFormErrors>({})
  const { busy, error, setError, run } = useSubmission(onBusyChange)
  const isEditing = cliente != null

  const change = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.currentTarget
    if (!Object.hasOwn(initialClienteForm, name)) return
    setFormData((prev) => ({ ...prev, [name]: value }))
    setErrors((prev) => {
      const next = { ...prev }
      delete next[name as keyof ClienteFormData]
      return next
    })
  }

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (busy) return
    setError('')
    const validation = validarCliente(formData)
    setErrors(validation)
    if (Object.keys(validation).length) return
    void run(
      (signal) => cliente
        ? clienteService.actualizar(cliente.id, prepararClienteUpdate(formData, activo), signal)
        : clienteService.crear(prepararClientePayload(formData), signal),
      (saved) => {
        onSaved(saved, isEditing ? 'edit' : 'create')
      },
      (fields) => setErrors(Object.fromEntries(
        Object.entries(fields).filter(([key]) => Object.hasOwn(initialClienteForm, key)),
      )),
    )
  }

  const restablecer = () => {
    setFormData(formularioDesdeCliente(cliente))
    setActivo(cliente?.activo ?? true)
    setErrors({})
    setError('')
  }

  return <form id="cliente-form" className="entity-form" aria-labelledby="cliente-form-title" onSubmit={submit} noValidate aria-busy={busy}>
    <div className="form-intro">
      <p className="page-heading__eyebrow">Guía 06 / {isEditing ? 'PUT' : 'POST'} real</p>
      <h3 id="cliente-form-title">{cliente ? `Editar cliente #${cliente.id}` : 'Nuevo cliente'}</h3>
      <p>{isEditing
        ? 'Se actualiza este perfil. Si tiene una cuenta vinculada, el nombre y email también cambian en esa cuenta. Su contraseña no cambia.'
        : 'Guardar crea un perfil de cliente en PostgreSQL. No crea una cuenta ni una contraseña.'}</p>
    </div>
    <fieldset className="form-fields" disabled={busy}>
      <legend className="sr-only">Datos del cliente</legend>
      <div className="form-grid">
        {campos.map((campo) => {
          const id = `cliente-${campo.name}`
          return <FormField key={campo.name} id={id} label={campo.label} error={errors[campo.name]}>
            <input id={id} name={campo.name} type={campo.type} value={formData[campo.name]} onChange={change}
              maxLength={campo.max} required={campo.name !== 'telefono'} autoFocus={campo.name === 'nombre'}
              aria-invalid={Boolean(errors[campo.name])} aria-describedby={errors[campo.name] ? `${id}-error` : undefined} />
          </FormField>
        })}
        {isEditing && <div className="form-field">
          <label className="crud-checkbox"><input type="checkbox" checked={activo} onChange={(event) => setActivo(event.currentTarget.checked)} />Cliente activo</label>
          <p className="table-help">Inactivo impide nuevas reservas; conserva las anteriores. No desactiva su cuenta de acceso.</p>
        </div>}
      </div>
    </fieldset>
    {Object.keys(errors).length > 0 && <p className="form-error-summary" role="alert">{Object.keys(errors).length} campos con errores. Revisá los mensajes.</p>}
    {error && <p className="form-error-summary" role="alert">{error}</p>}
    <div className="form-actions">
      <button type="button" className="btn-secondary" onClick={restablecer} disabled={busy}>{isEditing ? 'Restablecer datos' : 'Limpiar'}</button>
      {isEditing && <button type="button" className="btn-secondary" onClick={onCancelEdit} disabled={busy}>Cancelar edición</button>}
      <button className="btn-primary" disabled={busy} type="submit">{busy ? (isEditing ? 'Actualizando…' : 'Guardando…') : isEditing ? 'Actualizar cliente' : 'Guardar cliente'}</button>
    </div>
  </form>
}
