import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it, vi } from 'vitest'
import ClienteForm from './components/ClienteForm'
import ClientesPage from './pages/ClientesPage'
import { clienteService } from './services/clienteService'
import { ApiError } from '../../api/httpClient'
import { clientes } from '../../test/fixtures'

it('formulario inválido muestra errores sin crear un cliente', async () => {
  const create = vi.spyOn(clienteService, 'crear')
  render(<ClienteForm onSaved={vi.fn()} onCancelEdit={vi.fn()} onBusyChange={vi.fn()} />)
  await userEvent.setup().click(screen.getByRole('button', { name: 'Guardar cliente' }))
  expect(screen.getByText('El nombre es obligatorio.')).toBeVisible()
  expect(screen.getByText('El email es obligatorio.')).toBeVisible()
  expect(create).not.toHaveBeenCalled()
})
it('guardar normaliza el DTO y usa la identidad devuelta por la API', async () => {
  const create = vi.spyOn(clienteService, 'crear').mockResolvedValue(clientes[0])
  const saved = vi.fn(); render(<ClienteForm onSaved={saved} onCancelEdit={vi.fn()} onBusyChange={vi.fn()} />)
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Nombre completo'), ' Ana Torres ')
  await user.type(screen.getByLabelText('Email', { exact: true }), 'ANA@example.test')
  await user.click(screen.getByRole('button', { name: 'Guardar cliente' }))
  await waitFor(() => expect(saved).toHaveBeenCalledWith(clientes[0], 'create'))
  expect(create).toHaveBeenCalledWith({ nombre: 'Ana Torres', email: 'ana@example.test', telefono: null }, expect.any(AbortSignal))
})
it('un 409 mantiene el formulario y no comunica éxito', async () => {
  vi.spyOn(clienteService, 'actualizar').mockRejectedValue(new ApiError(409, 'El email ya existe'))
  const saved = vi.fn(); render(<ClienteForm cliente={clientes[0]} onSaved={saved} onCancelEdit={vi.fn()} onBusyChange={vi.fn()} />)
  const user = userEvent.setup(); await user.type(screen.getByLabelText('Teléfono (opcional)'), '77777777')
  await user.click(screen.getByRole('button', { name: 'Actualizar cliente' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('409')
  expect(screen.getByLabelText('Teléfono (opcional)')).toHaveValue('77777777')
  expect(saved).not.toHaveBeenCalled()
})
it('ClientesPage combina búsqueda y estado sin consultar por cada tecla', async () => {
  const list = vi.spyOn(clienteService, 'listar').mockResolvedValue(clientes)
  render(<ClientesPage />); const user = userEvent.setup()
  await screen.findByRole('table')
  await user.selectOptions(screen.getByLabelText('Estado del cliente'), 'todos')
  await user.type(screen.getByRole('searchbox', { name: 'Buscar clientes' }), ' BRUNO ')
  expect(screen.getByRole('table')).toHaveTextContent('Bruno Pérez')
  expect(screen.getByRole('table')).not.toHaveTextContent('Ana Torres')
  await user.selectOptions(screen.getByLabelText('Estado del cliente'), 'activos')
  expect(screen.queryByRole('table')).not.toBeInTheDocument()
  expect(screen.getByText(/No hay clientes/)).toBeVisible()
  expect(list).toHaveBeenCalledTimes(1)
})
