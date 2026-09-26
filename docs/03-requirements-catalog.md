# GastroReserva — catálogo trazable de requisitos

## Convenciones

- **Completo:** implementado y verificado en el alcance indicado.
- **Parcial:** existe implementación o documentación, pero falta al menos una experiencia, proceso o evidencia requerida.
- **Ausente:** todavía no implementado.
- **Bloqueado:** no puede verificarse en el entorno actual.

La matriz operativa detallada se mantiene en [PA-03-status.md](PA-03-status.md). Este catálogo define el contrato; la matriz de estado registra su avance.

## Requisitos funcionales

| ID | Requisito | Actores | Reglas relacionadas | Evidencia o criterio verificable | Estado |
|---|---|---|---|---|---|
| RF-01 | Autenticar usuarios y resolver permisos según el rol vigente. | Todos | — | Registro cliente, login JWT, BCrypt, lectura de rol/estado actual en cada petición y respuestas 401/403. | Parcial |
| RF-02 | Consultar listados con filtros relevantes y comunicar resultados vacíos. | Administrador, host, mesero, cliente | — | Filtros en usuarios, clientes, zonas, mesas, turnos y reservas; faltan estados vacíos web/móvil. | Parcial |
| RF-03 | Validar obligatorios en cliente y servidor, con backend como autoridad. | Todos | RN-01 a RN-08 | Bean Validation y errores REST uniformes; faltan clientes web/móvil y módulos restantes. | Parcial |
| RF-04 | Registrar fecha y usuario en cambios de estado de procesos de negocio. | Administrador, host, mesero | RN-03, RN-08 | `historial_reservas` registra transición, check-in o reasignación, motivo, usuario y fecha; faltan pedidos. | Parcial |
| RF-05 | Implementar gestión de clientes. | Administrador, host, cliente | — | Alta, consulta, filtros, actualización, perfil propio y vínculo opcional con usuario. | Parcial |
| RF-06 | Implementar gestión de zonas y mesas. | Administrador, host, mesero | RN-01, RN-02 | DTO, filtros, consulta, creación, actualización, capacidad y estados operativos `DISPONIBLE/OCUPADA`; falta experiencia web/móvil. | Parcial |
| RF-07 | Implementar gestión de turnos. | Administrador, host, mesero | RN-01 | DTO, filtros, horario incluso nocturno, capacidad, creación y actualización protegida. | Parcial |
| RF-08 | Implementar gestión de reservas. | Administrador, host, mesero, cliente | RN-01, RN-02, RN-03, RN-08 | Disponibilidad, alta propia/operativa, agenda, cancelación, estados, check-in, reasignación e historial; falta UI. | Parcial |
| RF-09 | Implementar check-in. | Administrador, host | RN-03, RN-08 | Backend pasa únicamente `CONFIRMADA` a `SENTADA`, valida la mesa y registra asignación/actor; falta UI. | Parcial |
| RF-10 | Implementar carta básica. | Administrador, cliente, mesero | — | CRUD/estado disponible de `ProductoMenu` y consulta según actor. | Ausente |
| RF-11 | Implementar pedidos. | Administrador, mesero | RN-04, RN-05, RN-06 | Alta de pedido e ítems para visita habilitada, con total operativo. | Ausente |
| RF-12 | Implementar estados de atención. | Administrador, mesero | RN-04, RN-06 | Transiciones válidas y auditadas del pedido/atención. | Ausente |
| RF-13 | Implementar feedback. | Cliente, administrador | RN-07 | Alta única/autorizada posterior a visita finalizada y consulta administrativa. | Ausente |
| RF-14 | Implementar panel y reportes. | Administrador, host | — | Indicadores agregados de ocupación y no-show con filtros temporales. | Ausente |
| RF-15 | Permitir reservar sólo si existe capacidad. | Cliente, host | RN-01 | Se rechaza superar capacidad de mesa o suma del turno; existen pruebas positivas y negativas. | Completo |
| RF-16 | Evitar solapamientos. | Cliente, host | RN-02 | Se rechazan intervalos coincidentes en la misma mesa; cancelación libera el intervalo. | Completo |
| RF-17 | Realizar check-in y asignación. | Host | RN-03, RN-08 | Backend asigna/reasigna mesa activa, suficiente, operativamente disponible y sin solapamiento, con historial; falta UI. | Parcial |
| RF-18 | Crear pedido con totales correctos. | Mesero | RN-04, RN-05, RN-06 | Total igual a suma de cantidad por precio histórico, sin alcance fiscal. | Ausente |
| RF-19 | Actualizar estado desde móvil de mesero. | Mesero | RN-04 | Flujo móvil conectado al mismo backend con éxito/error. | Ausente |
| RF-20 | Generar indicadores de ocupación y no-show. | Administrador | — | Consultas agregadas reproducibles y panel web. | Ausente |

