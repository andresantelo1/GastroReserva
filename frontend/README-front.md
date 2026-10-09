# GastroReserva — frontend web

## Qué hace ahora

Las guías 05–08 conectan React con Spring Boot usando fetch y organizan sus cargas con hooks reutilizables. Se puede iniciar sesión, listar/crear/editar/dar de baja/reactivar perfiles de clientes, consultar mesas activas y gestionar reservas operativas: alta, filtro por cliente, detalle/historial, corrección de cliente/observaciones sólo SOLICITADA y cancelación de SOLICITADA/CONFIRMADA. Los datos vienen de la API y los identificadores los asigna PostgreSQL.

La guía 09 suma búsqueda de texto, filtros combinados, páginas de 5/10/20 filas y confirmaciones modales con Escape/foco en Clientes y Reservas. No cambia contratos ni reglas del backend.

La guía 10 agrega Inicio con cuatro métricas derivadas y reservas por estado para ADMINISTRADOR/HOST, ErrorBoundary de render y 19 pruebas Vitest/Testing Library. Incluye build/preview, matriz fullstack y preparación de hosting, sin publicar. El resumen abarca todas las fechas cargadas: no sustituye los reportes de ocupación/no-show.

Este incremento es administrativo: ADMINISTRADOR/HOST gestionan clientes y reservas; MESERO consulta agenda/detalle/historial y mesas, pero la web no le ofrece alta, corrección ni cancelación. CLIENTE todavía no tiene su experiencia de autoservicio en esta web. Que exista esa función en el backend no significa que su interfaz esté terminada.

## Arranque paso a paso

1. Abrir el backend en IntelliJ, encender PostgreSQL y reiniciar Spring Boot con su configuración habitual para aplicar Flyway V9 y cargar el contrato nuevo de reservas (incluye version). Respaldar datos importantes antes. Salud: http://localhost:8080/api/health.
2. Abrir la carpeta `frontend/` del repositorio descargado en WebStorm. La ruta `C:/Users/antel/WebstormProjects/gastroreserva1` citada en evidencias corresponde al equipo original, no es un requisito.
3. En la terminal que contiene `package.json`, ejecutar `npm.cmd install` si faltan dependencias y `npm.cmd run dev`.
4. Abrir la URL indicada por Vite, entrar a Iniciar sesión y usar email/contraseña de una cuenta de **GastroReserva**.
5. Probar Clientes y Reservas. POST, PUT, PATCH y DELETE modifican la base que usa Spring Boot.

`npm.cmd` evita problemas de la política de scripts de PowerShell. Dejar la terminal del servidor funcionando. Ctrl+C la detiene. Versiones verificadas: Node 26.10.0 y npm 11.19.1; se conservaron las dependencias y el lockfile existentes.

El perfil de cliente creado desde Clientes no incluye contraseña ni crea usuario de acceso. Las cuentas se administran mediante los contratos existentes del backend. No poner credenciales de PostgreSQL en el login web.

## Variables y CORS

`.env.development`:

```dotenv
VITE_API_URL=http://localhost:8080/api
```

Se lee una sola vez en `src/api/apiClient.ts`. Reiniciar Vite al cambiarla. `.env.example` sirve de referencia. VITE_ es configuración pública incorporada al JavaScript; no guardar secretos allí ni en código.

El backend ya permite por defecto los orígenes exactos `http://localhost:5173` y `http://127.0.0.1:5173`. Si Vite usa otro puerto o se publica la web, configurar `CORS_ALLOWED_ORIGINS` del backend con los orígenes realmente necesarios y reiniciarlo. CORS no reemplaza JWT ni permisos. No se desactivó Spring Security ni se agregó un proxy para ocultar CORS.

La sesión y el token permanecen sólo en memoria. No se guardan en localStorage, cookies ni archivos. Una recarga completa cierra la sesión; navegar con el menú no. Un 401 invalida la sesión y pide ingresar nuevamente. Un 403 informa la falta de permisos; el backend sigue comprobando rol y actividad vigentes.

