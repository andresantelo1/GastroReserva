# GastroReserva — avance del frontend

Fecha: 2026-10-09. Implementación incremental de guías 01–10, con las adaptaciones y pendientes académicos indicados abajo. Cierra esta secuencia didáctica, no equivale al 100% del producto ni acredita por sí sola la defensa oral del estudiante.

| Área | Estado real |
|---|---|
| Stack y estructura | React, TypeScript, Vite, React Router; módulos, layout y rutas de guías anteriores conservados. |
| Datos y tablas | GET real de clientes y reservas; sin fallback a mocks. Proyección de DTO anidado a vista plana. |
| Clientes | CRUD para ADMINISTRADOR/HOST: listado, GET detalle, alta, edición, baja lógica confirmada, filtros por estado y reactivación. No crea una cuenta de acceso ni borra reservas. |
| Reservas | Agenda, filtro por cliente, detalle/historial. ADMINISTRADOR/HOST crean, corrigen cliente/observaciones sólo SOLICITADA con motivo/versión y cancelan SOLICITADA/CONFIRMADA. No hay DELETE físico ni reprogramación; MESERO sólo consulta en esta web. |
| Mesas | Consulta de mesas activas; no es un buscador de disponibilidad por fecha. |
| Login | POST real, JWT sólo en memoria, logout, caducidad y manejo de 401. Recargar requiere ingresar otra vez. |
| Formularios | Validación local, loading/submitting, bloqueo de duplicados, API errors visibles, reset sólo tras éxito, tabla con id del servidor. |
| HTTP | VITE_API_URL, fetch compartido, JSON, response.ok, 204, AbortController, timeout, errores de red/contrato y fieldErrors. |
| CORS | Configuración existente de Spring Security con orígenes exactos; preflight permitido/rechazado comprobado. |
| Estado/efectos/hooks | useAsyncList/useAsyncDetail y hooks de dominio; AbortController + invalidación de respuestas tardías, reload estable, carga/guardado/baja separados, éxito temporal y StrictMode conservado. |
| Componentes UI / filtros | Ocho componentes genéricos, búsqueda en Clientes/Reservas, filtros combinados, páginas 5/10/20 y modales nativos con foco/Escape. Resultados derivados, tokens CSS y diseño adaptable. |
| Dashboard | Cuatro métricas derivadas de clientes/reservas y seis estados, carga/vacío/error/reintento; sólo ADMINISTRADOR/HOST. Todas las fechas cargadas, no ocupación/no-show ni actualización en tiempo real. |
| Resiliencia y preparación | ErrorBoundary raíz, build/preview verificado, variables públicas y plantilla SPA. Sin publicación ni HTTPS real. |
| Verificación | Guía 10: 79 Node + 19 Vitest/Testing Library, lint/build correctos y recorrido de preview con PostgreSQL temporal. Maven verify: 129 pruebas H2 aprobadas. Sin cambios Java/SQL de este incremento. |
| Documentación del contrato | Controller/DTO y respuestas HTTP contrastados. Swagger UI no está instalado: no se marca ese punto literal verificado. |
| Evidencias académicas | Capturas de UI y registro HTTP disponibles. Capturas de la pestaña Network y defensa oral quedan para el estudiante. |
| Resto del proyecto | Autoservicio, check-in y otras experiencias web pendientes. Móvil pendiente. Docker/GitHub/publicación/IA aplazados. Defensa oral y capturas Network a realizar por el estudiante. |

## Trazabilidad y aceptación

- RF-01 / HU-01: ingreso real y permisos visuales mínimos para consumir una API protegida. No se reemplaza la autoridad del backend ni se implementa gestión completa de sesiones.
- RF-02 / RNF-09: listados reales con carga, vacío, error y reintento; búsqueda y filtros combinados en Clientes/Reservas; paginación local 5/10/20, límites y reinicio correctos. No se afirma paginación del servidor.
- RF-03 / RF-05 / HU-06: CRUD de clientes con contrato explícito, validación, confirmación, 404/409 y errores conservando el formulario. Baja lógica y reactivación mantienen id, cuenta y reservas.
- RF-06: lectura del catálogo activo de mesas; gestión de salón pendiente.
- RF-02/RF-04/RF-08/HU-06/HU-15: alta operativa, agenda/filtro por cliente, detalle/historial, corrección de cliente y observaciones sólo SOLICITADA y cancelación; clienteId numérico, Map sin N+1, estado decidido por backend.
- RN-01/RN-02/RN-03: Spring conserva capacidad, ausencia de solapamientos y estados. La corrección no cambia planificación; la cancelación libera capacidad y es terminal. Check-in y demás transiciones siguen sin UI. RN-08 de reasignación de mesa se conserva, no se sustituye por corrección de titular.
- RNF-08/RNF-09: arranque/configuración documentados, formularios accesibles, errores visibles y diseño adaptable conservado.

