# PA-03 GastroReserva — estado de cobertura

Última revisión: **2026-10-09**. Esta matriz distingue **backend principal** de **proyecto completo**. El alcance funcional del backend está implementado y verificado; no se marca como terminada una experiencia web/móvil por existir su API.

Por decisión del usuario, **Docker, GitHub Actions, despliegue e IA se dejan para el final**. El 2026-10-09 se autorizó compartir el código actualizado de backend y frontend en el repositorio GitHub existente. Compartir código no equivale a desplegar la aplicación ni a implementar CI.

## Backend principal

| Área | Estado | Evidencia |
|---|---|---|
| Java 21 / Spring Boot | Verificado | Compilación y empaquetado con Temurin 21; el PATH global puede requerir activar este JDK. |
| PostgreSQL / Flyway | Verificado en instancia temporal | V1–V9 desde cero, Hibernate validate y actualización V8→V9 conservando reservas/historial. Sin reescribir migraciones previas. |
| Usuarios / roles / clientes | Implementado | Registro, JWT, BCrypt, rol y actividad vigentes, DTO, perfil propio y administración. DELETE cliente con baja lógica/reactivación, preservando cuenta y reservas. GET /auth/me para frontend. |
| Zonas / mesas / turnos | Implementado | Catálogos, filtros, permisos y capacidades. Catálogos activos de disponibilidad accesibles al cliente. |
| Reservas / disponibilidad | Implementado | Reserva propia/operativa, capacidad, solapamiento, cancelación, estados, check-in, reasignación de mesa y corrección auditada de cliente/observaciones sólo SOLICITADA. |
| Carta | Implementado | Catálogo administrativo y sólo productos disponibles para consulta de clientes. |
| Mesa abierta | Implementado | Atención sin reserva, intervalo previsto, capacidad, protección ante conflicto y cierre explícito. |
| Pedidos / ítems / atención | Implementado | Origen único habilitado, responsable, precio/nombre histórico, total decimal, transiciones y auditoría. |
| Finalización | Implementado | No cierra visita con pedido pendiente; libera mesa después de cierre/cancelación de pedido. |
| Feedback | Implementado | Una opinión por reserva propia finalizada, puntuación 1–5, privacidad y consulta administrativa. |
| Reportes | Implementado | Ocupación actual y reservas/no-show por rango y turno; fórmulas documentadas y probadas. |
| Notificaciones | API de consulta implementada | Eventos de reservas propias por cursor, sin notas internas. No hay push ni experiencia móvil todavía. |
| Integración frontend | Parcial conectada | Proyecto web separado: login mínimo, CRUD de clientes, mesas de consulta y Cliente 1:N Reserva: alta, filtros, detalle/historial, corrección limitada y cancelación. Guías 08–09 organizan hooks/componentes/filtros/páginas/modales. Guía 10 agrega dashboard básico derivado, ErrorBoundary, Vitest y preparación de build/despliegue. El resto de experiencias web/móvil y publicación sigue pendiente. |
| Guía y demostración | Disponible | README, docs/09-backend-handoff.md, recorrido-backend.http y mesas-abiertas.http. |

## Reglas y trazabilidad

| Regla | Cobertura backend | Pruebas |
|---|---|---|
| RN-01 capacidad mesa/turno | Completa | Límites y dos altas concurrentes en mesas distintas, sin exceder capacidad. |
| RN-02 no solapar mesas | Completa | Intervalos de reservas y aperturas; carrera de reservas sobre una misma mesa. |
| RN-03 estados de reserva | Completa | Transiciones válidas, estados terminales y check-in específico. |
| RN-04 habilitación de pedido | Completa | Sólo SENTADA o apertura vigente, origen exclusivo y un pedido por visita; duplicados concurrentes. |
| RN-05 precio histórico | Completa | Copia al agregar, cambios posteriores no alteran ítems; agregados simultáneos sin pérdida. |
| RN-06 total operativo | Completa | BigDecimal, cantidades y totales exactos; sin pagos ni facturación. |
| RN-07 feedback posterior | Completa | Propiedad, FINALIZADA, validación y unicidad incluso con solicitudes simultáneas. |
| RN-08 reasignación trazable | Completa | Mesa anterior/nueva, motivo, actor y fecha; pedido sigue asociado a la visita. |

