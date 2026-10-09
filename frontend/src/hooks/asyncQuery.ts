import { esCancelacion, mensajeError } from '../api/httpClient.ts'

export type QueryLoader<T> = (signal: AbortSignal) => Promise<T>
interface QueryObserver<T> {
  start: () => void
  success: (value: T) => void
  error: (message: string) => void
  finish: () => void
}

// Una ejecución por setup. Cancelar invalida incluso loaders que ignoran signal.
export function startAsyncQuery<T>(loader: QueryLoader<T>, observer: QueryObserver<T>) {
  const controller = new AbortController()
  let active = true
  observer.start()
  const done = (async () => {
    try {
      const value = await loader(controller.signal)
      if (active) observer.success(value)
    } catch (error) {
      if (active && !esCancelacion(error)) observer.error(mensajeError(error))
    } finally {
      if (active) observer.finish()
    }
  })()
  return { done, cancel: () => { active = false; controller.abort() } }
}