## Requisitos no funcionales derivados del contrato

| ID | Requisito no funcional | Criterio de aceptación | Evidencia actual | Estado |
|---|---|---|---|---|
| RNF-01 | Stack obligatorio. | Java 21, Spring Boot, PostgreSQL, React + TS, React Native + TS, Docker y GitHub Actions sin sustituciones. | Backend Java/Spring/PostgreSQL; faltan web, móvil, Docker y CI. | Parcial |
| RNF-02 | Persistencia reproducible. | Esquema creado mediante migraciones y validado sin `ddl-auto=update`. | Flyway V1–V5 y Hibernate `validate`. | Parcial |
| RNF-03 | Seguridad. | Contraseñas no reversibles, secretos externos, mínimo privilegio y 401/403 uniformes. | BCrypt, JWT HS256, variables de entorno y permisos probados. | Completo, backend actual |
| RNF-04 | Contratos REST desacoplados. | Ningún controlador expone entidades JPA; usar DTO y rutas específicas. | DTO y servicios para módulos implementados. | Completo, backend actual |
| RNF-05 | Integridad y concurrencia. | La autoridad final es el backend; operaciones críticas resisten solicitudes concurrentes. | Validaciones, restricciones, versión optimista y bloqueos de turno/mesa al reservar. | Parcial |
| RNF-06 | Errores consistentes. | Respuestas JSON diferenciadas para validación, autenticación, permiso, inexistencia, conflicto y regla de negocio. | Códigos 400, 401, 403, 404, 409 y 422 implementados. | Completo, backend actual |
| RNF-07 | Calidad verificable. | Pruebas de éxito y error para operaciones críticas, ejecutables sin base local. | H2 efímera en modo PostgreSQL; 39 pruebas sin fallos. | Parcial |
| RNF-08 | Arranque configurable. | README reproducible y credenciales fuera del repositorio. | README y `.env.example`; arranque real verificado. | Completo, backend actual |
| RNF-09 | Experiencia web usable. | Carga, error, vacío, confirmaciones destructivas, validación y navegación consistente. | Aplicación web ausente. | Ausente |
| RNF-10 | Experiencia móvil diferenciada. | Navegación móvil y al menos un flujo transaccional contextual, no copia reducida del sitio. | Aplicación móvil ausente. | Ausente |
| RNF-11 | Contenedores. | Backend y PostgreSQL arrancan de forma reproducible con Docker Compose. | Docker/Compose ausente; ejecución local no disponible. | Bloqueado |
| RNF-12 | Integración continua. | GitHub Actions compila y ejecuta pruebas en cada cambio relevante. | Repositorio Git y workflow ausentes. | Ausente |
| RNF-13 | IA no bloqueante. | Puerto desacoplado, timeout, registro de error y fallback; la operación esencial funciona sin proveedor. | Integración de Spring AI ausente. | Ausente |

## Trazabilidad con artefactos

| Artefacto | Requisitos que respalda |
|---|---|
| `README.md` y `.env.example` | RNF-01, RNF-02, RNF-03, RNF-08 |
| Migraciones Flyway V1–V5 | RF-04 a RF-09, RF-15 a RF-17, RNF-02, RNF-05 |
| `SecurityConfig` y servicios JWT | RF-01, RNF-03 |
| Servicios/controladores de clientes | RF-02, RF-03, RF-05 |
| Servicios/controladores de zonas, mesas y turnos | RF-02, RF-03, RF-06, RF-07, RF-15 |
| Servicios/controladores de disponibilidad y reservas | RF-02, RF-03, RF-04, RF-08, RF-09, RF-15 a RF-17 |
| Pruebas de integración y autorización | RF-01 a RF-09, RF-15 a RF-17, RNF-03 a RNF-07 |
| Archivos `.http` | RF-01 a RF-09, RF-15 a RF-17, RNF-06, RNF-08 |
