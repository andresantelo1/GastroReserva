import { useCallback } from 'react'
import { useApiQuery } from './useApiQuery'

export function useAsyncDetail<T>(id: number | null, loader: (id: number, signal: AbortSignal) => Promise<T>) {
  const load = useCallback((signal: AbortSignal) => loader(id!, signal), [id, loader])
  return useApiQuery<T | null>(id === null ? null : load, null)
}
