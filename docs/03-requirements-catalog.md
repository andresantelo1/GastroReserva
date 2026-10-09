# GastroReserva — catálogo trazable de requisitos

## Convenciones

- **Completo:** implementado y verificado en el alcance indicado.
- **Parcial:** existe implementación o documentación, pero falta al menos una experiencia, proceso o evidencia requerida.
- **Ausente:** todavía no implementado.
- **Bloqueado:** no puede verificarse en el entorno actual.

La matriz operativa detallada se mantiene en [PA-03-status.md](PA-03-status.md). Este catálogo define el contrato; la matriz de estado registra su avance.

Actualizado al 2026-10-08. El backend principal está implementado; “Parcial” se mantiene donde falta la experiencia web/móvil. Docker, CI e IA están aplazados por el usuario. Evidencia detallada en [09-backend-handoff.md](09-backend-handoff.md).

## Requisitos funcionales

| ID | Requisito | Actores | Reglas relacionadas | Evidencia o criterio verificable | Estado |
|---|---|---|---|---|---|
| RF-01 | Autenticar usuarios y resolver permisos según el rol vigente. | Todos | — | Registro cliente, login JWT, BCrypt, lectura de rol/estado actual en cada petición y respuestas 401/403. | Parcial |
| RF-02 | Consultar listados con filtros relevantes y comunicar resultados vacíos. | Administrador, host, mesero, cliente | — | Filtros y listas vacías en catálogos, reservas, pedidos y feedback; faltan estados visuales web/móvil. | Parcial; backend completo |
| RF-03 | Validar obligatorios en cliente y servidor, con backend como autoridad. | Todos | RN-01 a RN-08 | Bean Validation, reglas en servicios y errores uniformes; faltan clientes web/móvil. | Parcial; backend completo |
| RF-04 | Registrar fecha y usuario en cambios de estado de procesos de negocio. | Administrador, host, mesero | RN-03, RN-08 | Historial de reservas y pedidos con actor/fecha, cambios de estado, ítems y responsables. | Completo en backend |
| RF-05 | Implementar gestión de clientes. | Administrador, host, cliente | — | Alta, consulta, filtros, actualización, baja lógica/reactivación, perfil propio y vínculo opcional con usuario. CRUD administrativo web conectado (guía 06); experiencia propia web/móvil pendiente. | Parcial; backend y CRUD web administrativo implementados |
| RF-06 | Implementar gestión de zonas y mesas. | Administrador, host, mesero | RN-01, RN-02 | DTO, filtros, consulta, creación, actualización, capacidad y estados operativos `DISPONIBLE/OCUPADA`; falta experiencia web/móvil. | Parcial |
| RF-07 | Implementar gestión de turnos. | Administrador, host, mesero | RN-01 | DTO, filtros, horario incluso nocturno, capacidad, creación y actualización protegida. | Parcial |
| RF-08 | Implementar gestión de reservas. | Administrador, host, mesero, cliente | RN-01, RN-02, RN-03, RN-08 | Backend completo; web administrativa con alta, agenda, filtro por cliente, detalle/historial, corrección de cliente/observaciones sólo SOLICITADA y cancelación. Resto de UI pendiente. | Parcial |
| RF-09 | Implementar check-in. | Administrador, host | RN-03, RN-08 | Backend pasa únicamente `CONFIRMADA` a `SENTADA`, valida la mesa y registra asignación/actor; falta UI. | Parcial |
| RF-10 | Implementar carta básica. | Administrador, cliente, mesero | — | V6, DTO y API de productos; administración de nombre/precio/disponibilidad y consulta de carta disponible por roles autenticados. Pruebas de éxito/error y permisos; faltan pantallas. | Parcial; backend completo |
| RF-11 | Implementar pedidos. | Administrador, mesero | RN-04, RN-05, RN-06 | V7, pedido único de reserva sentada o mesa abierta, ítems y total decimal; permisos HTTP y concurrencia. | Parcial; backend completo |
| RF-12 | Implementar estados de atención. | Administrador, mesero | RN-04, RN-06 | ABIERTO → EN_PREPARACION → SERVIDO → CERRADO, cancelación autorizada, auditoría y cierre protegido de visita. | Parcial; backend completo |
| RF-13 | Implementar feedback. | Cliente, administrador | RN-07 | V8, opinión única propia después de FINALIZADA, consulta privada/administrativa y validación. | Parcial; backend completo |
| RF-14 | Implementar panel y reportes. | Administrador, host | — | API de ocupación y no-show con fechas, definición de métricas y pruebas conocidas; panel pendiente. | Parcial; backend completo |
| RF-15 | Permitir reservar sólo si existe capacidad. | Cliente, host | RN-01 | Se rechaza superar capacidad de mesa o suma del turno; existen pruebas positivas y negativas. | Completo |
| RF-16 | Evitar solapamientos. | Cliente, host | RN-02 | Se rechazan intervalos coincidentes en la misma mesa; cancelación libera el intervalo. | Completo |
| RF-17 | Realizar check-in y asignación. | Host | RN-03, RN-08 | Backend asigna/reasigna mesa activa, suficiente, operativamente disponible y sin solapamiento, con historial; falta UI. | Parcial |
| RF-18 | Crear pedido con totales correctos. | Mesero | RN-04, RN-05, RN-06 | Ítems con precio/nombre histórico y suma exacta; cambios de catálogo no alteran totales. | Completo en backend |
| RF-19 | Actualizar estado desde móvil de mesero. | Mesero | RN-04 | API de pedidos por responsable y transiciones protegidas lista; aplicación React Native pendiente. | Parcial |
| RF-20 | Generar indicadores de ocupación y no-show. | Administrador | — | Indicadores persistidos probados; panel visual pendiente. | Parcial; backend completo |

