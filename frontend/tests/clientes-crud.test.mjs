import test from 'node:test'
import assert from 'node:assert/strict'
import { createClienteService } from '../src/features/clientes/services/clienteService.ts'
import { createApiClient, ApiError } from '../src/api/httpClient.ts'
import { formularioDesdeCliente, prepararClienteUpdate, guardarEnLista, marcarClienteInactivo, filtrarClientes } from '../src/features/clientes/utils/clienteCrud.ts'

const original = Object.freeze({ id: 7, nombre: 'Ana', email: 'ana@example.test', telefono: null, activo: true })
const otro = Object.freeze({ id: 8, nombre: 'Luis', email: 'luis@example.test', telefono: '70010009', activo: true })
const actual = { ...original, nombre: 'Ana editada' }

test('GET por id usa el recurso exacto y propaga AbortSignal', async () => {
  const controller = new AbortController()
  const service = createClienteService(async (url, options) => {
    assert.equal(url, '/clientes/7')
    assert.equal(options.signal, controller.signal)
    assert.equal(options.method, undefined)
    return original
  })
  assert.deepEqual(await service.obtenerPorId(7, controller.signal), original)
})

test('PUT manda el DTO completo sin id, usuario ni campos del ejemplo ParkFlow', async () => {
  const payload = prepararClienteUpdate({ nombre: ' Ana editada ', email: ' ANA@example.test ', telefono: '' }, false)
  const service = createClienteService(async (url, options) => {
    assert.equal(url, '/clientes/7')
    assert.equal(options.method, 'PUT')
    assert.deepEqual(JSON.parse(options.body), { nombre: 'Ana editada', email: 'ana@example.test', telefono: null, activo: false })
    return { ...actual, activo: false }
  })
  assert.equal((await service.actualizar(7, payload)).activo, false)
})

test('DELETE sin body acepta 204 y no intenta leer JSON', async () => {
  const transport = createApiClient({
    baseUrl: 'http://localhost:8080/api',
    fetchImpl: async (url, options) => {
      assert.equal(url, 'http://localhost:8080/api/clientes/7')
      assert.equal(options.method, 'DELETE')
      assert.equal(options.body, undefined)
      assert.equal(options.headers.has('Content-Type'), false)
      return new Response(null, { status: 204 })
    },
  })
  assert.equal(await createClienteService(transport).eliminar(7), undefined)
})

test('404 al cargar detalle se propaga, no produce un cliente ficticio', async () => {
  const service = createClienteService(async () => { throw new ApiError(404, 'No existe el cliente') })
  await assert.rejects(service.obtenerPorId(999), (error) => error.status === 404)
})

test('PUT y DELETE fallidos propagan 400/403/409/500, sin éxito falso', async () => {
  for (const status of [400, 403, 409, 500]) {
    const service = createClienteService(async () => { throw new ApiError(status, 'Rechazado') })
    await assert.rejects(service.actualizar(7, { nombre: 'Ana', email: 'ana@example.test', telefono: null, activo: true }), (error) => error.status === status)
    await assert.rejects(service.eliminar(7), (error) => error.status === status)
  }
})

test('Ids inválidos se rechazan antes de llamar al transporte', async () => {
  const service = createClienteService(async () => { assert.fail('No debe enviar HTTP') })
  for (const id of [0, -1, 1.5, NaN, Infinity, Number.MAX_SAFE_INTEGER + 1]) {
    await assert.rejects(service.obtenerPorId(id))
    await assert.rejects(service.actualizar(id, {}))
    await assert.rejects(service.eliminar(id))
  }
})

test('El formulario precarga una copia y transforma null a campo vacío', () => {
  const form = formularioDesdeCliente(original)
  assert.deepEqual(form, { nombre: 'Ana', email: 'ana@example.test', telefono: '' })
  form.nombre = 'Sin guardar'
  assert.equal(original.nombre, 'Ana')
  assert.deepEqual(formularioDesdeCliente(null), { nombre: '', email: '', telefono: '' })
})

test('Guardar edición reemplaza sólo el id seleccionado con la respuesta real', () => {
  const before = Object.freeze([original, otro])
  const result = guardarEnLista(before, actual, 'edit')
  assert.deepEqual(result, [actual, otro])
  assert.equal(result[1], otro)
  assert.equal(before[0], original)
})

test('Crear agrega la respuesta del backend sin duplicar un id repetido', () => {
  assert.deepEqual(guardarEnLista([original], otro, 'create'), [original, otro])
  assert.deepEqual(guardarEnLista([original], actual, 'create'), [actual])
})

test('Baja lógica conserva identidad/contacto, no altera otros ni muta el array', () => {
  const before = Object.freeze([original, otro])
  const after = marcarClienteInactivo(before, 7)
  assert.deepEqual(after[0], { ...original, activo: false })
  assert.equal(after[1], otro)
  assert.equal(before[0].activo, true)
  assert.deepEqual(marcarClienteInactivo(after, 7), after)
})

test('Vista Activos oculta la baja; Inactivos/Todos permiten recuperarla', () => {
  const all = marcarClienteInactivo([original, otro], 7)
  assert.deepEqual(filtrarClientes(all, 'activos'), [otro])
  assert.deepEqual(filtrarClientes(all, 'inactivos'), [{ ...original, activo: false }])
  assert.deepEqual(filtrarClientes(all, 'todos'), all)
})

test('Reactivar actualiza una fila sin inventar un id ni duplicar el cliente', () => {
  const all = marcarClienteInactivo([original, otro], 7)
  const reactivated = guardarEnLista(all, original, 'edit')
  assert.deepEqual(filtrarClientes(reactivated, 'activos'), [original, otro])
})
