import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { expect, it, vi } from 'vitest'
import { clientes, reserva } from '../../test/fixtures'
import { dashboardMetrics } from './utils/dashboardMetrics'
import DashboardData from './components/DashboardData'
import DashboardPage from '../../pages/DashboardPage'
import { clienteService } from '../clientes/services/clienteService'
import { reservaService } from '../reservas/services/reservaService'
import { setSession } from '../auth/session'

it('deriva cuatro métricas y seis estados sin mutar la fuente', () => {
  const reservas = [reserva(1), reserva(2, 'CONFIRMADA'), reserva(3, 'SENTADA'), reserva(4, 'FINALIZADA'), reserva(5, 'CANCELADA'), reserva(6, 'NO_SHOW')]
  const before = JSON.stringify({ clientes, reservas })
  expect(dashboardMetrics(clientes, reservas)).toEqual({ clientesTotal: 2, clientesActivos: 1, reservasTotal: 6, reservasActivas: 3,
    porEstado: { SOLICITADA: 1, CONFIRMADA: 1, SENTADA: 1, FINALIZADA: 1, CANCELADA: 1, NO_SHOW: 1 } })
  expect(JSON.stringify({ clientes, reservas })).toBe(before)
})
it('muestra carga y después indicadores reales de los services', async () => {
  vi.spyOn(clienteService, 'listar').mockResolvedValue(clientes)
  vi.spyOn(reservaService, 'listar').mockResolvedValue([reserva(1)])
  render(<DashboardData />)
  expect(screen.getByRole('status')).toHaveTextContent('Cargando indicadores')
  expect(await screen.findByRole('article', { name: 'Clientes registrados' })).toHaveTextContent('2')
  expect(screen.getByRole('article', { name: 'Clientes activos' })).toHaveTextContent('1')
  expect(screen.getByRole('article', { name: 'Reservas registradas' })).toHaveTextContent('1')
  expect(screen.getByRole('article', { name: 'Reservas activas' })).toHaveTextContent('1')
  expect(clienteService.listar).toHaveBeenCalledTimes(1)
})
it('el error no se disfraza como cero y permite reintentar', async () => {
  vi.spyOn(clienteService, 'listar').mockRejectedValueOnce(new Error('API no disponible')).mockResolvedValue([])
  vi.spyOn(reservaService, 'listar').mockResolvedValue([])
  render(<DashboardData />)
  expect(await screen.findByRole('alert')).toHaveTextContent('API no disponible')
  expect(screen.queryByRole('article')).not.toBeInTheDocument()
  await userEvent.setup().click(screen.getByRole('button', { name: 'Reintentar carga' }))
  expect(await screen.findByText('Todavía no hay clientes ni reservas')).toBeVisible()
  expect(screen.getByRole('article', { name: 'Clientes registrados' })).toHaveTextContent('0')
})
it('sin sesión no consulta datos administrativos', () => {
  const clientesGet = vi.spyOn(clienteService, 'listar'); const reservasGet = vi.spyOn(reservaService, 'listar')
  render(<MemoryRouter><DashboardPage /></MemoryRouter>)
  expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible()
  expect(clientesGet).not.toHaveBeenCalled(); expect(reservasGet).not.toHaveBeenCalled()
})
it('MESERO no consulta el catálogo de clientes desde el dashboard', () => {
  setSession({ accessToken: 'test-only', tokenType: 'Bearer', expiresAt: '2099-01-01T00:00:00Z', usuario: { id: 1, nombre: 'Mesero de prueba', email: 'mesero@example.test', activo: true, rol: 'MESERO' } })
  const clientesGet = vi.spyOn(clienteService, 'listar')
  render(<MemoryRouter><DashboardPage /></MemoryRouter>)
  expect(screen.getByRole('alert')).toHaveTextContent('Acceso no disponible para tu rol')
  expect(clientesGet).not.toHaveBeenCalled()
})
