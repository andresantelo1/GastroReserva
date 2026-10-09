import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it, vi } from 'vitest'
import AppErrorBoundary from './AppErrorBoundary'

it('no cambia el contenido de una pantalla sin error', () => {
  render(<AppErrorBoundary><p>Pantalla disponible</p></AppErrorBoundary>)
  expect(screen.getByText('Pantalla disponible')).toBeVisible()
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
})
it('captura un fallo de render, no expone su detalle y permite reintentar', async () => {
  const logging = vi.spyOn(console, 'error').mockImplementation(() => {})
  let fail = true
  function Broken() { if (fail) throw new Error('detalle técnico de prueba'); return <p>Recuperada</p> }
  render(<AppErrorBoundary><Broken /></AppErrorBoundary>)
  expect(screen.getByRole('alert')).toHaveTextContent('La interfaz tuvo un problema')
  expect(screen.queryByText('detalle técnico de prueba')).not.toBeInTheDocument()
  expect(logging).toHaveBeenCalled()
  fail = false
  await userEvent.setup().click(screen.getByRole('button', { name: 'Reintentar pantalla' }))
  expect(screen.getByText('Recuperada')).toBeVisible()
})
