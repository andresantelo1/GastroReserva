import { act, StrictMode, useLayoutEffect, type ReactNode } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { useApiQuery } from '../src/hooks/useApiQuery'
import { useAsyncDetail } from '../src/hooks/useAsyncDetail'
import { useSuccessMessage } from '../src/hooks/useSuccessMessage'
import { useSubmission } from '../src/hooks/useSubmission'
import { ApiError } from '../src/api/httpClient'

Object.assign(globalThis, { IS_REACT_ACT_ENVIRONMENT: true })
function check(condition: unknown, message: string) { if (!condition) throw new Error(message) }
function deferred<T>() {
  let resolve!: (value: T) => void, reject!: (error: unknown) => void
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
type Job = ReturnType<typeof deferred<string>> & { signal: AbortSignal; id?: number }
const jobs: Job[] = []
const loader = (signal: AbortSignal) => { const job = { ...deferred<string>(), signal }; jobs.push(job); return job.promise }
const byId = (id: number, signal: AbortSignal) => { const job = { ...deferred<string>(), signal, id }; jobs.push(job); return job.promise }
let query: ReturnType<typeof useApiQuery<string>>
let detail: ReturnType<typeof useAsyncDetail<string>>
let notice: ReturnType<typeof useSuccessMessage>
let mutation: ReturnType<typeof useSubmission>
// Capturar después del commit, nunca mutar el registro de pruebas en render.
export function Query() { const value = useApiQuery(loader, ''); useLayoutEffect(() => { query = value }); return <p>Dato: {value.data} / carga: {String(value.loading)} / error: {value.error}</p> }
export function Detail({ id }: { id: number | null }) { const value = useAsyncDetail(id, byId); useLayoutEffect(() => { detail = value }); return <p>Detalle: {value.data} / carga: {String(value.loading)} / error: {value.error}</p> }
export function Notice() { const value = useSuccessMessage(80); useLayoutEffect(() => { notice = value }); return <p>{value.message}</p> }
export function Mutation() { const value = useSubmission(); useLayoutEffect(() => { mutation = value }); return <p>Guardando: {String(value.busy)} / {value.error}</p> }
let pairValues: string[] = []
export function Pair() { const a = useApiQuery(loader, ''), b = useApiQuery(loader, ''); useLayoutEffect(() => { pairValues = [a.data, b.data] }); return <p>{a.data}/{b.data}</p> }
let root: Root | null = null
const host = document.getElementById('fixtures')!
async function mount(node: ReactNode) {
  await unmount(); jobs.length = 0
  root = createRoot(host)
  await act(async () => root!.render(<StrictMode>{node}</StrictMode>))
}
async function unmount() { if (root) { const old = root; root = null; await act(async () => old.unmount()) } }
async function render(node: ReactNode) { await act(async () => root!.render(<StrictMode>{node}</StrictMode>)) }
const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))
const cases: [string, () => Promise<void>][] = [
  ['StrictMode ejecuta setup/cleanup/setup y sólo acepta la respuesta vigente', async () => {
    await mount(<Query />); check(jobs.length === 2 && jobs[0].signal.aborted, 'No hubo cleanup del primer setup')
    await act(async () => jobs[1].resolve('actual')); await act(async () => jobs[0].resolve('obsoleto'))
    check(query.data === 'actual' && !query.loading && !query.error, 'Estado final incorrecto')
  }],
  ['reload estable cancela consulta anterior y reinicia carga/error', async () => {
    await mount(<Query />); const reload = query.reload; const old = jobs.at(-1)!
    await act(async () => query.reload()); check(old.signal.aborted && query.loading && query.reload === reload, 'Recarga inestable')
    await act(async () => jobs.at(-1)!.resolve('recargado')); await act(async () => old.reject(new Error('viejo')))
    check(query.data === 'recargado' && !query.error, 'Error viejo aplicado')
  }],
  ['Cambiar id cancela A y una respuesta tardía no reemplaza B', async () => {
    await mount(<Detail id={1} />); const old = jobs.at(-1)!
    await render(<Detail id={2} />); check(old.signal.aborted && detail.loading && detail.data === null, 'No se limpió A')
    await act(async () => jobs.at(-1)!.resolve('Cliente B')); await act(async () => old.resolve('Cliente A'))
    check(detail.data === 'Cliente B' && !detail.loading, 'A sobrescribió B')
  }],
  ['Cambiar id limpia error previo y no conserva datos del recurso anterior', async () => {
    await mount(<Detail id={1} />); await act(async () => jobs.at(-1)!.resolve('A'))
    await render(<Detail id={2} />); await act(async () => jobs.at(-1)!.reject(new ApiError(404, 'No existe B')))
    check(detail.data === null && detail.error.includes('404') && !detail.loading, 'Detalle viejo visible tras error')
    await render(<Detail id={3} />); check(detail.loading && !detail.error, 'No limpió error al cambiar id')
  }],
  ['Cerrar y reabrir el mismo id no reutiliza un detalle viejo', async () => {
    await mount(<Detail id={1} />); await act(async () => jobs.at(-1)!.resolve('primero'))
    await render(<Detail id={null} />); check(detail.data === null && !detail.loading, 'Cierre no vacío')
    await render(<Detail id={1} />); check(detail.data === null && detail.loading, 'Reapertura obsoleta')
    await act(async () => jobs.at(-1)!.resolve('vigente')); check(detail.data === 'vigente', 'No reconsultó')
  }],
  ['Desmontar aborta sin éxito/error/finally tardíos en pantalla', async () => {
    await mount(<Detail id={1} />); const old = jobs.at(-1)!
    await unmount(); check(old.signal.aborted, 'Señal no cancelada')
    await act(async () => old.resolve('tardío')); check(host.textContent === '', 'Render después de desmontar')
  }],
  ['AbortError no es error funcional y un 500 sí permite reintento', async () => {
    await mount(<Query />); await act(async () => jobs.at(-1)!.reject(new DOMException('Abortado', 'AbortError')))
    check(!query.error && !query.loading, 'Cancelación mostrada como error')
    await act(async () => query.reload()); await act(async () => jobs.at(-1)!.reject(new ApiError(500, 'Fallo de prueba')))
    check(query.error.includes('500') && !query.loading, 'Fallo real oculto')
    await act(async () => query.reload()); check(!query.error && query.loading, 'Reintento no limpia')
    await act(async () => jobs.at(-1)!.resolve('recuperado')); check(query.data === 'recuperado', 'No recuperó')
  }],
  ['setData funcional actualiza sin iniciar otro GET', async () => {
    await mount(<Query />); await act(async () => jobs.at(-1)!.resolve('uno')); const count = jobs.length
    await act(async () => query.setData((value) => value + ' dos'))
    check(query.data === 'uno dos' && jobs.length === count, 'Actualización disparó GET')
  }],
  ['Dos hooks comparten lógica, no datos ni estado', async () => {
    await mount(<Pair />); const active = jobs.filter((job) => !job.signal.aborted)
    await act(async () => active[0].resolve('A')); check(pairValues[0] === 'A' && pairValues[1] === '', 'Estado compartido')
    await act(async () => active[1].resolve('B')); check(pairValues[1] === 'B', 'Segunda instancia incorrecta')
  }],
  ['Éxito temporal reinicia su timer, expira y se limpia al desmontar', async () => {
    await mount(<Notice />); await act(async () => notice.showSuccess('Guardado')); await act(async () => wait(50))
    await act(async () => notice.showSuccess('Guardado')); await act(async () => wait(50)); check(notice.message === 'Guardado', 'Timer anterior borró el nuevo')
    await act(async () => wait(100)); check(notice.message === '', 'No expiró')
    await act(async () => notice.showSuccess('Otro')); await unmount(); await act(async () => wait(100)); check(host.textContent === '', 'Timer después de desmontar')
  }],
  ['Guardar sólo por evento, una mutación a la vez y busy separado de GET', async () => {
    await mount(<Mutation />); let calls = 0, saves = 0; const task = deferred<string>()
    const operation = () => { calls++; return task.promise }
    let pending!: Promise<void>
    await act(async () => { pending = mutation.run(operation, () => saves++); void mutation.run(operation, () => saves++) })
    check(calls === 1 && mutation.busy, 'Envío duplicado')
    await act(async () => { task.resolve('ok'); await pending }); check(saves === 1 && !mutation.busy, 'Finalización incorrecta')
  }],
  ['Desmontar mutación no aplica éxito ni error; abort no promete rollback', async () => {
    await mount(<Mutation />); const task = deferred<string>(); let signal!: AbortSignal, saved = false, pending!: Promise<void>
    await act(async () => { pending = mutation.run((s) => { signal = s; return task.promise }, () => { saved = true }) })
    await unmount(); check(signal.aborted, 'Mutación no abortada')
    await act(async () => { task.resolve('servidor pudo guardar'); await pending }); check(!saved, 'Éxito después de desmontar')
  }],
]

const button = document.getElementById('run') as HTMLButtonElement
button.addEventListener('click', async () => {
  button.disabled = true
  const results = document.getElementById('results')!, status = document.getElementById('result')!
  results.replaceChildren(); status.textContent = 'Ejecutando…'
  let passed = 0
  for (const [name, run] of cases) {
    const item = document.createElement('li')
    try { await run(); passed++; item.textContent = `OK — ${name}` }
    catch (error) { item.textContent = `FALLO — ${name}: ${error instanceof Error ? error.message : String(error)}` }
    finally { await unmount() }
    results.append(item)
  }
  status.textContent = `${passed}/${cases.length} pruebas aprobadas`
  button.disabled = false
})
