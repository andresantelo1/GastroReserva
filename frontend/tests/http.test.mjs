import test from 'node:test'
import assert from 'node:assert/strict'
import { createApiClient, ApiError } from '../src/api/httpClient.ts'
import { leerCliente } from '../src/features/clientes/services/clienteService.ts'
import { leerReserva, leerMesa, leerTurno } from '../src/features/reservas/services/reservaService.ts'
import { getSession, setSession, tokenActual, invalidarToken } from '../src/features/auth/session.ts'
const baseUrl = 'http://localhost:8080/api'
const json = (data, status = 200) => new Response(JSON.stringify(data), {status, headers:{'Content-Type':'application/json'}})
test('GET: URL centralizada, Bearer, Accept y sin Content-Type innecesario', async () => {
 const api = createApiClient({baseUrl:baseUrl+'/', getToken:()=> 'token-de-prueba', fetchImpl:async(url, options)=>{
   assert.equal(url,baseUrl+'/clientes'); assert.equal(options.headers.get('Authorization'),'Bearer token-de-prueba')
   assert.equal(options.headers.get('Accept'),'application/json'); assert.equal(options.headers.has('Content-Type'),false)
   assert.equal(options.credentials,'omit'); assert.equal(options.redirect,'error'); return json([])
 }})
 assert.deepEqual(await api('/clientes'),[])
})
test('POST: JSON y respuesta 201 con id del servidor', async () => {
 const api=createApiClient({baseUrl,fetchImpl:async(url,options)=>{
  assert.equal(options.method,'POST'); assert.equal(options.headers.get('Content-Type'),'application/json')
  assert.deepEqual(JSON.parse(options.body),{nombre:'Ana'});return json({id:7,nombre:'Ana'},201)
 }})
 assert.equal((await api('/clientes',{method:'POST',body:JSON.stringify({nombre:'Ana'})})).id,7)
})
test('Login anónimo no envía token previo', async()=>{
 const api=createApiClient({baseUrl,getToken:()=> 'previo',fetchImpl:async(u,o)=>{assert.equal(o.headers.has('Authorization'),false);return json({})}})
 await api('/auth/login',{anonymous:true,method:'POST',body:'{}'})
})
test('204 no intenta parsear JSON', async()=> {
 const api=createApiClient({baseUrl,fetchImpl:async()=>new Response(null,{status:204})})
 assert.equal(await api('/recurso'),undefined)
})
test('400 conserva fieldErrors de strings y descarta valores extraños', async()=>{
 const api=createApiClient({baseUrl,fetchImpl:async()=>json({message:'Datos inválidos',code:'VALIDATION_ERROR',fieldErrors:{nombre:'Obligatorio',extra:7}},400)})
 await assert.rejects(api('/clientes'), e=>e instanceof ApiError&&e.status===400&&e.fieldErrors.nombre==='Obligatorio'&& !('extra' in e.fieldErrors))
})
test('404 y 409 son errores aunque fetch resuelva la Promise',async()=>{
 for(const status of [404,409,422]) {
  const api=createApiClient({baseUrl,fetchImpl:async()=>json({message:'Rechazado'},status)})
  await assert.rejects(api('/clientes'),e=>e.status===status)
 }
})
test('500 y HTML no exponen trazas ni páginas completas', async()=>{
 const api=createApiClient({baseUrl,fetchImpl:async()=>new Response('<html>stacktrace secreta</html>',{status:500})})
 await assert.rejects(api('/clientes'),e=>e.status===500&&!e.message.includes('stacktrace'))
})
test('401 invalida token; 403 no borra la sesión',async()=>{
 for(const status of [401,403]) {
  let invalidado=false
  const api=createApiClient({baseUrl,getToken:()=> 'demo',onUnauthorized:()=>{invalidado=true},fetchImpl:async()=>json({},status)})
  await assert.rejects(api('/clientes'))
  assert.equal(invalidado,status===401)
 }
})
test('Error de red visible; POST advierte resultado incierto y no se reintenta',async()=>{
 let calls=0
 const api=createApiClient({baseUrl,fetchImpl:async()=>{calls++;throw new TypeError('Failed to fetch')}})
 await assert.rejects(api('/clientes',{method:'POST',body:'{}'}),e=>e.code==='NETWORK'&&e.message.includes('pudo haberse guardado'))
 assert.equal(calls,1)
})
test('AbortController cancela un GET sin convertirlo en error de red',async()=>{
 const c=new AbortController();c.abort()
 const api=createApiClient({baseUrl,fetchImpl:async(u,o)=>{o.signal.throwIfAborted()}})
 await assert.rejects(api('/clientes',{signal:c.signal}),e=>e.name==='AbortError')
})
test('Timeout cancela petición pendiente y se identifica',async()=>{
 const api=createApiClient({baseUrl,timeoutMs:5,fetchImpl:async(u,o)=>new Promise((resolve,reject)=>o.signal.addEventListener('abort',()=>reject(new DOMException('cancel','AbortError'))))})
 await assert.rejects(api('/clientes'),e=>e.code==='TIMEOUT')
})
test('Configuración ausente o ruta externa se rechaza antes de fetch',async()=>{
 let calls=0
 for (const [url,endpoint] of [[undefined,'/clientes'],['ftp://host/api','/clientes'],[baseUrl,'https://ajeno.test'],[baseUrl,'//ajeno.test'],[baseUrl,'/../privado']]) {
  const api=createApiClient({baseUrl:url,fetchImpl:async()=>{calls++;return json({})}})
  await assert.rejects(api(endpoint),e=>e.code==='CONFIG')
 }
 assert.equal(calls,0)
})
test('Respuesta exitosa que no es JSON se distingue del vacío válido',async()=>{
 const api=createApiClient({baseUrl,fetchImpl:async()=>new Response('<html>error</html>')})
 await assert.rejects(api('/clientes'),e=>e.code==='INVALID_RESPONSE')
})
test('ClienteResponse proyecta sólo los datos del listado, preserva null',()=>{
 assert.deepEqual(leerCliente({id:7,nombre:'Ana',email:'ana@example.com',telefono:null,activo:true,usuarioId:99}),{id:7,nombre:'Ana',email:'ana@example.com',telefono:null,activo:true})
 assert.throws(()=>leerCliente({id:'7'}),ApiError)
})
test('ReservaResponse anidada: id de cliente y número de mesa se mapean sin confundirlos',()=>{
 const r=leerReserva({id:9,cliente:{id:7,nombre:'Ana'},mesa:{id:33,numero:2},fecha:'2030-01-15',inicio:'2030-01-15T12:00:00',fin:'2030-01-15T15:00:00',cantidadPersonas:4,estado:'SOLICITADA',observaciones:null,version:0})
 assert.equal(r.clienteId,7);assert.equal(r.clienteNombre,'Ana');assert.equal(r.mesaNumero,2)
 assert.throws(()=>leerReserva({...r,estado:'INVENTADO'}),ApiError)
})
test('Catálogos reales interpretan actividad, zona y horarios',()=>{
 const m=leerMesa({id:3,numero:2,capacidad:4,estado:'DISPONIBLE',activa:true,zona:{id:1,nombre:'Terraza',activa:true}})
 assert.equal(m.id,3);assert.equal(m.numero,2)
 assert.equal(leerTurno({id:1,nombre:'Almuerzo',horaInicio:'12:00:00',horaFin:'15:30:00',activo:true}).nombre,'Almuerzo · 12:00–15:30')
})
test('Sesión en memoria y respuestas viejas no cierran una sesión nueva',()=>{
 const session={accessToken:'nuevo',expiresAt:new Date(Date.now()+60000).toISOString(),tokenType:'Bearer',usuario:{id:1,nombre:'Demo',email:'demo@example.com',rol:'HOST',activo:true}}
 setSession(session);invalidarToken('viejo');assert.equal(tokenActual(),'nuevo')
 invalidarToken('nuevo');assert.equal(getSession(),null)
 setSession({...session,expiresAt:'2000-01-01T00:00:00Z'});assert.equal(tokenActual(),undefined);setSession(null)
})