## Evidencia del incremento 10

[Guía y 50 respuestas](guia-10.md) · [Matriz F01–F12 y capturas](evidencias/guia-10/matriz-fullstack.md) · [Preparación de despliegue](despliegue.md).

Dashboard derivado y accesible por rol, MetricCard genérico, AppErrorBoundary y 19 pruebas Vitest sobre componentes, formularios, búsqueda/páginas, cargas/roles y recuperación. RF-02/RNF-09 y RNF-08; conserva RF-03/RF-05/RF-04/RF-08 y reglas existentes. **RF-14/RF-20/HU-18 en web siguen parciales**: hay resumen básico, no interfaz de los reportes de ocupación/no-show. No se cambiaron contratos, permisos, migraciones ni backend Java.

79 Node + 19 Vitest aprobadas, lint sin advertencias y build correcto. Preview contra PostgreSQL real: alta/edición de cliente, reserva con FK, corrección auditada, filtros/paginación, baja lógica, cancelación e historial, 409 y backend apagado. Recarga directa de rutas funciona con la reautenticación esperada. Maven verify con Java 21: 129 H2, BUILD SUCCESS. La suite completa PostgreSQL sigue siendo evidencia histórica de guía 07; no confundirla con el recorrido manual actual.

Prueba de boundary en página aislada: fallo y recuperación correctos. Nuevo CSS adaptable incluido; la emulación de viewport no se aplicó en esta sesión, por lo que la verificación visual móvil del dashboard queda pendiente. `.env.production` es local y pública, no despliegue. Hosting/HTTPS reales, Network y defensa oral pendientes. npm audit sin vulnerabilidades reportadas al cierre; source-map-js transitivo actualizado sin cambios forzados. Los servicios temporales se apagaron sin tocar la base habitual.

## Evidencia del incremento 09 (histórica)

[Detalle y 40 respuestas](guia-09.md) · [Recorrido y capturas](evidencias/guia-09/recorrido-http.md).

Button, SearchInput, SelectFilter, Pagination, Modal, ConfirmDialog, EmptyState y StatusBadge en components/ui, sin services/modelos de dominio. Búsqueda adaptada al DTO existente, relación mediante Map, filtros antes de slice, listas derivadas sin useState duplicado, reset/clamp de página y tokens CSS. Confirmaciones con foco seguro, Escape sin HTTP, restauración, error y bloqueo durante escritura.

Verificación de guía 09: 79 Node, 8 UI React/StrictMode y 12 hooks de regresión aprobadas; lint/build correctos. Navegador sobre API/PostgreSQL aislados comprobó páginas/filtros/vacío, baja de única fila final, reactivación/alta de cliente, corrección/cancelación/historial de reserva, foco/teclado y modal a 390 px. No se usaron credenciales ni datos habituales; sin cambios API, permisos, Java, SQL, dependencias o .env en aquel incremento. RN-01/RN-02/RN-03/RN-08 conservadas. Defensa oral pendiente.

## Evidencia del incremento 08 (histórica)

[Detalle y 40 respuestas](guia-08.md) · [Recorrido y capturas](evidencias/guia-08/recorrido-http.md).

Refactorización de cargas de Clientes/Reservas, selección por id sin duplicar el detalle, limpieza al cambiar/cerrar/desmontar y avisos de éxito con timer. Filtros y contadores ya eran derivados y se preservan. Las mutaciones permanecen en handlers y el backend sigue siendo autoridad final. No se modificaron API, permisos, migraciones, dependencias ni .env.

RF-02/RNF-09: recuperación de carga y mensajes coherentes. RF-03/RF-05/HU-06 y RF-04/RF-08/HU-15: conservación de clientes y reservas. RN-01/RN-02/RN-03/RN-08 sin cambios. Pruebas: 67 Node, 12 React/StrictMode; lint/build. Navegador comprobó cancelación de GET lento sin error falso, HTTP 404 controlado y reintento 200, CRUD cliente, alta/corrección/cancelación de reserva e historial. Sólo datos ficticios aislados.

## Evidencia del incremento 07 (histórica)

