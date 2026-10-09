import test from 'node:test'
import assert from 'node:assert/strict'
import { startAsyncQuery } from '../src/hooks/asyncQuery.ts'
import { ApiError } from '../src/api/httpClient.ts'

function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
function observe(events) {
  return { start: () => events.push('start'), success: (x) => events.push(x), error: (e) => events.push(e), finish: () => events.push('finish') }
}

test('Query: setup, éxito y finalización separados', async () => {
  const events = [], task = deferred()
  const query = startAsyncQuery(() => task.promise, observe(events))
  assert.deepEqual(events, ['start'])
  task.resolve('datos'); await query.done
  assert.deepEqual(events, ['start', 'datos', 'finish'])
})
test('Cleanup aborta señal y no aplica resolución tardía aunque loader ignore abort', async () => {
  const events = [], task = deferred(); let signal
  const query = startAsyncQuery((s) => { signal = s; return task.promise }, observe(events))
  query.cancel(); assert.equal(signal.aborted, true)
  task.resolve('viejo'); await query.done
  assert.deepEqual(events, ['start'])
})
test('Una respuesta vieja no sobrescribe a la nueva ni ejecuta su finally', async () => {
  const events = [], old = deferred(), recent = deferred()
  const first = startAsyncQuery(() => old.promise, observe(events)); first.cancel()
  const second = startAsyncQuery(() => recent.promise, observe(events))
  recent.resolve('nuevo'); await second.done; old.resolve('viejo'); await first.done
  assert.deepEqual(events, ['start', 'start', 'nuevo', 'finish'])
})
test('Un error de la consulta anterior tampoco reemplaza el resultado nuevo', async () => {
  const events = [], old = deferred()
  const first = startAsyncQuery(() => old.promise, observe(events)); first.cancel()
  await startAsyncQuery(async () => 'nuevo', observe(events)).done
  old.reject(new ApiError(500, 'fallo viejo')); await first.done
  assert.deepEqual(events, ['start', 'start', 'nuevo', 'finish'])
})
test('AbortError esperado no se presenta como error funcional', async () => {
  const events = []
  await startAsyncQuery(async () => { throw new DOMException('Cancelado', 'AbortError') }, observe(events)).done
  assert.deepEqual(events, ['start', 'finish'])
})
test('Error HTTP real conserva status y termina carga', async () => {
  const events = []
  await startAsyncQuery(async () => { throw new ApiError(404, 'No existe') }, observe(events)).done
  assert.deepEqual(events, ['start', 'HTTP 404: No existe', 'finish'])
})
test('Loader que lanza síncronamente no deja un spinner infinito', async () => {
  const events = []
  await startAsyncQuery(() => { throw new Error('Configuración inválida') }, observe(events)).done
  assert.deepEqual(events, ['start', 'Configuración inválida', 'finish'])
})
test('Dos instancias reutilizan lógica pero no comparten cancelación ni datos', async () => {
  const a = [], b = [], pending = deferred()
  const first = startAsyncQuery(() => pending.promise, observe(a))
  const second = startAsyncQuery(() => pending.promise, observe(b))
  first.cancel(); pending.resolve('segundo'); await Promise.all([first.done, second.done])
  assert.deepEqual(a, ['start']); assert.deepEqual(b, ['start', 'segundo', 'finish'])
})
test('Setup-cleanup-setup no bloquea la segunda consulta y cancelar es idempotente', async () => {
  const events = [], pending = deferred()
  const first = startAsyncQuery(() => pending.promise, observe(events)); first.cancel(); first.cancel()
  await startAsyncQuery(async () => 'segundo setup', observe(events)).done
  pending.reject(new Error('viejo')); await first.done
  assert.deepEqual(events, ['start', 'start', 'segundo setup', 'finish'])
})
