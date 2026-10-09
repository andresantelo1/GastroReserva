import { useEffect, useRef, useState } from 'react'

// Sólo un temporizador: no hace peticiones HTTP. La ref evita envíos duplicados.
export function useEnvioSimulado() {
  const [enviando, setEnviando] = useState(false)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => () => {
    if (timer.current !== null) clearTimeout(timer.current)
  }, [])

  const iniciar = (alTerminar: () => void) => {
    if (timer.current !== null) return
    setEnviando(true)
    timer.current = setTimeout(() => {
      timer.current = null
      setEnviando(false)
      alTerminar()
    }, 500)
  }

  return { enviando, iniciar }
}