[Detalle y 40 respuestas](guia-07.md) · [Recorrido y capturas](evidencias/guia-07/recorrido-http.md).

PUT de corrección limitado, version en ReservaResponse y Flyway V9 para auditar referencias/observaciones anteriores/nuevas. No se reescriben migraciones aplicadas. GET y PATCH de estado existentes se consumen desde React; cancelar no equivale a borrar ni devuelve 204. Se preserva el proyecto previo y no se ampliaron dependencias, CORS ni infraestructura.

58 pruebas frontend + lint/build correctos. Suite final de 129 pruebas backend con PostgreSQL 17 temporal, BUILD SUCCESS. Cubre roles, datos ajenos, 400/404/409/422, estados, idempotencia y conservación al migrar. Navegador real verificó alta 201, edición 200, cancelar edición sin HTTP, cambio de cliente sin duplicar id, auditoría, filtro sin N+1, cancelación conservando fila y 409 de edición vieja conservando lo escrito. Guía 07 completada en su adaptación al dominio; no se afirma CRUD irrestricto, Swagger ni capturas Network realizadas.

## Evidencia del incremento 06 (histórica)

[Detalle y 35 respuestas](guia-06.md) · [Recorrido y capturas](evidencias/guia-06/recorrido-http.md).

Se agregó DELETE /api/clientes/{id} → 204 con baja lógica transaccional en Spring, bajo permisos ADMINISTRADOR/HOST existentes. No se cambiaron migraciones, CORS ni dependencias. GET/PUT ya existían; el frontend ahora los consume para detalle/edición con un único formulario, cancelación, actualización de la fila y filtro del estado guardado.

Pruebas finales: 44 frontend y 118 backend H2, sin fallos; lint/build y Maven verify correctos. Navegador conectado a una API 18081 y PostgreSQL 17 temporal comprobó POST 201, PUT 200/409, DELETE 204, reactivación, conservación de reserva, exclusión de inactivos de nuevas reservas y GET/DELETE 404 reales. Sólo se retiró un registro ficticio sin reservas de esa base aislada para generar el caso obsoleto; ningún dato habitual del usuario fue alterado.

No se ejecutó la suite completa de 118 contra PostgreSQL: allí se verificó HTTP real. Los servicios temporales se apagan al finalizar y se conservan logs/datos. Las capturas entregadas son de UI, no de DevTools Network; ese registro visual y la defensa oral del estudiante siguen pendientes. Swagger UI no está instalado.

## Evidencia del incremento 05 (histórica)

[Detalle y 30 respuestas](guia-05.md) · [Recorrido y capturas](evidencias/guia-05/recorrido-http.md).

Se creó una instancia PostgreSQL 17 temporal separada y una API en 18080 para pruebas de navegador con Vite 5174. Se confirmó POST cliente 201, POST reserva 201, persistencia después de GET/reinicio, 409, 422, permisos mesero, CORS y backend apagado. No se usaron contraseñas ni se alteraron datos del PostgreSQL habitual.

Un fallo detectado en backend (ruta desconocida producía 500) se corrigió a 404 ROUTE_NOT_FOUND, conservando 401 para acceso anónimo. Verify final: 112 pruebas, cero fallos/errores/omitidas y BUILD SUCCESS, con H2 temporal. No se afirma haber repetido las 112 pruebas completas contra PostgreSQL; allí se ejecutó el recorrido HTTP real.

Los ejercicios documento/dirección/referencia y espera de 500 ms de la guía 04 quedaron fuera del formulario conectado. No se cambió el contrato del negocio ni las migraciones.

## Historia

- [Guía 01](guia-01.md): estructura.
- [Guía 02](guia-02.md): navegación/layout.
- [Guía 03](guia-03.md): modelos, relación y mocks (históricos).
- [Guía 04](guia-04.md): formularios locales (históricos).
- [Guía 05](guia-05.md): GET/POST reales. Esta documentación reemplaza las afirmaciones anteriores de que Guardar no persiste.
- [Guía 06](guia-06.md): CRUD de clientes con baja lógica, reactivación y relación preservada.
- [Guía 07](guia-07.md): Cliente 1:N Reserva, filtro, corrección limitada auditada y cancelación.
- [Guía 08](guia-08.md): estado fuente/derivado, efectos con limpieza, asincronía y hooks reutilizables.
- [Guía 09](guia-09.md): componentes UI, búsqueda/filtros, paginación local y modales accesibles.
- [Guía 10](guia-10.md): dashboard, ErrorBoundary, Vitest, integración y preparación de build/despliegue.