## Organización

| Lugar | Responsabilidad |
|---|---|
| src/app, src/main.tsx | Montaje de React, estilos y BrowserRouter. |
| src/routes, src/layouts | Rutas, layout con Outlet y navegación común. |
| src/api/httpClient.ts | fetch, URL, headers, JSON, response.ok, errores, timeout y cancelación. |
| src/api/apiClient.ts | Instancia configurada con la URL de Vite y JWT en memoria. |
| src/api/contracts.ts | Validación en ejecución de las respuestas; TypeScript solo no valida JSON externo. |
| src/features/auth | Login real, sesión en memoria y límites visuales por rol. |
| src/features/*/services | Endpoints, objetos de envío y mapeo de respuestas. |
| src/features/*/models | Datos utilizados por las vistas. |
| src/features/*/types y utils | Estado del formulario, contratos de envío y validaciones puras. |
| src/features/*/components y pages | Formularios, tablas y coordinación del estado visible. |
| src/hooks | useApiQuery, useAsyncList/useAsyncDetail para GET; useSuccessMessage para avisos; useSubmission para mutaciones; usePagination para página/tamaño/límites. |
| src/features/*/hooks | useClientes/useReservas y detalles: loaders estables hacia los services del dominio. |
| src/components/common | Cabecera, menú, campos y estados de carga/error. |
| src/components/ui | Button, SearchInput, SelectFilter, Pagination, Modal, ConfirmDialog, EmptyState, StatusBadge y MetricCard; sin servicios de dominio. |
| src/styles | Estilo compartido/adaptable, tokens.css y ui.css. |
| src/features/*/data | Mocks históricos de guía 03; no se importan en las pantallas conectadas. |
| tests | 79 pruebas Node; prácticas manuales históricas en /tests/ui.html y /tests/hooks.html. /tests/error-boundary.html demuestra un fallo aislado, sólo en dev. |
| src/**/*.test.tsx y src/test | 19 pruebas Vitest/Testing Library con jsdom y dobles de HTTP/servicios, sin acceso a la base real. |
| src/features/dashboard | Resumen derivado y coordinación de la carga mediante hooks existentes. |

Los archivos de práctica previa que no se usan se conservan como referencia; no hay fallback a mocks cuando la API falla. No se instalaron Axios ni librerías de formularios.

## Formularios reales

El alta de Cliente envía `nombre`, `email` y `telefono` (null si está vacío). Su edición envía además `activo` booleano obligatorio. Reserva envía `clienteId`, `fecha`, `turnoId`, `mesaId`, `cantidadPersonas` y `observaciones`. Los identificadores y cantidad son números, no etiquetas ni cadenas.

Documento, dirección, checkbox de alta y referencia eran ejercicios locales de la guía 04: se retiraron del flujo real porque no pertenecen a esos DTO. El checkbox de estado de la guía 06 aparece sólo al editar y sí corresponde a ClienteUpdateRequest. No se agregaron campos ni migraciones para copiar ParkFlow360.

Las listas de clientes/mesas/turnos del formulario vienen de GET. Sólo se ofrecen opciones activas. La mesa visible se identifica por número, pero el POST envía su id. Un catálogo activo **no equivale a disponibilidad para una fecha**: Spring verifica capacidad y solapamientos y puede rechazar con 422.

Guardar valida, muestra Guardando y bloquea controles. Si tiene éxito, usa el objeto devuelto por la API, actualiza la tabla y limpia el formulario. Si falla, conserva lo escrito y muestra el error. Para recargar toda la lista, cerrar el formulario y usar Actualizar. No hay reintento automático de POST: ante un fallo de red podría haberse guardado en el servidor; revisar el listado antes de repetir.

## CRUD de clientes (guía 06)

