import test from 'node:test'
import assert from 'node:assert/strict'
import { searchClientes } from '../src/features/clientes/utils/clienteSearch.ts'
import { searchReservas } from '../src/features/reservas/utils/reservaSearch.ts'
import { paginationRange } from '../src/utils/pagination.ts'
const clientes = [
  { id: 1, nombre: 'Ana López', email: 'ana@example.test', telefono: null, activo: true },
  { id: 2, nombre: 'Bruno', email: 'ventas@example.test', telefono: '70009999', activo: false },
  { id: 3, nombre: 'Ana María', email: 'maria@example.test', telefono: '70001234', activo: false },
]
const reservas = [
  { id: 11, clienteId: 1, mesaNumero: 8, fecha: '2030-01-15', estado: 'SOLICITADA', observaciones: 'Cumpleaños' },
  { id: 12, clienteId: 2, mesaNumero: 9, fecha: '2030-01-16', estado: 'CANCELADA', observaciones: null },
  { id: 13, clienteId: 99, clienteNombre: 'Celia', mesaNumero: 8, fecha: '2030-01-17', estado: 'CONFIRMADA', observaciones: null },
]
test('Cliente: texto insensible a mayúsculas y espacios, combinado con actividad', () => {
  assert.deepEqual(searchClientes(clientes, '  ANA  ', 'activos').map(c => c.id), [1])
  assert.deepEqual(searchClientes(clientes, 'ana', 'inactivos').map(c => c.id), [3])
  assert.equal(searchClientes(clientes, 'ana', 'todos').length, 2)
})
test('Cliente: email, teléfono, id y null sin inventar apellido/documento', () => {
  for (const term of ['VENTAS', '9999', '2']) assert.equal(searchClientes(clientes, term, 'todos')[0].id, 2)
  assert.equal(searchClientes(clientes, 'no existe', 'todos').length, 0)
  assert.equal(searchClientes(clientes, ' ', 'todos').length, 3)
})
test('Reserva: búsqueda por nombre relacionado, cliente numérico y estado con AND', () => {
  assert.deepEqual(searchReservas(reservas, clientes, 'ANA', '1', 'SOLICITADA').map(r => r.id), [11])
  assert.equal(searchReservas(reservas, clientes, 'ana', '2', '').length, 0)
  assert.equal(searchReservas(reservas, clientes, 'ana', '1', 'CANCELADA').length, 0)
})
test('Reserva: id, mesa, fecha ISO/visible y observaciones', () => {
  for (const term of ['11', '2030-01-15', '15/01/2030', 'CUMPLE']) assert.equal(searchReservas(reservas, clientes, term, '', '')[0].id, 11)
  assert.equal(searchReservas(reservas, clientes, '8', '', '').length, 2)
})
test('Reserva: fallback para mesero y relación ausente, sin catálogo ni HTTP', () => {
  assert.equal(searchReservas(reservas, [], 'celia', '', '')[0].id, 13)
  assert.equal(searchReservas(reservas, [], '', '99', 'CONFIRMADA')[0].id, 13)
  assert.equal(searchReservas(reservas, clientes, '', 'NaN', '').length, 0)
})
test('Los filtros no mutan listas ni registros', () => {
  const before = JSON.stringify({ clientes, reservas })
  searchClientes(clientes, 'a', 'todos'); searchReservas(reservas, clientes, 'a', '', '')
  assert.equal(JSON.stringify({ clientes, reservas }), before)
})
test('Paginación vacía: página 1/1 y rango 0–0', () => {
  assert.deepEqual(paginationRange(0, 4, 10), { page: 1, totalPages: 1, start: 0, end: 0, from: 0, total: 0 })
})
test('Paginación exacta y última página parcial para 5, 10, 20', () => {
  for (const size of [5, 10, 20]) {
    assert.equal(paginationRange(size * 2, 2, size).totalPages, 2)
    const last = paginationRange(size * 2 + 1, 3, size)
    assert.equal(last.end - last.start, 1)
    assert.equal(last.from, size * 2 + 1)
  }
})
test('Nunca permite página cero, negativa, NaN o superior al total', () => {
  for (const page of [0, -1, NaN, 1.5]) assert.equal(paginationRange(21, page, 10).page, 1)
  assert.equal(paginationRange(21, 999, 10).page, 3)
})
test('Al quitar la última fila de página 3 se vuelve a página 2', () => {
  assert.equal(paginationRange(11, 3, 5).page, 3)
  assert.equal(paginationRange(10, 3, 5).page, 2)
})
test('Se filtra antes de paginar y aparecen coincidencias fuera de la primera página original', () => {
  const data = Array.from({length: 21}, (_, i) => ({ ...clientes[0], id: i + 1, nombre: i < 15 ? 'Bruno' : 'Ana', email: `prueba${i}@example.test` }))
  const filtered = searchClientes(data, 'ana', 'todos')
  const p = paginationRange(filtered.length, 1, 5)
  assert.deepEqual(filtered.slice(p.start, p.end).map(c => c.id), [16, 17, 18, 19, 20])
  assert.equal(p.totalPages, 2)
})
test('Tamaños/cantidades inválidos se rechazan, no dividen por cero', () => {
  for (const size of [0, -1, NaN, 1.5]) assert.throws(() => paginationRange(3, 1, size))
  assert.throws(() => paginationRange(-1, 1, 10))
})
