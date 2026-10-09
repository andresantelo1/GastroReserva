import { useId, useRef, type ReactNode, type RefObject } from 'react'
import Button from './Button'
import Modal from './Modal'

export default function ConfirmDialog({ open, title, message, confirming = false, onConfirm, onCancel, error = '', children,
  confirmLabel = 'Confirmar', cancelLabel = 'Volver', fallbackFocusRef }: {
  open: boolean; title: string; message: string; confirming?: boolean; onConfirm: () => void; onCancel: () => void
  error?: string; children?: ReactNode; confirmLabel?: string; cancelLabel?: string; fallbackFocusRef?: RefObject<HTMLElement | null>
}) {
  const descriptionId = useId()
  const cancelRef = useRef<HTMLButtonElement>(null)
  return <Modal open={open} title={title} onClose={onCancel} busy={confirming} descriptionId={descriptionId} fallbackFocusRef={fallbackFocusRef} initialFocusRef={cancelRef}>
    <form noValidate onSubmit={(e) => { e.preventDefault(); if (!confirming) onConfirm() }}>
      <p id={descriptionId}>{message}</p>
      {children}
      {error && <p className="form-error-summary" role="alert">{error}</p>}
      {confirming && <p role="status">Procesando la acción. Esperá la respuesta antes de cerrar.</p>}
      <div className="ui-modal__actions">
        <Button ref={cancelRef} variant="secondary" autoFocus disabled={confirming} onClick={onCancel}>{cancelLabel}</Button>
        <Button variant="danger" type="submit" loading={confirming}>{confirmLabel}</Button>
      </div>
    </form>
  </Modal>
}
