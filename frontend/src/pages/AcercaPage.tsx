import { Link } from 'react-router'

export default function AcercaPage() {
  return (
    <section aria-labelledby="acerca-title">
      <div className="page-heading">
        <p className="page-heading__eyebrow">Proyecto académico / PA-03</p>
        <h2 id="acerca-title">Acerca de GastroReserva</h2>
        <p className="page-heading__description">Una aplicación para organizar las reservas y la atención de un restaurante.</p>
      </div>
      <div className="placeholder-card about-card">
        <h3>Qué construimos en esta etapa</h3>
        <p>Las guías 1 a 6 prepararon la estructura, conexión real y CRUD de clientes. La guía 7 trabaja Cliente 1:N Reservas: alta con selector, filtro por cliente, detalle e historial, corrección de cliente/observaciones mediante PUT y cancelación con PATCH. Guardar modifica la base configurada en el backend.</p>
        <p>La guía 8 conserva esas operaciones y organiza su carga en hooks reutilizables: cancela consultas al salir, ignora respuestas antiguas, permite reintentar errores y separa carga, guardado y confirmaciones temporales. No agrega nuevas entidades ni cambia las reglas del backend.</p>
        <p>La guía 9 agrega búsqueda y filtros combinados, páginas de 5, 10 o 20 filas y componentes UI compartidos. Las confirmaciones usan un diálogo modal accesible, con teclado, cierre con Escape antes de enviar y bloqueo mientras se guarda. No se borran reservas ni se agregan campos ajenos al restaurante.</p>
        <p>La guía 10 incorpora un dashboard administrativo con cuatro indicadores derivados de la API, recuperación ante errores de render y pruebas con Vitest y Testing Library. El build de producción se comprueba localmente; no significa que exista un sitio publicado ni que estén listas todas las experiencias del proyecto.</p>
        <h3>Qué viene después</h3>
        <p>El backend conserva la autoridad sobre permisos, capacidad y solapamientos. Sólo se corrigen reservas SOLICITADA, con motivo y auditoría; no se reprograman fecha, mesa o turno en este formulario. Cancelar no borra el historial. Autoservicio del cliente, atención y los demás módulos web continúan pendientes.</p>
        <div className="tech-list" aria-label="Tecnologías del frontend">
          <span>React</span><span>TypeScript</span><span>Vite</span><span>React Router</span><span>Vitest</span>
        </div>
        <Link className="button-link" to="/">Volver al inicio <span aria-hidden="true">→</span></Link>
      </div>
    </section>
  )
}
