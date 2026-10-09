import { useEffect, useId, useRef, type ReactNode, type RefObject } from 'react'
import Button from './Button'

export interface ModalProps {
  open: boolean; title: string; children: ReactNode; onClose: () => void; busy?: boolean
  descriptionId?: string; fallbackFocusRef?: RefObject<HTMLElement | null>; initialFocusRef?: RefObject<HTMLElement | null>
}
export default function Modal({ open, title, children, onClose, busy = false, descriptionId, fallbackFocusRef, initialFocusRef }: ModalProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  useEffect(() => {
    const dialog = ref.current
    if (!dialog || !open) return
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const fallback = fallbackFocusRef?.current
    dialog.showModal()
    initialFocusRef?.current?.focus()
    return () => {
      if (dialog.open) dialog.close()
      // Esperar a que React retire disabled; si se eliminó el disparador,
      // volver a un control existente de la página. No robar foco a otro modal.
      requestAnimationFrame(() => {
        if (document.querySelector('dialog[open]')) return
        const target = opener?.isConnected && !opener.matches(':disabled') ? opener : fallback
        if (target?.isConnected && !target.matches(':disabled')) target.focus()
      })
    }
  }, [open, fallbackFocusRef, initialFocusRef])
  return <dialog ref={ref} className="ui-modal" aria-labelledby={titleId} aria-describedby={descriptionId} aria-busy={busy || undefined}
    onCancel={(e) => { e.preventDefault(); if (!busy) onClose() }}>
    <header className="ui-modal__header"><h2 id={titleId}>{title}</h2>
      <Button variant="ghost" aria-label="Cerrar" disabled={busy} onClick={onClose}>×</Button>
    </header>
    {children}
  </dialog>
}