| Requisito / historia | Backend | Pendiente fuera del backend |
|---|---|---|
| RF-01 / HU-01 | Autenticación, permisos y sesión actual verificados. | Login web mínimo conectado; gestión completa de sesión y móvil pendientes. |
| RF-02 a RF-04 | Filtros, validación y auditoría de reservas/pedidos. | Carga/vacío/error, búsqueda y filtros combinados, paginación local de Clientes/Reservas y detalle/historial disponibles; otras interfaces pendientes. |
| RF-05 a RF-09 / HU-02 a HU-07, HU-13 a HU-15 | Clientes, salón, turnos, reserva y check-in. | CRUD web de clientes y gestión administrativa limitada de reservas conectados (guía 07); check-in, autoservicio y resto de interfaces pendientes. |
| RF-10 / HU-08, HU-16 | Carta/productos disponibles. | Pantallas de catálogo/carta. |
| RF-11, RF-18 / HU-09, HU-10 | Pedido, ítems y total histórico. | Pantalla de pedido. |
| RF-12 / HU-11, HU-12 | Atención/finalización protegida y auditada. | Experiencia operativa. |
| RF-13 / HU-17 | Opiniones autorizadas, persistidas sin IA. | Formulario y consulta visual. |
| RF-14, RF-20 / HU-18 | Indicadores de ocupación/no-show. | Panel web parcial: guía 10 muestra totales de clientes/reservas y estados, no consume aún los reportes de ocupación/no-show ni sus filtros. |
| RF-15, RF-16 | Capacidad y solapamiento, con evidencia concurrente. | Mostrar resultados al usuario. |
| RF-17 | Check-in/reasignación. | Interfaz host. |
| RF-19 | API de cambios y pedidos del responsable. | React Native del mesero; requisito completo aún parcial. |
| HU-19 | Consulta privada de eventos disponible. | Notificaciones visibles en móvil; no se implementó push. |
| HU-20 / RNF-13 | Aplazado, sin llamadas a proveedor. | Puerto/adaptador IA, timeout, fallback y clasificación. |

## Evidencia y alcance de las pruebas

- Guía 10: **79 Node + 19 Vitest/Testing Library** aprobadas, lint/build correctos y recorrido real de frontend compilado en preview contra Spring/PostgreSQL temporal. Dashboard, CRUD/FK, corrección/cancelación/historial, 409, recargas SPA y error de conexión verificados; boundary probado aisladamente. Maven verify con Java 21: **129 H2**, cero fallos y BUILD SUCCESS; sin cambios Java/SQL. Se preparó publicación, no se publicó. Guía y 50 respuestas: `C:/Users/antel/WebstormProjects/gastroreserva1/docs/guia-10.md`; matriz y límites (Network, defensa oral, viewport móvil no aplicado): `docs/evidencias/guia-10/matriz-fullstack.md` de ese frontend.

- Guía 09, sólo frontend: **79 pruebas Node, 8 UI React y 12 hooks React/StrictMode** aprobadas; lint/build correctos. Navegador contra API/PostgreSQL temporal: filtros/páginas/vacío, baja y ajuste de última página, reactivación/alta cliente, corrección/cancelación/historial reserva, modal/teclado/foco y responsive. Sin cambios Java/SQL ni Maven nuevo. Guía y 40 respuestas en `C:/Users/antel/WebstormProjects/gastroreserva1/docs/guia-09.md`.

- Guía 08, sólo frontend: **67 pruebas Node y 12 pruebas de hooks React/StrictMode** aprobadas, lint/build correctos. Navegador contra API/PostgreSQL temporal: cancelación de GET, error HTTP controlado/reintento y CRUD conservado. Sin cambios Java/SQL, por lo que no se reejecutó Maven; las 129 pruebas siguientes son evidencia de guía 07. Detalle y 40 respuestas en `C:/Users/antel/WebstormProjects/gastroreserva1/docs/guia-08.md`.

- Último incremento guía 07: **129 pruebas aprobadas en PostgreSQL 17 temporal**, cero fallos/errores/omitidas y `BUILD SUCCESS`, incluida toda la suite anterior. H2 pasó 128 pruebas antes de añadir el caso adicional de actualización V8→V9. Frontend: 58 pruebas, lint y build correctos, más recorrido de navegador con otra base aislada.
- La suite incorpora pruebas HTTP con JWT real de pedidos, aperturas, feedback, reportes, notificaciones, sesión y CORS.
- Comprueba 400/401/403/404/409/422, datos ajenos, responsables y roles vigentes.
- Las carreras usan dos hilos y transacciones confirmadas, no una única transacción simulada; esquema exclusivo de pruebas.
- Se comprueba migración limpia V1–V9, actualización V5→V9 y actualización V8→V9 con reserva/historial anteriores conservados.
- La verificación PostgreSQL usa binarios locales en una instancia efímera separada, sin Docker ni claves del usuario. El script siempre solicita apagar esa instancia al terminar.
- No se ejecutaron los nuevos ejemplos manuales contra la base habitual ni se reinició la aplicación del usuario. Las migraciones pendientes, incluida V9, se aplicarán al siguiente arranque; hacer respaldo si los datos son importantes.
- No se garantiza rendimiento bajo cualquier carga; los casos concurrentes son pruebas de invariantes, no un benchmark productivo.

