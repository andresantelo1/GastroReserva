export const PAGE_SIZES = [5, 10, 20] as const
export type PageSize = typeof PAGE_SIZES[number]
export function paginationRange(total: number, requestedPage: number, pageSize: number) {
  if (!Number.isSafeInteger(total) || total < 0 || !Number.isSafeInteger(pageSize) || pageSize < 1) throw new Error('Paginación inválida')
  const totalPages = Math.max(1, Math.ceil(total / pageSize))
  const page = Math.min(totalPages, Math.max(1, Number.isSafeInteger(requestedPage) ? requestedPage : 1))
  const start = (page - 1) * pageSize
  return { page, totalPages, start, end: Math.min(start + pageSize, total), from: total ? start + 1 : 0, total }
}
