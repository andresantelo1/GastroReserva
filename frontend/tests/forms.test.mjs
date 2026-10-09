import test from 'node:test'
import assert from 'node:assert/strict'
import { validarCliente, prepararClientePayload } from '../src/features/clientes/utils/clienteValidation.ts'
import { validarReserva, prepararReservaPayload } from '../src/features/reservas/utils/reservaValidation.ts'
import { initialClienteForm } from '../src/features/clientes/types/ClienteFormData.ts'
import { initialReservaForm } from '../src/features/reservas/types/ReservaFormData.ts'
import { clientesMock } from '../src/features/clientes/data/clientes.mock.ts'
import { mesasFormularioMock, turnosFormularioMock } from '../src/features/reservas/data/opcionesReserva.mock.ts'

const cliente = { nombre: '  Ana Prueba  ', email: '  ANA@EXAMPLE.COM  ', telefono: ' 70010001 ', activo: false, documento: ' ABC12 ', direccion: ' Calle de ejemplo 123 ' }
const reserva = { clienteId: '1', fecha: '2030-01-15', turnoId: '1', mesaId: '3', cantidadPersonas: '4', observaciones: ' Cerca de la ventana ', referencia: ' visita-ana ' }
const opciones = { clientes: clientesMock, mesas: mesasFormularioMock, turnos: turnosFormularioMock }

test('Cliente vacío: dos campos obligatorios según el contrato real', () => {
  assert.deepEqual(Object.keys(validarCliente(initialClienteForm)).sort(), ['email', 'nombre'])
})
test('Cliente válido: trim, email minúsculo y sin mutar estado ni enviar campos de práctica', () => {
  const antes = { ...cliente }
  assert.deepEqual(validarCliente(cliente), {})
  assert.deepEqual(prepararClientePayload(cliente), { nombre: 'Ana Prueba', email: 'ana@example.com', telefono: '70010001' })
  assert.deepEqual(cliente, antes)
})
test('Cliente: espacios no cuentan como nombre ni email', () => {
  const errores = validarCliente({ ...cliente, nombre: '   ', email: '    ' })
  assert.ok(errores.nombre && errores.email)
})
test('Cliente: email incorrecto y máximo de caracteres', () => {
  for (const email of ['ana', 'ana@', 'ana @example.com', 'a'.repeat(250) + '@example.com']) assert.ok(validarCliente({ ...cliente, email }).email)
  assert.ok(validarCliente({ ...cliente, nombre: 'a'.repeat(121) }).nombre)
})
test('Cliente: teléfono opcional, mínimo de siete dígitos y máximo de treinta caracteres', () => {
  assert.deepEqual(validarCliente({ ...cliente, telefono: ' ' }), {})
  assert.equal(prepararClientePayload({ ...cliente, telefono: ' ' }).telefono, null)
  for (const telefono of ['abc', '123456', '1'.repeat(31)]) assert.ok(validarCliente({ ...cliente, telefono }).telefono)
  assert.equal(validarCliente({ ...cliente, telefono: '1234567' }).telefono, undefined)
})
test('Cliente: no exige ni envía los campos de práctica retirados', () => {
  const real = { nombre: 'Ana', email: 'ana@example.com', telefono: '' }
  assert.deepEqual(validarCliente(real), {})
  assert.deepEqual(Object.keys(prepararClientePayload(real)), ['nombre','email','telefono'])
})
test('Reserva vacía: cinco errores, no convertir vacío a cero como dato válido', () => {
  assert.equal(Object.keys(validarReserva(initialReservaForm, opciones)).length, 5)
})
test('Reserva válida: payload de la API, ids numéricos, sin estado ni referencia local', () => {
  const antes = { ...reserva }
  assert.deepEqual(validarReserva(reserva, opciones), {})
  assert.deepEqual(prepararReservaPayload(reserva), { clienteId: 1, fecha: '2030-01-15', turnoId: 1, mesaId: 3, cantidadPersonas: 4, observaciones: 'Cerca de la ventana' })
  assert.equal(mesasFormularioMock.find(m=>m.id===3).numero, 2)
  assert.deepEqual(reserva, antes)
})
test('Reserva: sólo clientes activos existentes', () => {
  for (const clienteId of ['', ' ', '999', '3', '6', 'NaN', '-1', '1.5']) assert.ok(validarReserva({ ...reserva, clienteId }, opciones).clienteId)
})
test('Reserva: turno y mesa válidos, rechazo de opciones ausentes o inactivas', () => {
  assert.ok(validarReserva({ ...reserva, turnoId: '999' }, opciones).turnoId)
  assert.ok(validarReserva({ ...reserva, mesaId: '999' }, opciones).mesaId)
  assert.ok(validarReserva(reserva, { ...opciones, mesas: mesasFormularioMock.map(m=>({...m, activa:false})) }).mesaId)
  assert.ok(validarReserva(reserva, { ...opciones, mesas: mesasFormularioMock.map(m=>({...m, zona:{...m.zona,activa:false}})) }).mesaId)
  assert.ok(validarReserva(reserva, { ...opciones, turnos: turnosFormularioMock.map(t=>({...t,activo:false})) }).turnoId)
})
test('Reserva: fechas reales, incluidos años bisiestos', () => {
  for (const fecha of ['', '2030-02-30', '2030-02-29', '2030-13-01', '15/01/2030', '0000-01-01']) assert.ok(validarReserva({ ...reserva, fecha }, opciones).fecha)
  assert.equal(validarReserva({ ...reserva, fecha: '2032-02-29' }, opciones).fecha, undefined)
})
test('Reserva: cantidad entera, positiva y dentro de la capacidad de la mesa de ejemplo', () => {
  for (const cantidadPersonas of ['', ' ', '0', '-1', '2.5', '5', 'abc', 'Infinity', '1e1', '9999999999999999', '2147483648']) assert.ok(validarReserva({ ...reserva, cantidadPersonas }, opciones).cantidadPersonas)
  assert.equal(validarReserva({ ...reserva, cantidadPersonas: '1' }, opciones).cantidadPersonas, undefined)
  assert.equal(validarReserva({ ...reserva, mesaId: '5', cantidadPersonas: '6' }, opciones).cantidadPersonas, undefined)
})
test('Reserva: observaciones opcionales, límite 500 y sin cambiar sus mayúsculas', () => {
  assert.equal(prepararReservaPayload({ ...reserva, observaciones: ' ' }).observaciones, null)
  assert.equal(validarReserva({ ...reserva, observaciones: 'a'.repeat(500) }, opciones).observaciones, undefined)
  assert.ok(validarReserva({ ...reserva, observaciones: 'a'.repeat(501) }, opciones).observaciones)
})
test('Reserva: referencia de práctica retirada del formulario y del contrato', () => {
  assert.equal('referencia' in prepararReservaPayload(reserva), false)
  const { referencia, ...real } = reserva
  assert.ok(referencia)
  assert.deepEqual(validarReserva(real, opciones), {})
})
test('Los formularios no alteran los mocks de la guía 03', () => {
  assert.equal(clientesMock.length, 6)
  assert.equal(clientesMock.filter(c=>c.activo).length, 4)
  assert.equal(initialClienteForm.nombre, '')
  assert.equal(initialReservaForm.clienteId, '')
})
