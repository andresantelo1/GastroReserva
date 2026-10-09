import { Link } from 'react-router'
import AccessBoundary from '../features/auth/AccessBoundary'
import DashboardData from '../features/dashboard/components/DashboardData'

const modules = [
  { to: '/clientes', title: 'Clientes', number: '01', description: 'Las personas detrás de cada visita. Un espacio para organizar sus perfiles.' },
  { to: '/mesas', title: 'Mesas', number: '02', description: 'El espacio del restaurante. Zonas, capacidad y estado de cada mesa.' },
  { to: '/reservas', title: 'Reservas', number: '03', description: 'Cada visita, en su momento. El punto de partida para organizar la agenda.' },
]

export default function DashboardPage() {
  return (
    <section aria-labelledby="dashboard-title">
      <div className="page-heading">
        <p className="page-heading__eyebrow">Guía 10 / Panel principal</p>
        <h2 id="dashboard-title">Bienvenido a GastroReserva</h2>
        <p className="page-heading__description">Clientes y reservas en un solo lugar. Revisá el resumen y continuá con la gestión del restaurante.</p>
      </div>
      <AccessBoundary roles={['ADMINISTRADOR', 'HOST']}><DashboardData /></AccessBoundary>
      <div className="section-heading">
        <h3>Tu restaurante, por módulos</h3>
        <span className="demo-label">Navegación disponible</span>
      </div>
      <div className="module-grid">
        {modules.map((module) => (
          <Link className="module-link" to={module.to} key={module.to}>
            <span className="module-link__number" aria-hidden="true">{module.number}</span>
            <h3>{module.title}</h3>
            <p>{module.description}</p>
            <span className="module-link__action">Explorar módulo <span aria-hidden="true">↗</span></span>
          </Link>
        ))}
      </div>
      <section className="stage-summary" aria-labelledby="stage-title">
        <div>
          <p className="page-heading__eyebrow">Dónde estamos</p>
          <h3 id="stage-title">Del formulario a la base de datos.<br />Conexión real.</h3>
        </div>
        <div>
          <p>Clientes permite crear, editar y dar de baja perfiles. Reservas permite crear, filtrar por cliente, consultar el historial, corregir solicitudes y cancelar conservando la trazabilidad. Iniciá sesión con una cuenta administrativa. La atención, el autoservicio y los demás módulos web siguen pendientes.</p>
          <Link className="text-link" to="/acerca">Conocer el alcance de esta etapa <span aria-hidden="true">→</span></Link>
        </div>
      </section>
    </section>
  )
}