## Pendientes del proyecto completo

| Área | Estado |
|---|---|
| React + TypeScript web | Guías 01–10 adaptadas: CRUD clientes, gestión limitada de reservas, hooks/UI/filtros/páginas/modales, dashboard básico, ErrorBoundary y pruebas. Pendientes autoservicio, atención y demás experiencias del contrato; los indicadores básicos no completan reportes. Swagger UI no instalado; Network/defensa oral y nueva revisión visual móvil del dashboard pendientes. No equivale al 100% del proyecto. |
| React Native + TypeScript | Aplicación pendiente. |
| Docker / Compose | Aplazado a pedido del usuario. |
| Repositorio compartido / GitHub Actions | Entrega conjunta del backend en raíz y React en frontend/, con INICIO-EQUIPO.md y secretos excluidos. GitHub Actions y despliegue siguen pendientes. |
| Spring AI | Aplazado; feedback y reportes funcionan sin proveedor. |
| Colección Postman/Bruno/Insomnia de entrega | Pendiente si el docente exige ese formato; existen ejemplos IntelliJ y pruebas automatizadas. |

## Próximo incremento

Con las diez guías adaptadas, elegir el próximo recorrido del producto sobre [09-backend-handoff.md](09-backend-handoff.md): autoservicio del cliente con disponibilidad/reserva, o atención del restaurante. Revisar también el dashboard en móvil y practicar la demostración. Despliegue/Docker/CI/IA siguen aplazados. No confundir “backend principal cerrado” o “diez guías implementadas” con “PA-03 completo”.

## Registro de verificación

### 2026-10-09 — preparación de entrega conjunta al grupo

- Backend existente en raíz y copia del frontend verificado en frontend/, sin mover ni borrar el original de WebStorm. README e INICIO-EQUIPO.md explican dependencias, JDK 21, base propia, variables privadas, administrador local y datos iniciales.
- Los archivos HTTP compartidos usan variables del entorno privado de IntelliJ. Se conservaron los originales como auth.local.http y clientes.local.http, excluidos por Git, y se añadió una plantilla privada vacía. No se cambiaron contraseñas del sistema ni datos de PostgreSQL.
- No se incluyen node_modules/dist/target/IDE ni la base del autor. Los archivos tmp ya presentes en el historial anterior se preservan, pero se ignoran nuevas incorporaciones de esa carpeta. Sin reescribir historial ni migraciones aplicadas.
- Se detectó una credencial administrativa en un commit previo; quitarla del ejemplo actual no la borra de versiones anteriores. Se advirtió al usuario que cambie esa contraseña si sigue vigente. La entrega no habilita GitHub Actions, Docker ni hosting.
- Verificación de la copia entregable: instalación limpia con npm ci, 79 Node + 19 Vitest, lint/build correctos; backend Maven verify con Java 21, 129 pruebas H2 y BUILD SUCCESS. Escaneo de archivos candidatos sin coincidencias de las credenciales privadas conocidas ni patrones de tokens/claves; no sustituye auditoría exhaustiva del historial.

### 2026-10-09 — frontend web, guía 10: integración final

- Dashboard básico derivado de GET autorizados, MetricCard genérico, carga/vacío/error/reintento y seis estados reales. RF-02/RNF-09; RF-14/RF-20/HU-18 web siguen parciales.
- ErrorBoundary raíz y demostración aislada; 19 Vitest con jsdom/Testing Library sumadas a las 79 Node. Lint/build correctos; npm audit sin vulnerabilidades reportadas. Verificación Maven Java 21: 129 H2, BUILD SUCCESS.
- Preview con API/PostgreSQL temporal: F01–F12 adaptados, relación y auditoría preservadas, 409 real y error de red visible. Sin modificar Java, SQL, permisos ni datos habituales. Servicios temporales apagados y pruebas conservadas.
- `.env.production` sólo para build local, plantilla SPA y procedimiento HTTPS/CORS sin publicación. Documentación y 50 respuestas en el frontend de WebStorm; Network/defensa oral y comprobación móvil no certificadas. Docker/GitHub/IA aplazados.

### 2026-10-09 — frontend web, guía 09: búsqueda, filtros y modales

