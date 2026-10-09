import { Component, type ErrorInfo, type ReactNode } from 'react'
import Button from '../components/ui/Button'

export default class AppErrorBoundary extends Component<{ children: ReactNode }, { hasError: boolean }> {
  state = { hasError: false }

  static getDerivedStateFromError() { return { hasError: true } }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // Sin telemetría externa ni detalles técnicos en la pantalla de producción.
    if (import.meta.env.DEV) console.error('Error de render de GastroReserva', error, info.componentStack)
    else console.error('GastroReserva: error de render. La interfaz mostró su recuperación segura.')
  }

  render() {
    if (this.state.hasError) return <main className="app-error" role="alert">
      <h1>La interfaz tuvo un problema</h1>
      <p>No pudimos mostrar esta pantalla. Los cambios no guardados pueden perderse; si estabas guardando, comprobá la lista antes de repetir la operación.</p>
      <Button onClick={() => this.setState({ hasError: false })}>Reintentar pantalla</Button>
      <a href="/">Volver al inicio</a>
      <p>Volver al inicio recarga la aplicación y requiere iniciar sesión otra vez.</p>
    </main>
    return this.props.children
  }
}
