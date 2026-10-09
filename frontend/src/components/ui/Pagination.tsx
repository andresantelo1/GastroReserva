import Button from './Button'
import SelectFilter from './SelectFilter'
import { PAGE_SIZES, paginationRange } from '../../utils/pagination'

export default function Pagination({ page, pageSize, total, onPageChange, onPageSizeChange, disabled = false, label = 'Paginación' }: {
  page: number; pageSize: number; total: number; onPageChange: (page: number) => void; onPageSizeChange: (size: number) => void; disabled?: boolean; label?: string
}) {
  const range = paginationRange(total, page, pageSize)
  return <nav className="ui-pagination" aria-label={label}>
    <SelectFilter label="Filas por página" value={String(pageSize)} options={PAGE_SIZES.map((size) => ({ value: String(size), label: String(size) }))}
      onChange={(size) => onPageSizeChange(Number(size))} disabled={disabled} />
    <p role="status" className="ui-pagination__summary">{range.from}–{range.end} de {total} resultados · Página {range.page} de {range.totalPages}</p>
    <div className="ui-pagination__actions">
      <Button variant="ghost" disabled={disabled || range.page <= 1} onClick={() => onPageChange(range.page - 1)}>Anterior</Button>
      <Button variant="ghost" disabled={disabled || range.page >= range.totalPages} onClick={() => onPageChange(range.page + 1)}>Siguiente</Button>
    </div>
  </nav>
}