- RF-02/RNF-09, preservando RF-05/HU-06 y RF-08/HU-15: ocho componentes UI independientes del dominio, búsquedas adaptadas, filtros AND y campos OR, paginación 5/10/20 posterior al filtrado, límites/reset y EmptyState. Reserva conserva sus seis estados; no se agregaron vehículos, documento ni activo a su modelo.
- ConfirmDialog/Modal nativos, selección pendiente, foco inicial seguro y restauración con fallback, Escape/cancelar sin HTTP, bloqueo de doble envío/cierre durante escritura y errores visibles. Baja lógica del cliente y cancelación de reserva mantienen los contratos existentes. RN-01/RN-02/RN-03/RN-08 intactas.
- 79 pruebas Node, 8 UI React y 12 hooks React aprobadas; lint/build correctos. Recorrido real de navegador en base temporal: filtro combinado y páginas, baja de última fila con ajuste 3→2, reactivación y alta cliente, corrección de titular, cancelación con motivo e historial de reserva y modal a 390 px. Consola sin errores capturados.
- Documentación y 40 respuestas en `C:/Users/antel/WebstormProjects/gastroreserva1/docs/guia-09.md`, evidencias en `docs/evidencias/guia-09` del frontend. Defensa oral no certificada. El desarrollo queda en el proyecto de WebStorm; este repositorio sólo actualiza documentación en este incremento.
- Sin cambios Java/SQL/REST/permisos/dependencias/.env. No se reejecutó Maven ni se tocaron datos habituales. Servicios de prueba apagados al finalizar, archivos temporales conservados. Docker/GitHub/IA siguen aplazados; guía 10 pendiente de recibir.

### 2026-10-08 — frontend web, guía 07: Cliente 1:N Reserva

- Adaptación de RF-02/RF-03/RF-04/RF-08, HU-06/HU-15 y RNF-05/RNF-09, preservando RN-01/RN-02/RN-03/RN-08. No se copiaron vehículos, placas ni eliminación física al dominio de reservas.
- Web en `C:/Users/antel/WebstormProjects/gastroreserva1`: carga paralela autorizada, Map por cliente, filtro local, alta, GET de detalle/historial, formulario de corrección y cancelación confirmada; sin GET por fila ni cambios locales ficticios ante error. MESERO usa cliente proyectado en agenda sin consultar el catálogo protegido.
- Nuevo PUT `/api/reservas/{id}/datos-cliente` para ADMINISTRADOR/HOST, sólo SOLICITADA, cliente activo y motivo. `version` en DTO protege contra edición obsoleta (409); repetición idéntica no agrega eventos. Programación, capacidad y creador se conservan. No habilita edición irrestricta, reprogramación, DELETE físico ni reactivación de CANCELADA.
- V9 añade historial de cliente/observaciones anteriores y nuevos, FKs/checks/índices. V1–V8 intactas. Pruebas de actualización V8→V9 verifican conservación de reserva y evento existentes; permiso/propiedad/estado/errores comprobados por HTTP.
- Frontend: **58 pruebas**, lint y build correctos. Backend: verify H2 de 128 pruebas antes del último caso de migración; suite final completa de **129 pruebas en PostgreSQL 17**, cero fallos/errores/omitidas, BUILD SUCCESS. Comando: `scripts/verify-postgres.ps1 -MavenRepository C:/Users/antel/.m2/repository -Offline`, JDK 21. Reportes conservados en `C:/Users/antel/AppData/Local/Temp/gastro-pg-test-3fc09e51a6f147098a54b33cadac42f5`.
- Navegador + API 18082/PostgreSQL aislados: relación 2:1 de tres reservas iniciales, filtros/vacío sin HTTP adicional, alta 201, edición descartada sin PUT, corrección 200 sin duplicar reserva, auditoría antes/después, cancelación 200 manteniendo fila, conflicto 409 conservando texto y GET de persistencia. Evidencias y 40 respuestas adaptadas en `docs/guia-07.md` del frontend. No son capturas de DevTools Network; éstas y defensa oral quedan para el estudiante.
- Sin modificar base habitual, credenciales, dependencias, Docker, IA ni GitHub. Se requiere reiniciar el backend del usuario para aplicar V9 y cargar el contrato. Guías 08–10 pendientes, no se marca toda la web terminada.

### 2026-10-08 — frontend web, guía 06

