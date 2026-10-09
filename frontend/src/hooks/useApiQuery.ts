import { useCallback, useEffect, useRef, useState, type SetStateAction } from 'react'
import { startAsyncQuery, type QueryLoader } from './asyncQuery'

interface Snapshot<T> {
  source: QueryLoader<T> | null
  revision: number
  data: T
  loading: boolean
  error: string
}

// loader estable; null significa que no hay recurso seleccionado.
// initial es el valor vacío, no una dependencia que dispare peticiones.
export function useApiQuery<T>(loader: QueryLoader<T> | null, initial: T) {
  const [empty] = useState(() => initial)
  const [revision, setRevision] = useState(0)
  const [state, setState] = useState<Snapshot<T>>(() => ({ source: loader, revision: 0, data: initial, loading: loader !== null, error: '' }))
  const current = useRef<ReturnType<typeof startAsyncQuery<T>> | null>(null)
  const pending = state.source !== loader || state.revision !== revision
  // Ajuste condicionado al cambio de identidad: evita pintar un recurso viejo
  // y no necesita un Effect adicional para copiar/resetear datos derivados.
  if (pending) setState({ source: loader, revision, data: empty, loading: loader !== null, error: '' })

  useEffect(() => {
    if (!loader) return
    const query = startAsyncQuery(loader, {
      start: () => undefined,
      success: (data) => setState((prev) => ({ ...prev, data })),
      error: (error) => setState((prev) => ({ ...prev, error })),
      finish: () => setState((prev) => ({ ...prev, loading: false })),
    })
    current.current = query
    return () => {
      query.cancel()
      if (current.current === query) current.current = null
    }
  }, [loader, revision])

  const reload = useCallback(() => {
    // Invalida el GET anterior inmediatamente, antes del próximo Effect.
    current.current?.cancel()
    setRevision((value) => value + 1)
  }, [])
  const setData = useCallback((update: SetStateAction<T>) => {
    setState((prev) => ({ ...prev, data: typeof update === 'function' ? (update as (value: T) => T)(prev.data) : update }))
  }, [])
  const setError = useCallback((error: string) => setState((prev) => ({ ...prev, error })), [])
  return {
    // No mostrar el detalle anterior en el render previo al cleanup.
    data: !loader || pending || state.loading ? empty : state.data,
    loading: loader !== null && (pending || state.loading),
    error: !loader || pending ? '' : state.error,
    setData, setError, reload,
  }
}

