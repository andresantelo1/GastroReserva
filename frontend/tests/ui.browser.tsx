import { act, StrictMode, useLayoutEffect, useState, type ReactNode } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import Button from '../src/components/ui/Button'
import ConfirmDialog from '../src/components/ui/ConfirmDialog'
import Pagination from '../src/components/ui/Pagination'
import { usePagination } from '../src/hooks/usePagination'
import '../src/styles/tokens.css'
import '../src/styles/ui.css'

Object.assign(globalThis, { IS_REACT_ACT_ENVIRONMENT: true })
const host = document.getElementById('fixtures')!
let root: Root | null = null
let paging: ReturnType<typeof usePagination>
let confirms = 0, cancels = 0
function check(value: unknown, message: string) { if (!value) throw new Error(message) }
export function Paging({ total }: { total: number }) {
  const value = usePagination(total)
  useLayoutEffect(() => { paging = value })
  return <Pagination {...value} />
}
export function Confirmation({ busy = false, error = '' }: { busy?: boolean; error?: string }) {
  const [open, setOpen] = useState(false)
  return <><Button id="opener" onClick={() => setOpen(true)}>Abrir confirmación</Button>
    <ConfirmDialog open={open} title="Confirmación de prueba" message="No se modifica ninguna base de datos." confirming={busy} error={error}
      onCancel={() => { cancels++; setOpen(false) }} onConfirm={() => { confirms++ }} />
  </>
}
async function render(node: ReactNode) { await act(async () => root!.render(<StrictMode>{node}</StrictMode>)) }
async function mount(node: ReactNode) {
  if (root) await act(async () => root!.unmount())
  root = createRoot(host); await render(node)
}
async function click(selector: string) { await act(async () => (host.querySelector(selector) as HTMLElement).click()) }
async function tick() { await new Promise(requestAnimationFrame) }
const cases: [string, () => Promise<void>][] = [
  ['Página y tamaño controlados; límites y reset a 1', async () => {
    await mount(<Paging total={21} />)
    await act(async () => paging.onPageChange(3)); check(paging.page === 3, 'No navegó')
    await act(async () => paging.onPageChange(999)); check(paging.page === 3, 'Superó límite')
    await act(async () => paging.onPageSizeChange(5)); check(paging.page === 1 && paging.pageSize === 5, 'No reinició')
    await act(async () => paging.onPageSizeChange(0)); check(paging.pageSize === 5, 'Aceptó tamaño inválido')
  }],
  ['Eliminar la última fila ajusta y conserva la página válida', async () => {
    await mount(<Paging total={11} />); await act(async () => paging.onPageSizeChange(5)); await act(async () => paging.onPageChange(3))
    await render(<Paging total={10} />); check(paging.page === 2, 'No ajustó página')
    await render(<Paging total={11} />); check(paging.page === 2, 'Reapareció página obsoleta')
  }],
  ['Lista vacía: rango 0–0 y ambas flechas deshabilitadas', async () => {
    await mount(<Paging total={0} />)
    check(host.textContent?.includes('0–0 de 0 resultados'), 'Rango vacío incorrecto')
    check([...host.querySelectorAll('button')].every(button => button.disabled), 'Flechas habilitadas')
  }],
  ['Button conserva atributos nativos; loading bloquea clic y mantiene className', async () => {
    let clicks = 0
    await mount(<Button id="busy" className="extra" aria-label="Acción de prueba" type="submit" loading onClick={() => clicks++}>Enviar</Button>)
    await click('#busy'); const button = host.querySelector('button')!
    check(clicks === 0 && button.disabled && button.type === 'submit' && button.classList.contains('extra') && button.getAttribute('aria-busy') === 'true', 'Props o bloqueo incorrectos')
  }],
  ['showModal crea modal real en StrictMode y enfoca la acción segura', async () => {
    confirms = 0; cancels = 0; await mount(<Confirmation />)
    ;(host.querySelector('#opener') as HTMLElement).focus(); await click('#opener')
    const dialog = host.querySelector('dialog')!
    check(dialog.open && dialog.matches(':modal'), 'No está en capa modal')
    check(dialog.contains(document.activeElement) && document.activeElement?.textContent === 'Volver', 'Foco no está en acción segura')
    check(confirms === 0 && cancels === 0, 'Ejecutó acción al montar')
  }],
  ['Cancelar no confirma; cierre devuelve foco y permite reabrir', async () => {
    const dialog = host.querySelector('dialog')!
    await act(async () => { dialog.dispatchEvent(new Event('cancel', { cancelable: true })) }); await tick()
    check(!dialog.open && confirms === 0 && cancels === 1, 'Cancelar ejecutó confirmación o no cerró')
    check(document.activeElement?.id === 'opener', 'No restauró foco')
    await click('#opener'); check(dialog.open && dialog.matches(':modal'), 'No reabrió')
  }],
  ['Mientras confirma bloquea Escape, cierre y doble clic; conserva error visible', async () => {
    await render(<Confirmation busy error="HTTP 409 de prueba" />)
    const dialog = host.querySelector('dialog')!
    await act(async () => { dialog.dispatchEvent(new Event('cancel', { cancelable: true })) })
    check(dialog.open && cancels === 1, 'Cerró durante envío')
    await click('button[type="submit"]')
    check(confirms === 0 && [...dialog.querySelectorAll('button')].every(button => button.disabled), 'Envío duplicado habilitado')
    check(dialog.textContent?.includes('HTTP 409 de prueba'), 'Ocultó error')
    await render(<Confirmation error="HTTP 409 de prueba" />)
    await click('button[type="submit"]'); check(confirms === 1, 'No permitió reintentar')
  }],
  ['Desmontar limpia el dialog sin disparar confirmación/cancelación', async () => {
    await act(async () => root!.unmount()); root = null; await tick()
    check(!document.querySelector('dialog[open]') && confirms === 1 && cancels === 1, 'Cleanup provocó acción')
  }],
]
document.getElementById('run')!.addEventListener('click', async () => {
  const button = document.getElementById('run') as HTMLButtonElement
  button.disabled = true
  document.getElementById('results')!.replaceChildren()
  let passed = 0
  for (const [name, run] of cases) {
    const li = document.createElement('li')
    try { await run(); passed++; li.textContent = `OK — ${name}` }
    catch (error) { li.textContent = `ERROR — ${name}: ${String(error)}` }
    document.getElementById('results')!.append(li)
  }
  if (root) { await act(async () => root!.unmount()); root = null }
  document.getElementById('result')!.textContent = `${passed}/${cases.length} pruebas aprobadas`
  button.disabled = false
})
