import { useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import MetricCard from './MetricCard'
import StatusBadge from './StatusBadge'
import SearchInput from './SearchInput'
import Pagination from './Pagination'
import Button from './Button'
import { usePagination } from '../../hooks/usePagination'

describe('Componentes visibles y controlados', () => {
  it('MetricCard muestra etiqueta, cero real y explicación', () => {
    render(<MetricCard label="Reservas activas" value={0} helper="No representa ocupación actual" />)
    expect(screen.getByRole('article', { name: 'Reservas activas' })).toHaveTextContent('0')
    expect(screen.getByText('No representa ocupación actual')).toBeVisible()
  })
  it('StatusBadge comunica el estado con texto', () => {
    render(<StatusBadge label="Cancelada" tone="danger" />)
    expect(screen.getByText('Cancelada')).toBeVisible()
  })
  it('SearchInput actualiza una búsqueda controlada', async () => {
    function Example() { const [value, setValue] = useState(''); return <SearchInput label="Buscar clientes" value={value} onChange={setValue} /> }
    render(<Example />)
    await userEvent.setup().type(screen.getByRole('searchbox', { name: 'Buscar clientes' }), 'ana')
    expect(screen.getByRole('searchbox')).toHaveValue('ana')
  })
  it('Button no envía ni ejecuta el clic cuando está cargando', async () => {
    const onClick = vi.fn()
    render(<Button loading onClick={onClick}>Guardar</Button>)
    const button = screen.getByRole('button')
    expect(button).toHaveAttribute('type', 'button')
    await userEvent.setup().click(button)
    expect(button).toBeDisabled(); expect(onClick).not.toHaveBeenCalled()
  })
})

describe('Paginación controlada', () => {
  function Example({ total = 21 }: { total?: number }) { const pagination = usePagination(total); return <Pagination {...pagination} /> }
  it('avanza, llega al límite y retrocede', async () => {
    render(<Example />); const user = userEvent.setup()
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(screen.getByRole('status')).toHaveTextContent('11–20 de 21 resultados · Página 2 de 3')
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Anterior' }))
    expect(screen.getByRole('status')).toHaveTextContent('Página 2 de 3')
  })
  it('cambiar tamaño reinicia a 1', async () => {
    render(<Example />); const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    await user.selectOptions(screen.getByLabelText('Filas por página'), '5')
    expect(screen.getByRole('status')).toHaveTextContent('1–5 de 21 resultados · Página 1 de 5')
  })
  it('ajusta la página cuando se elimina la última fila sin saltar al crecer', async () => {
    const view = render(<Example />); const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    view.rerender(<Example total={20} />)
    expect(screen.getByRole('status')).toHaveTextContent('Página 2 de 2')
    view.rerender(<Example total={21} />)
    expect(screen.getByRole('status')).toHaveTextContent('Página 2 de 3')
  })
  it('el vacío tiene rango cero y controles deshabilitados', () => {
    render(<Example total={0} />)
    expect(screen.getByRole('status')).toHaveTextContent('0–0 de 0 resultados · Página 1 de 1')
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled()
  })
})