Editar hace GET por id y abre el mismo formulario de alta con datos actuales. Actualizar cliente hace PUT y reemplaza sólo su fila con la respuesta; Cancelar edición vuelve a un alta vacía sin guardar. Al modificar un perfil vinculado a una cuenta se sincronizan nombre/email, no contraseña.

Eliminar requiere confirmación y hace DELETE con respuesta 204 sin JSON. Es una baja lógica: conserva id, correo único, cuenta, reservas e historial, sin cancelarlos. Activos/Inactivos/Todos permite consultar los perfiles; Editar → Cliente activo permite reactivar. Guardar selecciona automáticamente la vista del estado resultante.

Errores 400/404/409 no inventan modificaciones locales. La confirmación puede cancelarse sin HTTP; durante detalle/guardado/baja se bloquean acciones incompatibles. No hay reintentos automáticos de mutaciones ni protección de edición concurrente entre distintos usuarios añadida en esta guía.

## Cliente 1:N Reserva (guía 07)

ReservasPage carga clientes/reservas con Promise.all para ADMINISTRADOR/HOST. La tabla resuelve nombres con Map, no con fetch por fila. El filtro de cliente es local y convierte el id de string a number. MESERO no consulta `/clientes` sin permiso: usa la proyección incluida en la agenda.

Editar primero consulta GET por id y abre un formulario específico para cliente/observaciones y motivo. PUT `/reservas/{id}/datos-cliente` incluye la versión recibida (0 válida); se admite sólo SOLICITADA. No cambia programación ni personas. El backend registra ambas referencias y observaciones, actor, motivo y fecha en el historial V9. Un cambio obsoleto distinto da 409 sin sobrescribir ni borrar lo escrito. Cancelar edición descarta los campos sin request.

Cancelar requiere confirmación y motivo; PATCH `/reservas/{id}/estado` devuelve 200 con la reserva CANCELADA. La fila se conserva, no se borra; es terminal, no reactivable. Sólo la respuesta exitosa actualiza la tabla. Detalle consulta reserva/historial; cada lectura ocurre por acción, no por fila. La edición de reservas sí incorpora versión, a diferencia del CRUD de clientes anterior.

Guía, límites, diagnóstico y 40 respuestas: [guía 07](docs/guia-07.md).

## Compilar y probar

```powershell
npm.cmd run lint
npm.cmd run test:run
npm.cmd run build
```

Build verifica TypeScript y crea `dist`; no arranca el servidor ni reemplaza `npm run dev`. El modo producción no lee `.env.development`; ahora `.env.production` configura la API localhost para probar localmente. Para publicar, proporcionar la URL HTTPS real durante el build y recompilar, sin secretos. `npm.cmd test` es Vitest en vigilancia; `test:unit` sólo Node; `test:run` ambas suites una vez; `check` reúne lint, pruebas y build.

`npm.cmd run preview` suele usar 4173: si se prueba allí, autorizar ese origen exacto en el backend, o usar un puerto ya autorizado que esté libre. No ampliar CORS a todos los orígenes. Al publicar una SPA, el hosting debe servir index.html para rutas del frontend sin interceptar archivos ni API.

Las pruebas usan el soporte TypeScript nativo de Node y se comprobaron con Node 26.10.0. No se asegura compatibilidad del runner con versiones anteriores no verificadas.

## Diagnóstico rápido

| Resultado | Qué revisar |
|---|---|
| Error de conexión | Backend encendido, URL/puerto y CORS. El navegador no distingue de forma fiable CORS de red desde fetch. |
| 400 | Request Payload y fieldErrors; comparar con DTO. |
| 401 | Iniciar sesión otra vez. |
| 403 | Rol/actividad del usuario en el backend. |
| 404 | Ruta exacta y recurso existente. |
| 409 | Email/valor único duplicado; en corrección de reserva, versión vieja intentando otro cambio. |
| 422 | Capacidad, fecha/turno, mesa, solapamiento, cliente inactivo o estado que no permite la operación. |
| 500 | Logs de Spring Boot; no modificar React a ciegas. |
| 200 con error de contrato | Comparar Response con los campos que proyecta el service. |
| Sin opciones para reservar | Crear/activar zona, mesa, turno y cliente mediante el backend. |