- RF-02/RF-03/RF-05, HU-06 y RNF-09: CRUD administrativo de clientes en el proyecto web separado. GET por id antes de editar, único formulario POST/PUT, cancelación sin mutación, errores conservando campos, sustitución por id, DELETE confirmado y filtros Activos/Inactivos/Todos con reactivación.
- Se agregó DELETE /api/clientes/{id} → 204 sin body: baja lógica transaccional, idempotente, sin borrar reservas/historial ni desactivar la cuenta vinculada. PUT permite reactivar. No se alteraron RN-01 a RN-08; nuevas reservas siguen requiriendo cliente activo, capacidad y ausencia de solapamientos.
- Pruebas HTTP nuevas: ADMINISTRADOR/HOST, rechazo 401/403, 400/404/409, idempotencia PUT/DELETE, conservación de reserva/historial/usuario y rechazo de nueva reserva para un cliente inactivo.
- Verificación final JDK 21 / Maven offline verify: **118 pruebas H2, 0 fallos, 0 errores, 0 omitidas, BUILD SUCCESS**. Frontend: **44 pruebas**, lint y build correctos.
- Navegador → Spring Boot 18081 → PostgreSQL 17 temporal: alta 201, detalle 200, PUT 200/409, cancelación, DELETE 204, reserva conservada, exclusión de inactivos y reactivación. GET/DELETE 404 reales se demostraron retirando únicamente un perfil ficticio sin reservas de esa base aislada. No se usó ni alteró la base habitual.
- Documentación frontend en docs/guia-06.md: adaptación explícita a baja lógica, contratos, recorrido, capturas UI y 35 respuestas. No se afirma evidencia DevTools Network ni Swagger UI; defensa oral/capturas Network pendientes del estudiante. La suite completa de 118 no se repitió contra PostgreSQL; allí se ejecutó el recorrido HTTP.
- Sin migraciones nuevas (activo ya existía), cambios de permisos/CORS, dependencias, commits, push, Docker ni IA. Servicios de prueba apagados al finalizar; logs/datos temporales conservados.

### 2026-10-08 — frontend web, guía 05

- Proyecto separado en `C:/Users/antel/WebstormProjects/gastroreserva1`: URL pública VITE_API_URL, apiClient con fetch/JSON/response.ok, services, GET reales y POST cliente/reserva operativa. Adaptación Cliente 1:N Reserva con catálogos reales e IDs numéricos.
- RF-01/RF-02/RF-03/RF-05/RF-08 y RNF-09: login JWT en memoria, guardas por rol, loading/error/vacío/submitting, errores por campo, bloqueo de duplicados, respuesta del servidor incorporada a tabla y campos conservados al fallar. RN-01/RN-02/RN-03 siguen en backend; no se agregó autoservicio ni transiciones web.
- Se retiraron del formulario conectado los ejercicios locales no contractuales de guía 04. Mocks conservados sólo como referencia/pruebas; no son fallback de API.
- Se detectó que una ruta desconocida autenticada devolvía 500. ApiExceptionHandler ahora responde 404 ROUTE_NOT_FOUND para NoResourceFoundException/NoHandlerFoundException, sin alterar el 401 anónimo. Prueba de regresión agregada.
- Verificación final backend con JDK 21 y Maven offline: **112 pruebas H2, 0 fallos, 0 errores, 0 omitidas, BUILD SUCCESS**. Frontend: **32 pruebas aprobadas**, lint y build correctos.
- Recorrido navegador → Spring Boot → PostgreSQL 17 temporal: altas 201 de ambas entidades, GET persistidos incluso tras reiniciar API, 409/422, permisos mesero, CORS permitido/rechazado, body 400, ruta 404 y conexión caída. No se alteró el PostgreSQL habitual ni se usaron credenciales reales; no se repitió la suite completa de 112 casos contra PostgreSQL.
- Evidencias/contratos/30 respuestas en `docs/guia-05.md` del frontend. Swagger UI no está instalado: contraste realizado con Controller/DTO y HTTP real. Capturas DevTools Network y defensa oral quedan explícitamente pendientes; no se presentan capturas de UI como evidencia de esa pestaña.
- No hubo nuevas migraciones, dependencias, cambios de permisos/CORS, commits, push, Docker ni IA.

### 2026-10-08 — frontend web, guía 04

