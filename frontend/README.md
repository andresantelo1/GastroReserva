# GastroReserva — React + TypeScript + Vite

Esta carpeta forma parte del repositorio compartido GastroReserva. Para instalar backend y frontend desde cero, seguir [INICIO-EQUIPO.md](../INICIO-EQUIPO.md). Abrir esta carpeta (`frontend/`) en WebStorm; no hace falta tener las rutas personales citadas en evidencias históricas.

Frontend web separado del backend Spring Boot. Guías 01–10 adaptadas: estructura, rutas, tablas, formularios y **conexión HTTP real**, con CRUD de clientes y relación Cliente 1:N Reserva. La guía 08 organiza estado/efectos y hooks; la 09 agrega búsqueda, filtros, páginas y modales; la 10 incorpora dashboard, recuperación de errores de render, Vitest y preparación de build/despliegue.

## Probarlo

1. Encender PostgreSQL y reiniciar el backend de IntelliJ en el puerto 8080 con sus variables habituales. La guía 07 agrega el PUT de corrección, version en la respuesta y migración V9 del historial: un proceso anterior no cumple ese contrato. Respaldar primero datos importantes; Flyway aplica migraciones pendientes, no crear tablas manualmente.
2. En la terminal de esta carpeta, ejecutar:

```powershell
npm.cmd install
npm.cmd run dev
```

3. Abrir la dirección de Vite, normalmente http://localhost:5173, e ir a **Iniciar sesión**.
4. Usar una cuenta de GastroReserva con rol ADMINISTRADOR o HOST, no el usuario/contraseña de PostgreSQL.
5. Abrir **Clientes → + Nuevo cliente**. Probar **Editar**, **Cancelar edición** y **Eliminar** con confirmación. Para reactivar: **Estado del cliente → Inactivos → Editar → Cliente activo**.
6. En **Reservas → + Nueva reserva** se usan sólo clientes activos. Probá el filtro por cliente, **Editar** una SOLICITADA (cliente/observaciones y motivo), **Detalle** con historial y **Cancelar** con confirmación.
7. Durante una carga lenta de detalle, **Cancelar consulta** cierra el GET sin guardar ni cancelar la reserva. Los avisos de éxito duran 5 segundos; los errores ofrecen reintento. Para las pruebas aisladas de hooks, abrir `/tests/hooks.html` con Vite encendido y pulsar Ejecutar.
8. Usar **Buscar clientes/reservas**, combinar filtros y elegir **Filas por página: 5/10/20**. Cada cambio de criterio vuelve a página 1. Los filtros son locales sobre la lista cargada.
9. En una confirmación, **Escape** cierra sin guardar y devuelve el foco; mientras se envía la operación se bloquea el cierre. Pruebas UI independientes: `/tests/ui.html`.
10. Volver a **Inicio**: ver los cuatro indicadores y reservas por estado; **Actualizar indicadores** vuelve a consultar la API. Sólo ADMINISTRADOR/HOST ven el resumen; no es aún el reporte de ocupación/no-show.

**Guardar, dar de baja y cancelar escriben en la base de datos de la API configurada.** No es una simulación. Eliminar un cliente es baja lógica reactivable. Cancelar una reserva conserva fila/historial, pero es un estado final sin reactivación. Editar reserva no cambia fecha, turno, mesa ni personas. Crear un perfil de cliente no crea una cuenta de acceso. Para reservar deben existir clientes, mesas, zonas y turnos activos en el backend.

`.env.development` contiene sólo la URL pública `VITE_API_URL=http://localhost:8080/api`. Si se cambia, reiniciar Vite. La sesión se conserva sólo en memoria: recargar la página requiere iniciar sesión otra vez.

## Verificar

```powershell
npm.cmd run lint
npm.cmd run test:run
npm.cmd run build
```

`npm.cmd test` abre Vitest en vigilancia. `test:run` ejecuta las dos suites una vez; `check` reúne lint, pruebas y build. `.env.production` contiene sólo la API local para la demostración del build. Para publicar hay que cambiarla por una URL HTTPS real y recompilar. Nunca colocar contraseñas, JWT_SECRET ni claves de base de datos en variables VITE_.

Guía detallada: [README-front.md](README-front.md). Componentes/filtros/páginas/modales y **40 respuestas**: [guía 09](docs/guia-09.md). Evidencias actuales: [recorrido HTTP y pruebas React](docs/evidencias/guia-09/recorrido-http.md). Estado y límites: [PA-03-status.md](docs/PA-03-status.md). Hooks: [guía 08](docs/guia-08.md). Relación: [guía 07](docs/guia-07.md). CRUD de clientes: [guía 06](docs/guia-06.md). Conexión inicial: [guía 05](docs/guia-05.md).

Guía final y **50 respuestas**: [guía 10](docs/guia-10.md). [Matriz F01–F12](docs/evidencias/guia-10/matriz-fullstack.md) y [build/preview/despliegue](docs/despliegue.md). Verificación guía 10: **79 Node + 19 Vitest**, lint/build correctos; recorrido real sobre preview, Spring y PostgreSQL temporal. No se publicó en Internet. Completar estas diez guías no significa que todas las experiencias web/móvil del contrato estén terminadas.

Guías anteriores (históricas): [01](docs/guia-01.md), [02](docs/guia-02.md), [03](docs/guia-03.md), [04](docs/guia-04.md). Sus mocks y simulación fueron reemplazados en las pantallas conectadas; no describen el funcionamiento actual de Guardar.
