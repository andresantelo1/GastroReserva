import { useState, type ChangeEvent, type FormEvent } from 'react'
import FormField from '../../../components/common/FormField'
import { useSubmission } from '../../../hooks/useSubmission'
import { reservaService } from '../services/reservaService'
import type { Reserva } from '../models/Reserva'
import { initialReservaForm, type ReservaFormData } from '../types/ReservaFormData'
import { prepararReservaPayload, validarReserva, type OpcionesReserva, type ReservaFormErrors } from '../utils/reservaValidation'
interface Props extends OpcionesReserva { onCreated: (reserva: Reserva) => void; onBusyChange: (busy: boolean) => void }
export default function ReservaForm({ clientes, mesas, turnos, onCreated, onBusyChange }: Props) {
  const [formData, setFormData] = useState<ReservaFormData>(initialReservaForm), [errors, setErrors] = useState<ReservaFormErrors>({}), [mensaje, setMensaje] = useState('')
  const { busy, error, setError, run } = useSubmission(onBusyChange)
  const activos = clientes.filter((c) => c.activo)
  const mesasActivas = mesas.filter((m) => m.activa && m.zona.activa)
  const turnosActivos = turnos.filter((t) => t.activo)
  const sinOpciones = !activos.length || !mesasActivas.length || !turnosActivos.length
  const change = (event: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = event.currentTarget
    if (!Object.hasOwn(initialReservaForm, name)) return
    setFormData((prev) => ({ ...prev, [name]: value })); setMensaje('')
    setErrors((prev) => { const next = { ...prev }; delete next[name as keyof ReservaFormData]; return next })
  }
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (busy) return
    setMensaje(''); setError('')
    const validation = validarReserva(formData, { clientes, mesas, turnos }); setErrors(validation)
    if (Object.keys(validation).length) return
    void run((signal) => reservaService.crear(prepararReservaPayload(formData), signal), (creada) => {
      onCreated(creada); setFormData(initialReservaForm); setErrors({})
      setMensaje(`Reserva #${creada.id} creada correctamente. Estado: ${creada.estado}.`)
    }, (fields) => setErrors(Object.fromEntries(Object.entries(fields).filter(([key]) => Object.hasOwn(initialReservaForm, key)))))
  }
  const a11y = (key: keyof ReservaFormData) => ({ 'aria-invalid': Boolean(errors[key]), 'aria-describedby': errors[key] ? `reserva-${key}-error` : undefined })
  const limpiar = () => { setFormData(initialReservaForm); setErrors({}); setError(''); setMensaje('') }
  return <form id="reserva-form" className="entity-form" aria-labelledby="reserva-form-title" onSubmit={submit} noValidate aria-busy={busy}>
    <div className="form-intro"><p className="page-heading__eyebrow">Guía 07 / Reserva operativa</p><h3 id="reserva-form-title">Nueva reserva</h3><p>Elegís el cliente por su nombre, pero se envía su identificador numérico. Spring Boot comprueba actividad, capacidad y solapamientos. Una mesa en esta lista no garantiza disponibilidad.</p></div>
    {sinOpciones && <p className="form-error-summary" role="alert">Faltan clientes, mesas o turnos activos. Creá los catálogos necesarios y actualizá las opciones.</p>}
    <fieldset className="form-fields" disabled={busy}><legend className="sr-only">Datos de la reserva</legend><div className="form-grid">
      <FormField id="reserva-clienteId" label="Cliente" error={errors.clienteId}><select id="reserva-clienteId" name="clienteId" value={formData.clienteId} onChange={change} required {...a11y('clienteId')}><option value="">Seleccioná un cliente</option>{activos.map((c) => <option key={c.id} value={c.id}>{c.nombre} · {c.email}</option>)}</select></FormField>
      <FormField id="reserva-fecha" label="Fecha" error={errors.fecha}><input id="reserva-fecha" name="fecha" type="date" value={formData.fecha} onChange={change} required {...a11y('fecha')} /></FormField>
      <FormField id="reserva-turnoId" label="Turno" error={errors.turnoId}><select id="reserva-turnoId" name="turnoId" value={formData.turnoId} onChange={change} required {...a11y('turnoId')}><option value="">Seleccioná un turno</option>{turnosActivos.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}</select></FormField>
      <FormField id="reserva-mesaId" label="Mesa" error={errors.mesaId}><select id="reserva-mesaId" name="mesaId" value={formData.mesaId} onChange={change} required {...a11y('mesaId')}><option value="">Seleccioná una mesa</option>{mesasActivas.map((m) => <option key={m.id} value={m.id}>Mesa {m.numero} · {m.zona.nombre} · {m.capacidad} personas</option>)}</select></FormField>
      <FormField id="reserva-cantidadPersonas" label="Cantidad de personas" error={errors.cantidadPersonas}><input id="reserva-cantidadPersonas" name="cantidadPersonas" type="number" min="1" step="1" value={formData.cantidadPersonas} onChange={change} required {...a11y('cantidadPersonas')} /></FormField>
      <FormField id="reserva-observaciones" label="Observaciones (opcional)" error={errors.observaciones} wide><textarea id="reserva-observaciones" name="observaciones" value={formData.observaciones} onChange={change} maxLength={500} rows={3} {...a11y('observaciones')} /></FormField>
    </div></fieldset>
    {Object.keys(errors).length > 0 && <p className="form-error-summary" role="alert">{Object.keys(errors).length} campos con errores. Revisá los mensajes.</p>}
    {error && <p className="form-error-summary" role="alert">{error}</p>}
    {mensaje && <p className="form-success" role="status">{mensaje}</p>}
    <div className="form-actions"><button className="btn-secondary" type="button" onClick={limpiar} disabled={busy}>Limpiar</button><button className="btn-primary" type="submit" disabled={busy || sinOpciones}>{busy ? 'Guardando…' : 'Guardar reserva'}</button></div>
  </form>
}