- Proyecto separado en `C:/Users/antel/WebstormProjects/gastroreserva1`: formularios controlados de clientes y reservas, tipos de estado/objeto de envío separados, validadores puros, errores por campo, Limpiar, contador y espera simulada de 500 ms con bloqueo/cancelación.
- Adaptación de RF-03/RF-05/RF-08 y RNF-09 sólo a nivel visual. Select de clientes activos y capacidad individual local; disponibilidad, solapamiento, permisos y estados siguen bajo autoridad backend. No se crea ningún registro ni se alteran las tablas mock.
- Documento/dirección/checkbox y referencia normalizada cumplen la práctica docente como datos locales explícitamente excluidos de los contratos REST; no se ampliaron entidades ni migraciones. La integración futura deberá separar estos ejercicios del formulario real.
- Frontend: build/lint correctos, 15 pruebas unitarias aprobadas; recorrido manual de errores/éxito, normalización, IDs, limpieza, bloqueo, cancelación y pantallas de 1366/390/320 px. Detalle y evidencias en `docs/guia-04.md` del frontend.
- No se cambió código Java, contratos, permisos ni PostgreSQL; no se reejecutó Maven. Guías 05–10 pendientes de recibir; Docker, GitHub e IA siguen aplazados.

### 2026-10-08 — frontend web, guías 01–03

- Proyecto separado en `C:/Users/antel/WebstormProjects/gastroreserva1`; su documentación y evidencia están en `docs/guia-03.md` y `docs/PA-03-status.md` de esa carpeta.
- Guía 03 adaptada a Cliente 1:N Reserva: interfaces, arreglos tipados, tablas con props/map/key/find, estados, estadísticas y vacíos. 6 clientes y 8 reservas simuladas; sin consumir API ni alterar PostgreSQL.
- Trazabilidad parcial de RF-02/RF-05/RF-08 y RNF-09: representación visual. Estados coherentes con RN-01/RN-03, sin sustituir validaciones backend ni marcar gestión real completa.
- Build y lint del frontend correctos; navegación, listas, vacío, relación ausente, cambio de una fila y responsive verificados. Prueba intencional de TS2322 ejecutada y revertida.
- No se modificó código Java ni migraciones en este incremento; no se reejecutó Maven. Formularios, autenticación web, integración HTTP, móvil e infraestructura siguen pendientes.

### 2026-08-15 — incremento de base técnica

- Línea base previa: `mvn test` compiló con Java 21, pero falló la única prueba porque dependía de PostgreSQL y no había credencial disponible.
- Corrección de reproducibilidad: el wrapper de Windows quedó compatible con una carpeta `.m2` normal y se documentó cómo activar el JDK 21 ya instalado.
- Migración: Flyway ejecuta `V1__create_zonas_and_mesas.sql` sobre una base efímera y Hibernate valida el esquema resultante.
- Comando de verificación: `.\mvnw.cmd test`.
- Resultado: compilación exitosa y pruebas sin fallos después de integrar el starter Flyway requerido por Spring Boot 4.

### 2026-08-15 — contratos de zonas y mesas

- Se sustituyó la exposición directa de JPA por DTO de entrada/salida y una capa de servicios transaccional.
- Se agregaron filtros, consulta por id, creación y actualización para zonas y mesas.
- Se normalizan textos, se impiden nombres de zona y números de mesa duplicados y se exige una zona existente.
- Los errores de validación, inexistencia y conflicto usan respuestas REST uniformes con estados `400`, `404` y `409`.
- Verificación: `.\mvnw.cmd verify` finalizó con **13 pruebas, 0 fallos y 0 errores**.

### 2026-08-15 — autenticación, usuarios y roles

- Flyway V2 incorpora usuarios sin mezclar el concepto de seguridad con la futura entidad de negocio `Cliente`.
- Se implementaron los roles `ADMINISTRADOR`, `HOST`, `MESERO` y `CLIENTE`, contraseñas BCrypt, registro público limitado a cliente y login JWT HS256.
- Un administrador puede listar, filtrar, crear, actualizar y cambiar contraseñas de usuarios; ningún contrato expone hashes.
- Zonas y mesas permiten consulta a administrador/host/mesero y escritura sólo a administrador; cliente y anónimo reciben `403`/`401` JSON.
- Cada petición protegida vuelve a resolver en base de datos el estado y rol vigentes, por lo que los cambios de permisos tienen efecto inmediato.
- Verificación intermedia: `.\mvnw.cmd test` finalizó con **19 pruebas, 0 fallos y 0 errores**.

### 2026-08-15 — clientes y turnos

- Flyway V3 incorpora `clientes` y `turnos` con unicidad, relaciones, capacidad positiva, estado y marcas temporales.
- El registro público crea o vincula el perfil de negocio por correo sin mezclarlo con las credenciales; el cliente puede consultar y actualizar únicamente su perfil.
- Administrador y host pueden consultar, filtrar, crear y actualizar clientes; las respuestas nunca exponen la entidad JPA ni datos de contraseña.
- Los turnos admiten consulta y filtros para administrador/host/mesero, mientras la escritura queda limitada al administrador; se validan capacidad positiva y ventana horaria no vacía.
- Verificación: `.\mvnw.cmd test` finalizó con **27 pruebas, 0 fallos y 0 errores**.

