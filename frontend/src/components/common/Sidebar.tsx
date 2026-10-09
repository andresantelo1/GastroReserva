import { NavLink } from 'react-router'

const menuItems = [
  { to: '/', label: 'Inicio', number: '01', end: true },
  { to: '/clientes', label: 'Clientes', number: '02' },
  { to: '/mesas', label: 'Mesas', number: '03' },
  { to: '/reservas', label: 'Reservas', number: '04' },
  { to: '/acerca', label: 'Acerca', number: '05' },
]

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar__brand">
        <span className="sidebar__brand-mark" aria-hidden="true">G.</span>
        <div><strong>GastroReserva</strong><small>Administración</small></div>
      </div>
      <p className="sidebar__label">ESPACIO DE TRABAJO</p>
      <nav className="sidebar__nav" aria-label="Navegación principal">
        {menuItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              `sidebar__link${isActive ? ' sidebar__link--active' : ''}`
            }
          >
            <span className="sidebar__number" aria-hidden="true">{item.number}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar__note">
        <p>Un lugar para cada visita.</p>
        <small>React + TypeScript + Vite</small>
      </div>
    </aside>
  )
}
