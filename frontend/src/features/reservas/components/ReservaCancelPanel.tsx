import { useState, type RefObject } from 'react'
import ConfirmDialog from '../../../components/ui/ConfirmDialog'
import { useSubmission } from '../../../hooks/useSubmission'
import type { Reserva } from '../models/Reserva'
import { reservaService } from '../services/reservaService'
export default function ReservaCancelPanel({ reserva, onSaved, onClose, onBusyChange, fallbackFocusRef }: {
  reserva: Reserva; onSaved: (r: Reserva) => void; onClose: () => void; onBusyChange: (busy: boolean) => void; fallbackFocusRef?: RefObject<HTMLElement | null>
}) {
  const [motivo, setMotivo] = useState('')
  const submission = useSubmission(onBusyChange)
  const submit = () => {
    if (submission.busy) return
    if (!motivo.trim() || motivo.length > 500) { submission.setError('Indicá un motivo de 1 a 500 caracteres.'); return }
    void submission.run((signal) => reservaService.cancelar(reserva.id, motivo.trim(), signal), onSaved)
  }
  return <ConfirmDialog open title={`¿Cancelar reserva #${reserva.id}?`}
    message={`Cliente #${reserva.clienteId}, ${reserva.fecha}, mesa ${reserva.mesaNumero}. Se libera su capacidad, pero la reserva y su historial se conservan. CANCELADA es un estado final: no se reactiva.`}
    confirming={submission.busy} error={submission.error} onConfirm={submit} onCancel={onClose} fallbackFocusRef={fallbackFocusRef}
    confirmLabel="Confirmar cancelación" cancelLabel="Volver sin cancelar">
    <div className="form-field"><label htmlFor="cancel-motivo">Motivo de cancelación</label><textarea id="cancel-motivo" value={motivo} onChange={(e) => setMotivo(e.target.value)} maxLength={500} disabled={submission.busy} rows={3} required /></div>
  </ConfirmDialog>
}
