import { useState, type FormEvent } from 'react'
import FormField from '../../../components/common/FormField'
import { useSubmission } from '../../../hooks/useSubmission'
import type { Cliente } from '../../clientes/models/Cliente'
import type { Reserva } from '../models/Reserva'
import { reservaService } from '../services/reservaService'
import type { ReservaClienteFormData } from '../types/ReservaClienteRequest'
import { formularioDeReserva, prepararCorreccion, validarCorreccion, puedeCorregir } from '../utils/reservaCrud'

interface Props { reserva: Reserva; clientes: Cliente[]; onSaved: (r: Reserva) => void; onCancel: () => void; onBusyChange: (busy: boolean) => void }
export default function ReservaClienteForm({ reserva, clientes, onSaved, onCancel, onBusyChange }: Props) {
  const [data, setData] = useState(() => formularioDeReserva(reserva))
  const [errors, setErrors] = useState<Partial<Record<keyof ReservaClienteFormData, string>>>({})
  const submission = useSubmission(onBusyChange)
  const activos = clientes.filter((c) => c.activo)
  const elegible = puedeCorregir(reserva) && reserva.version !== undefined
  const change = (key: keyof ReservaClienteFormData, value: string) => {
    setData((prev) => ({ ...prev, [key]: value }))
    setErrors((prev) => ({ ...prev, [key]: undefined }))
  }
  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (submission.busy || !elegible || reserva.version === undefined) return
    submission.setError('')
    const issues = validarCorreccion(data, clientes); setErrors(issues)
    if (Object.keys(issues).length) return
    const payload = prepararCorreccion(data, reserva.version)
    void submission.run((signal) => reservaService.actualizarCliente(reserva.id, payload, signal), onSaved,
      (fields) => setErrors(Object.fromEntries(Object.entries(fields).filter(([key]) => ['clienteId','observaciones','motivo'].includes(key)))))
  }
  const a11y = (key: keyof ReservaClienteFormData) => ({ 'aria-invalid': Boolean(errors[key]), 'aria-describedby': errors[key] ? `correccion-${key}-error` : undefined })
  return <form id="reserva-form" className="entity-form" onSubmit={submit} noValidate aria-busy={submission.busy} aria-labelledby="correccion-title">
    <div className="form-intro"><p className="page-heading__eyebrow">Guía 07 / Relación Cliente → Reservas</p><h3 id="correccion-title">Editar reserva #{reserva.id}</h3>
      <p>Corrección administrativa de cliente y observaciones, sólo en estado SOLICITADA. El cambio de cliente transfiere la reserva a su perfil; el anterior deja de verla como propia. Se registran ambas referencias, el motivo y el responsable.</p>
      <p>Se conservan: {reserva.fecha}, {reserva.inicio.slice(11,16)}–{reserva.fin.slice(11,16)}, mesa {reserva.mesaNumero}, {reserva.cantidadPersonas} personas. Este formulario no reprograma ni cambia el estado.</p></div>
    {!elegible && <p role="alert" className="form-error-summary">Esta reserva ya no admite correcciones. Cerrá el formulario y actualizá la agenda.</p>}
    <fieldset className="form-fields" disabled={submission.busy || !elegible}><legend className="sr-only">Corrección de reserva</legend><div className="form-grid">
      <FormField id="correccion-clienteId" label="Cliente de la reserva" error={errors.clienteId}>
        <select autoFocus id="correccion-clienteId" value={data.clienteId} onChange={(e) => change('clienteId',e.target.value)} required {...a11y('clienteId')}>
          <option value="">Seleccioná un cliente activo</option>
          {!activos.some((c) => String(c.id) === data.clienteId) && data.clienteId !== '' && <option value={data.clienteId} disabled>Cliente #{data.clienteId} (inactivo o no disponible)</option>}
          {activos.map((c) => <option key={c.id} value={String(c.id)}>{c.nombre} · #{c.id} · {c.email}</option>)}
        </select>
      </FormField>
      <FormField id="correccion-observaciones" label="Observaciones (opcional)" error={errors.observaciones} wide><textarea id="correccion-observaciones" value={data.observaciones} maxLength={500} rows={2} onChange={(e) => change('observaciones',e.target.value)} {...a11y('observaciones')} /></FormField>
      <FormField id="correccion-motivo" label="Motivo de la corrección" error={errors.motivo} wide><textarea id="correccion-motivo" value={data.motivo} maxLength={500} required rows={2} onChange={(e) => change('motivo',e.target.value)} {...a11y('motivo')} /></FormField>
    </div></fieldset>
    {submission.error && <p className="form-error-summary" role="alert">{submission.error}</p>}
    <div className="form-actions"><button type="button" className="btn-secondary" disabled={submission.busy} onClick={onCancel}>Cancelar edición</button><button type="submit" className="btn-primary" disabled={submission.busy || !elegible || activos.length === 0}>{submission.busy ? 'Guardando…' : 'Actualizar reserva'}</button></div>
  </form>
}