### 2026-08-15 — disponibilidad y reservas

- Flyway V4 incorpora `reservas` e `historial_reservas`, estados contractuales, intervalos, versión optimista, relaciones, restricciones e índices operativos.
- La consulta de disponibilidad combina mesas activas, zona, capacidad individual, capacidad restante del turno y reservas que ocupan el intervalo.
- La creación bloquea turno y mesa antes de validar y persistir, impide superar capacidades y rechaza solapamientos de estados activos.
- Cliente crea/lista/cancela únicamente reservas propias; administrador/host registra reservas operativas y administrador/host/mesero consulta agenda y transiciona estados.
- Cada creación y transición registra estado anterior/nuevo, motivo, usuario y fecha; se probaron secuencia válida, estado final inmutable y autoría.
- Verificación final: `.\mvnw.cmd verify` finalizó con **35 pruebas, 0 fallos y 0 errores** y generó el JAR ejecutable.

### 2026-08-15 — documentación del Parcial 1

- Se consolidó el material inicial de visión, glosario y backlog sin modificar la carpeta original del Escritorio.
- `docs/README.md` indexa visión/contexto, actores, catálogo RF/RNF, RN-01 a RN-08, casos de uso/mapa, flujo crítico, modelo conceptual, DER, diccionario, arquitectura y backlog con aceptación.
- Los documentos distinguen alcance contractual, diseño futuro y evidencia realmente implementada.
- `AGENTS.md` conserva las reglas de trabajo y stack para futuras continuaciones.
- Verificación documental: **10 archivos Markdown**, más de **10 000 palabras**, **0 enlaces relativos rotos** y credencial administrativa restaurada a marcador seguro en `auth.http`.

### 2026-08-20 — check-in y reasignación trazable

- Flyway V5 amplía `historial_reservas` con tipo de evento, mesa anterior y mesa nueva, agrega sus claves foráneas/índices y restringe el estado operativo de mesa a `DISPONIBLE` u `OCUPADA`.
- `PATCH /api/reservas/{id}/check-in` permite a administrador/host pasar únicamente una reserva `CONFIRMADA` a `SENTADA`, conservando o cambiando mesa después de validar actividad, capacidad, disponibilidad operativa y solapamiento.
- `PATCH /api/reservas/{id}/mesa` reasigna reservas `CONFIRMADA` o `SENTADA`, exige motivo y conserva ambos extremos, actor y fecha en un evento append-only.
- Reserva y mesas se bloquean pesimistamente; las mesas se adquieren en orden de identificador para reducir interbloqueos durante operaciones concurrentes.
- El endpoint genérico ya no puede producir `SENTADA`; finalizar una visita sentada libera la mesa ocupada.
- Verificación final: `.\mvnw.cmd verify` finalizó con **38 pruebas, 0 fallos y 0 errores** y generó el JAR ejecutable.

### 2026-08-24 — auditoría y correcciones del backend actual

- Se corrigió la declaración dañada de `UsuarioController`, que impedía compilar el proyecto.
- RN-08 ahora exige el motivo de reasignación también dentro de `ReservaService`, además de la validación del contrato HTTP, para conservar la regla en la autoridad de negocio.
- Se agregó una prueba que rechaza motivos vacíos y comprueba que no cambien ni la mesa ni el historial.
- Se corrigieron contradicciones documentales sobre RN-08 y los estados operativos de mesa.
- Verificación final: `.\mvnw.cmd verify` finalizó con **39 pruebas, 0 fallos y 0 errores** y generó el JAR ejecutable.

### 2026-09-30 — carta básica y productos del menú

