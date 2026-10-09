import test from 'node:test'
import assert from 'node:assert/strict'
import { createReservaService, leerReserva, leerEvento } from '../src/features/reservas/services/reservaService.ts'
import { ApiError } from '../src/api/httpClient.ts'
import { formularioDeReserva, validarCorreccion, prepararCorreccion, guardarReserva, filtrarPorCliente, nombreDeCliente, puedeCorregir, puedeCancelar } from '../src/features/reservas/utils/reservaCrud.ts'

const dto = {id:1,cliente:{id:7,nombre:'Ana'},mesa:{id:40,numero:2},fecha:'2030-01-15',inicio:'2030-01-15T12:00:00',fin:'2030-01-15T15:00:00',cantidadPersonas:2,estado:'SOLICITADA',observaciones:null,version:0}
const reserva = Object.freeze(leerReserva(dto))
const clientes = [{id:7,nombre:'Ana',email:'ana@example.test',activo:true}, {id:8,nombre:'Luis',email:'luis@example.test',activo:true}, {id:9,nombre:'Inactivo',activo:false}]
test('Reserva GET por id exacto y cancelable, sin consultas por cliente',async()=>{
 let calls=0; const controller=new AbortController()
 const service=createReservaService(async(url,options)=>{calls++; assert.equal(url,'/reservas/1');assert.equal(options.signal,controller.signal);return dto})
 assert.deepEqual(await service.obtenerPorId(1,controller.signal),reserva);assert.equal(calls,1)
})
test('PUT de corrección envía referencia numérica, versión y sólo campos editables',async()=>{
 const payload=prepararCorreccion({clienteId:'8',observaciones:'  Nota  ',motivo:'  Corrección  '},0)
 const service=createReservaService(async(url,options)=>{
  assert.equal(url,'/reservas/1/datos-cliente');assert.equal(options.method,'PUT')
  assert.deepEqual(JSON.parse(options.body),{clienteId:8,observaciones:'Nota',motivo:'Corrección',version:0})
  return {...dto,cliente:{id:8,nombre:'Luis'},observaciones:'Nota',version:1}
 })
 assert.equal((await service.actualizarCliente(1,payload)).clienteId,8)
})
test('Cancelar usa PATCH de estado real, no DELETE inventado',async()=>{
 const service=createReservaService(async(url,options)=>{assert.equal(url,'/reservas/1/estado');assert.equal(options.method,'PATCH');assert.deepEqual(JSON.parse(options.body),{estado:'CANCELADA',motivo:'No asistirá'});return {...dto,estado:'CANCELADA',version:1}})
 assert.equal((await service.cancelar(1,'No asistirá')).estado,'CANCELADA')
})
test('GET/PUT/PATCH propagan 400,403,404,409,422 y 500 sin resultados ficticios',async()=>{
 for(const status of [400,403,404,409,422,500]){
  const service=createReservaService(async()=>{throw new ApiError(status,'Rechazado')})
  await assert.rejects(service.obtenerPorId(1),e=>e.status===status)
  await assert.rejects(service.actualizarCliente(1,{}),e=>e.status===status)
  await assert.rejects(service.cancelar(1,'Motivo'),e=>e.status===status)
 }
})
test('Rutas rechazan IDs inválidos antes de transmitir',async()=>{
 const service=createReservaService(async()=>assert.fail('No llamar HTTP'))
 for(const id of [0,-1,NaN,1.5,Infinity]) {await assert.rejects(service.obtenerPorId(id));await assert.rejects(service.actualizarCliente(id,{}));await assert.rejects(service.cancelar(id,''))}
})
test('La versión HTTP acepta cero y rechaza ausencia, texto y negativos',()=>{
 assert.equal(leerReserva(dto).version,0)
 for(const version of [undefined,'0',-1,0.5]) assert.throws(()=>leerReserva({...dto,version}),ApiError)
})
test('Formulario copia cliente como string, null como vacío, motivo nuevo',()=>{
 const data=formularioDeReserva(reserva);assert.deepEqual(data,{clienteId:'7',observaciones:'',motivo:''});data.clienteId='8';assert.equal(reserva.clienteId,7)
})
test('Validación bloquea referencia vacía, inexistente, inactiva, NaN y no entera',()=>{
 for(const clienteId of ['', '0','abc','99','9','7.5']) assert.ok(validarCorreccion({clienteId,observaciones:'',motivo:'Razón'},clientes).clienteId)
 assert.deepEqual(validarCorreccion({clienteId:'8',observaciones:'',motivo:'Razón'},clientes),{})
})
test('Motivo obligatorio y límites de observación',()=>{
 const errors=validarCorreccion({clienteId:'7',observaciones:'a'.repeat(501),motivo:' '},clientes)
 assert.ok(errors.motivo);assert.ok(errors.observaciones)
 assert.equal(prepararCorreccion({clienteId:'7',observaciones:' ',motivo:' Test '},2).observaciones,null)
})
test('Editar reemplaza una fila por respuesta y conserva otras e identidad',()=>{
 const otra={...reserva,id:2};const original=Object.freeze([reserva,otra]);const saved={...reserva,clienteId:8,version:1}
 const result=guardarReserva(original,saved,'edit');assert.equal(result.length,2);assert.equal(result[0],saved);assert.equal(result[1],otra);assert.equal(original[0].clienteId,7)
})
test('Cancelación conserva fila, relación e historial consultable por su id',()=>{
 const result=guardarReserva([reserva],{...reserva,estado:'CANCELADA',version:1},'edit')
 assert.equal(result.length,1);assert.equal(result[0].id,1);assert.equal(result[0].clienteId,7)
})
test('Filtro local convierte string y muestra cliente nuevo tras corrección',()=>{
 const rows=[reserva,{...reserva,id:2,clienteId:8}]
 assert.equal(filtrarPorCliente(rows,'').length,2);assert.equal(filtrarPorCliente(rows,'8')[0].id,2);assert.deepEqual(filtrarPorCliente(rows,'9'),[])
 const updated=guardarReserva(rows,{...reserva,clienteId:8},'edit');assert.equal(filtrarPorCliente(updated,'7').length,0);assert.equal(filtrarPorCliente(updated,'8').length,2)
})
test('Map resuelve nombre y fallback sin lanzar GET por fila',()=>{
 assert.equal(nombreDeCliente(reserva,new Map([[7,{nombre:'Ana actual'}]])),'Ana actual')
 assert.equal(nombreDeCliente(reserva,new Map()),'Ana')
 assert.equal(nombreDeCliente({...reserva,clienteNombre:undefined},new Map()),'Cliente #7')
})
test('Acciones coherentes con estados y auditoría de corrección proyectada',()=>{
 for(const estado of ['SOLICITADA','CONFIRMADA','SENTADA','FINALIZADA','CANCELADA','NO_SHOW']){
  assert.equal(puedeCorregir({...reserva,estado}),estado==='SOLICITADA')
  assert.equal(puedeCancelar({...reserva,estado}),['SOLICITADA','CONFIRMADA'].includes(estado))
 }
 const event=leerEvento({id:3,tipoEvento:'CORRECCION_CLIENTE',estadoAnterior:'SOLICITADA',estadoNuevo:'SOLICITADA',motivo:'Corregir',cambiadoPorNombre:'Host',creadoEn:'2030-01-01T12:00:00Z',clienteAnteriorId:7,clienteNuevoId:8,observacionesAnteriores:null,observacionesNuevas:'Nota'})
 assert.equal(event.clienteAnteriorId,7);assert.equal(event.clienteNuevoId,8)
})
