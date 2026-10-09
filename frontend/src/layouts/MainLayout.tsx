import { Outlet } from 'react-router'
import Header from '../components/common/Header'
import Sidebar from '../components/common/Sidebar'

export default function MainLayout() {
  return (
    <div className="app-shell">
      <a className="skip-link" href="#contenido">Saltar al contenido</a>
      <Sidebar />
      <div className="app-shell__main">
        <Header />
        <main id="contenido" className="app-content" tabIndex={-1}>
          {/* Aquí cambia la página; el menú y la cabecera permanecen. */}
          <Outlet />
        </main>
        <footer className="app-footer">
          <span>GastroReserva · Proyecto académico PA-03</span>
          <span>Guía 10 / Dashboard e integración final</span>
        </footer>
      </div>
    </div>
  )
}