## Requisitos no funcionales derivados del contrato

| ID | Requisito no funcional | Criterio de aceptación | Evidencia actual | Estado |
|---|---|---|---|---|
| RNF-01 | Stack obligatorio. | Java 21, Spring Boot, PostgreSQL, React + TS, React Native + TS, Docker y GitHub Actions sin sustituciones. | Backend Java/Spring/PostgreSQL; faltan web, móvil, Docker y CI. | Parcial |
| RNF-02 | Persistencia reproducible. | Esquema creado mediante migraciones y validado sin `ddl-auto=update`. | Flyway V1–V9, Hibernate validate, migración desde cero y actualizaciones V5→V9 y V8→V9 conservando datos. | Completo en backend |
| RNF-03 | Seguridad. | Contraseñas no reversibles, secretos externos, mínimo privilegio y 401/403 uniformes. | BCrypt, JWT HS256, variables de entorno y permisos probados. | Completo, backend actual |
| RNF-04 | Contratos REST desacoplados. | Ningún controlador expone entidades JPA; usar DTO y rutas específicas. | DTO y servicios para módulos implementados. | Completo, backend actual |
| RNF-05 | Integridad y concurrencia. | La autoridad final es el backend; operaciones críticas resisten solicitudes concurrentes. | Bloqueos, versión de reserva, restricciones y pruebas de dos actores concurrentes en H2/PostgreSQL. | Verificado en escenarios cubiertos |
| RNF-06 | Errores consistentes. | Respuestas JSON diferenciadas para validación, autenticación, permiso, inexistencia, conflicto y regla de negocio. | Códigos 400, 401, 403, 404, 409 y 422 implementados. | Completo, backend actual |
| RNF-07 | Calidad verificable. | Pruebas de éxito y error para operaciones críticas, ejecutables sin base local. | Suite de servicios, HTTP, JWT, CORS, migraciones y concurrencia; resultados en PA-03-status.md. | Completo en alcance backend principal |
| RNF-08 | Arranque configurable. | README reproducible y credenciales fuera del repositorio. | README y `.env.example`; arranque real verificado. | Completo, backend actual |
| RNF-09 | Experiencia web usable. | Carga, error, vacío, confirmaciones destructivas, validación y navegación consistente. | Proyecto React separado existente; experiencias finales no implementadas/verificadas aquí. | Parcial |
| RNF-10 | Experiencia móvil diferenciada. | Navegación móvil y al menos un flujo transaccional contextual, no copia reducida del sitio. | Aplicación móvil ausente. | Ausente |
| RNF-11 | Contenedores. | Backend y PostgreSQL arrancan de forma reproducible con Docker Compose. | Aplazado a petición del usuario; sin cambios Docker en este cierre. | Pendiente |
| RNF-12 | Integración continua. | GitHub Actions compila y ejecuta pruebas en cada cambio relevante. | Aplazado a petición del usuario; no se creó workflow ni se publicó el incremento. | Pendiente |
| RNF-13 | IA no bloqueante. | Puerto desacoplado, timeout, registro de error y fallback; la operación esencial funciona sin proveedor. | Sin proveedor ni llamadas externas; feedback funciona sin IA; integración aplazada. | Pendiente |

## Trazabilidad con artefactos

| Artefacto | Requisitos que respalda |
|---|---|
| `README.md` y `.env.example` | RNF-01, RNF-02, RNF-03, RNF-08 |
| Migraciones Flyway V1–V9 | RF-04 a RF-18, RNF-02, RNF-05 |
| `SecurityConfig` y servicios JWT | RF-01, RNF-03 |
| Servicios/controladores de clientes | RF-02, RF-03, RF-05 |
| Servicios/controladores de zonas, mesas y turnos | RF-02, RF-03, RF-06, RF-07, RF-15 |
| Servicios/controladores de disponibilidad y reservas | RF-02, RF-03, RF-04, RF-08, RF-09, RF-15 a RF-17 |
| ProductoMenu, carta, migración V6 y sus pruebas de servicio/permisos | RF-02, RF-03, RF-10, HU-08, HU-16 |
| Pedido, MesaAbierta, ItemPedido, Feedback, reportes y consulta de notificaciones | RF-11 a RF-14, RF-18 a RF-20, RN-04 a RN-07, HU-19 |
| Pruebas de integración y autorización | RF-01 a RF-20 (componente backend), RNF-03 a RNF-07 |
| Archivos `.http` | RF-01 a RF-20 (componente backend), RNF-06, RNF-08 |
