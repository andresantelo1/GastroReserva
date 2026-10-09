import { useApiQuery } from './useApiQuery'
import type { QueryLoader } from './asyncQuery'

export function useAsyncList<T>(loader: QueryLoader<T[]>) {
  return useApiQuery<T[]>(loader, [])
}
