// Sólo página de pruebas de Vite dev; no forma parte de rutas ni del build.
import { useState } from 'react'
import { createRoot } from 'react-dom/client'
import AppErrorBoundary from '../src/app/AppErrorBoundary'
import '../src/styles/global.css'
import '../src/styles/tokens.css'
import '../src/styles/ui.css'
import '../src/styles/dashboard.css'
export function ThrowOnRender({ fail }: { fail: boolean }) {
  if (fail) throw new Error('Fallo intencional aislado de la guía 10')
  return <p>Pantalla de prueba disponible, sin API ni base de datos.</p>
}
export function Demo() {
  const [fail, setFail] = useState(false)
  return <><h1>Prueba aislada de error de render</h1><button onClick={() => setFail(true)}>Provocar fallo de prueba</button>
    <button onClick={() => setFail(false)}>Retirar fallo de prueba</button>
    <AppErrorBoundary><ThrowOnRender fail={fail} /></AppErrorBoundary></>
}
createRoot(document.getElementById('root')!).render(<Demo />)