- Alcance: RF-10, HU-08 y HU-16, con filtros RF-02, validaciones RF-03 y DTO/permisos. La carta es la base del futuro pedido; no se marca RN-04, RN-05 ni RN-06 como implementada.
- Flyway V6 agrega `productos_menu`, precio `NUMERIC(12,2)`, disponibilidad, marcas temporales y nombre normalizado único. No se reescribieron V1–V5.
- `/api/productos-menu` permite al administrador crear, consultar, filtrar y actualizar productos. `/api/carta` permite a todos los roles autenticados consultar exclusivamente productos disponibles, incluyendo la consulta por id.
- Se rechazan nombres vacíos/duplicados, precios no positivos, exceso de precisión y campos inválidos. La baja es lógica mediante `disponible=false`, sin eliminar identidades.
- Se agregaron 27 pruebas: servicio/persistencia, validación directa, unicidad en base, filtros literales, disponibilidad, JWT administrativo, roles y respuestas 400/401/403/404/409. La prueba de migraciones se actualizó a V6 y verifica que no haya pendientes ni fallos de validación.
- Verificación final con JDK 21: `.\mvnw.cmd '-Dmaven.repo.local=C:/Users/antel/.m2/repository' -o clean verify` finalizó con **66 pruebas, 0 fallos, 0 errores y 0 omitidas**, `BUILD SUCCESS` y JAR ejecutable.
- Se usó la caché local de Maven explícita porque el entorno restringido intentaba usar `C:/.m2/repository`; la compilación necesitó acceso normal a esa caché. No se cambió la configuración del proyecto para resolverlo.
- Las pruebas usan H2 temporal en modo PostgreSQL. No se ejecutó V6 sobre el PostgreSQL del usuario ni se reinició su aplicación; se aplicará al próximo arranque. La validación en PostgreSQL real queda pendiente.
- `carta.http` contiene el recorrido manual, guarda el id automáticamente e incluye errores esperados. Los cambios previos del usuario en `auth.http` y `clientes.http` se preservaron; no se agregaron credenciales, commits ni publicaciones.
- Se actualizaron API, permisos, diccionario/DER, requisitos y backlog; se corrigieron notas obsoletas sobre la existencia de Git y Node. No se modificó el frontend.
- Siguiente incremento: mesa abierta y pedidos con ítems, precio histórico y total operativo (RN-04 a RN-06); después estados de atención, feedback y reportes.

### 2026-10-08 — cierre del backend principal

- Se verificó y consolidó el incremento iniciado previamente: V7 para aperturas/pedidos/ítems/auditoría, V8 para feedback; reglas RN-04 a RN-07 y soporte de RF-11 a RF-14, RF-18 a RF-20 y HU-19.
- Se integraron permisos por rol y responsable, bloqueo transaccional del origen, estados terminales, precios históricos y protección de finalización con pedidos pendientes. Reserva/mesa abierta/pedido quedan coordinados sin agregar pagos ni facturas.
- Sesión actual, opciones de reserva para el cliente, CORS y zona horaria configurables preparan el consumo desde React y React Native.
- Se corrigió la consulta del reporte de ocupación: totales y ocupadas se obtienen en un mismo snapshot SQL en vez de dos lecturas independientes.
- `BackendPrincipalHttpIntegrationTest`: 11 escenarios con JWT real y HTTP, incluidos flujo completo, errores, acceso ajeno, cambio de rol/actividad, responsable y CORS.
- `ConcurrencyIntegrationTest`: 13 escenarios con dos hilos/transacciones y esquema aislado; capacidad/solapamiento, pedido único, ítems sin pérdida, apertura única, feedback único y pedido contra finalización.
- `MigrationUpgradeIntegrationTest`: esquema temporal en V5 con dato existente, actualización a V8 y comprobación de conservación/checksums. La prueba habitual comprueba también migraciones desde cero.
- Comando H2: `.\mvnw.cmd '-Dmaven.repo.local=C:/Users/antel/.m2/repository' -o verify`, Temurin 21. Resultado a las 13:00 (America/La_Paz): **111 pruebas, 0 fallos, 0 errores, 0 omitidas, BUILD SUCCESS**.
- Comando PostgreSQL: `.\scripts\verify-postgres.ps1 -MavenRepository C:/Users/antel/.m2/repository -Offline`. PostgreSQL 17 temporal, misma suite: **111 pruebas, 0 fallos, 0 errores, 0 omitidas, BUILD SUCCESS** a las 13:03. Instancia apagada correctamente; logs y reportes conservados en la ruta temporal informada por el script.
- Se actualizaron documentos 01–08, índice, README y matriz; se agregó `docs/09-backend-handoff.md` con contratos, fórmulas y límites explícitos. `recorrido-backend.http` y `mesas-abiertas.http` preparan demostración con ids automáticos. Los `.http` se entregan para ejecución manual; no se ejecutaron sobre los datos del usuario.
- Se preservaron los cambios anteriores de `auth.http` y `clientes.http`; revisar sus credenciales antes de una publicación futura. Se ignoró `http-client.private.env.json`. No se hicieron commits, push, Docker ni integración IA, y no se modificó el frontend ni el PostgreSQL habitual.
- **Cierre:** backend principal funcional y verificado para comenzar integración frontend. El proyecto académico completo sigue pendiente de interfaces, móvil, infraestructura e IA aplazadas.
