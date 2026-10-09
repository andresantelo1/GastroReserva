import { useState } from 'react'
import { PAGE_SIZES, paginationRange, type PageSize } from '../utils/pagination'

export function usePagination(total: number) {
  const [requestedPage, setRequestedPage] = useState(1)
  const [pageSize, setSize] = useState<PageSize>(10)
  const range = paginationRange(total, requestedPage, pageSize)
  // Si desaparece la última fila de una página, conservar el nuevo límite
  // como selección evita volver a una página vieja al crecer la lista.
  if (requestedPage !== range.page) setRequestedPage(range.page)
  const resetPage = () => setRequestedPage(1)
  const onPageChange = (page: number) => setRequestedPage(paginationRange(total, page, pageSize).page)
  const onPageSizeChange = (size: number) => {
    if (!PAGE_SIZES.includes(size as PageSize)) return
    setSize(size as PageSize); resetPage()
  }
  return { ...range, pageSize, resetPage, onPageChange, onPageSizeChange }
}
