import { useCallback, useEffect, useState } from 'react'

export function useSuccessMessage(durationMs = 5000) {
  const [notice, setNotice] = useState({ message: '', revision: 0 })
  const showSuccess = useCallback((message: string) => {
    // Repetir una confirmación idéntica también reinicia el tiempo de lectura.
    setNotice((prev) => ({ message, revision: prev.revision + 1 }))
  }, [])
  const clearSuccess = useCallback(() => showSuccess(''), [showSuccess])
  useEffect(() => {
    if (!notice.message) return
    const timer = setTimeout(clearSuccess, durationMs)
    return () => clearTimeout(timer)
  }, [notice, durationMs, clearSuccess])
  return { message: notice.message, showSuccess, clearSuccess }
}