No hay Swagger UI instalado en este backend. En este incremento se verificaron rutas/JSON leyendo Controller y DTO y haciendo HTTP real. El procedimiento de evidencia Network y esa adaptación se explican en [guía 05](docs/guia-05.md).

## Estado y asincronía (guía 08)

Los GET de lista y detalle se sincronizan mediante efectos con dependencias estables, AbortController y una bandera que invalida resultados tardíos, incluidos errores/finally. Cancelar consulta, cambiar id o desmontar no debe abrir un formulario viejo ni mostrar AbortError como fallo del negocio. StrictMode sigue activo: su comprobación adicional no se oculta con un ref.

Guardar y confirmar bajas/cancelaciones siguen siendo eventos; no hay escrituras por montar componentes ni reintentos automáticos de POST. Lista/detalle, guardado y baja tienen indicadores separados. Los filtros y contadores siguen siendo derivados; los avisos de éxito desaparecen a los 5 segundos con limpieza del temporizador. Abortar una escritura no garantiza rollback en Spring.

Con Vite encendido, `/tests/hooks.html` permite ejecutar las 12 pruebas aisladas de hooks, sin login/API. No se incluyen en el build ni se ejecutan con npm test. Explicación, archivos y 40 respuestas: [guía 08](docs/guia-08.md).

## Búsqueda, filtros y paginación (guía 09)

Clientes busca nombre completo/email/teléfono/id y combina con Estado del cliente. Reservas busca id/nombre relacionado/mesa/fecha/observaciones y combina con cliente y los seis estados reales. No se agregan documento, apellido, vehículos ni activo a Reserva para copiar el ejemplo del docente.

Primero se filtra toda la lista, luego se calcula el total y se usa slice para mostrar la página. No hay listas derivadas duplicadas en useState ni peticiones por cambiar página. Texto/filtros/tamaño reinician a 1; una baja en la última página ajusta el límite. Cero resultados muestra EmptyState y 0–0 de 0. Limpiar filtros recupera la lista cargada.

Button y los otros siete componentes UI comparten tokens y no importan services. ConfirmDialog abre un dialog modal nativo: foco inicial seguro, fondo inerte, Escape/cancelar sin HTTP, restauración del foco al cerrar, error visible y bloqueo mientras confirma. Baja de cliente y cancelación de reserva conservan las reglas existentes. El modal móvil y teclado se verificaron en navegador.

La paginación es local; una futura migración al servidor requiere definir contenido/totales/filtros en la API. No se mandan parámetros de paginación inexistentes. Pruebas UI: `/tests/ui.html`. Detalle, límites y 40 respuestas: [guía 09](docs/guia-09.md).

## Alcance pendiente

Disponibilidad, check-in/reasignación de mesa y otros estados, autoservicio del cliente, carta/pedidos/reportes web completos y una estrategia de sesión más completa siguen pendientes según el contrato. El dashboard de guía 10 no sustituye ocupación/no-show. La corrección de reserva es limitada, no un editor irrestricto. Las guías 01–10 están implementadas en su adaptación, con límites de verificación documentados. Docker, GitHub, publicación e IA continúan aplazados; React Native no se modificó.

[Guía 10 y 50 respuestas](docs/guia-10.md) · [Matriz final](docs/evidencias/guia-10/matriz-fullstack.md) · [Build/preview/despliegue](docs/despliegue.md)

[Estado real](docs/PA-03-status.md) · [Guía 09 y defensa](docs/guia-09.md) · [Evidencias](docs/evidencias/guia-09/recorrido-http.md) · [Hooks, guía 08](docs/guia-08.md) · [Relación 1:N, guía 07](docs/guia-07.md) · [CRUD clientes, guía 06](docs/guia-06.md)
