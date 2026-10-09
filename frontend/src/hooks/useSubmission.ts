import { useEffect, useRef, useState } from 'react'
import { ApiError, esCancelacion, mensajeError } from '../api/httpClient'
export function useSubmission(onBusyChange?: (busy: boolean) => void) {
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  const controllerRef = useRef<AbortController | null>(null)
  useEffect(() => () => { controllerRef.current?.abort(); controllerRef.current = null }, [])
  async function run<T>(operation: (signal: AbortSignal) => Promise<T>, onSuccess: (value: T) => void, onFields?: (fields: Record<string, string>) => void) {
    if (controllerRef.current) return
    const controller = new AbortController(); controllerRef.current = controller
    setBusy(true); setError(''); onBusyChange?.(true)
    try { const value = await operation(controller.signal); if (!controller.signal.aborted) onSuccess(value) }
    catch (err) {
      if (!controller.signal.aborted && !esCancelacion(err)) { setError(mensajeError(err)); if (err instanceof ApiError) onFields?.(err.fieldErrors) }
    } finally {
      if (controllerRef.current === controller) controllerRef.current = null
      if (!controller.signal.aborted) { setBusy(false); onBusyChange?.(false) }
    }
  }
  return { busy, error, setError, run }
}

