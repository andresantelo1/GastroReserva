# GastroReserva — backlog priorizado y criterios de aceptación

## Prioridades

- **P0:** imprescindible para demostrar el MVP y su flujo crítico.
- **P1:** importante para completar experiencias y supervisión.
- **P2:** complementario y no bloquea la operación esencial.

## Backlog funcional

| ID | Prioridad | Historia de usuario | RF/RN | Estado |
|---|:---:|---|---|---|
| HU-01 | P0 | Como usuario, quiero iniciar sesión para acceder únicamente a funciones permitidas por mi rol. | RF-01, RF-03 | Backend completo |
| HU-02 | P0 | Como cliente, quiero consultar disponibilidad por fecha, turno y personas para elegir una opción válida. | RF-02, RF-15, RF-16, RN-01, RN-02 | Backend completo |
| HU-03 | P0 | Como cliente, quiero crear una reserva para asegurar mi lugar. | RF-08, RF-15, RF-16, RN-01 a RN-03 | Backend completo |
| HU-04 | P0 | Como administrador, quiero gestionar zonas y mesas para representar el salón. | RF-06, RN-01 | Backend completo |
| HU-05 | P0 | Como administrador, quiero gestionar turnos y capacidad para controlar horarios. | RF-07, RN-01 | Backend completo |
| HU-06 | P0 | Como host, quiero gestionar clientes y reservas operativas para atender llamadas o llegadas. | RF-05, RF-08 | Backend completo |
| HU-07 | P0 | Como host, quiero realizar check-in y asignar/reasignar una mesa para iniciar la atención. | RF-09, RF-17, RN-01 a RN-03, RN-08 | Backend completo |
| HU-08 | P0 | Como administrador, quiero gestionar la carta básica y su disponibilidad. | RF-10 | Backend completo; UI pendiente |
| HU-09 | P0 | Como mesero, quiero crear un pedido para una reserva sentada o mesa abierta. | RF-11, RN-04 | Backend completo; UI pendiente |
| HU-10 | P0 | Como mesero, quiero agregar ítems y calcular el total conservando precios históricos. | RF-18, RN-05, RN-06 | Backend completo; UI pendiente |
| HU-11 | P0 | Como mesero, quiero actualizar estados de atención desde móvil. | RF-12, RF-19 | Backend completo; UI pendiente |
| HU-12 | P0 | Como mesero, quiero finalizar la visita para cerrar su atención. | RF-04, RN-03, RN-07 | Backend completo; UI pendiente |
| HU-13 | P1 | Como cliente, quiero consultar mis reservas para conocer fecha, horario y estado. | RF-02, RF-08 | Backend completo |
| HU-14 | P1 | Como cliente, quiero cancelar una reserva para liberar capacidad. | RF-08, RN-03 | Backend completo |
| HU-15 | P1 | Como administrador, quiero consultar el historial de una reserva para conocer su trazabilidad. | RF-04, RF-08, RN-08 | Backend completo; UI pendiente |
| HU-16 | P1 | Como cliente, quiero consultar la carta disponible. | RF-10 | Backend completo; UI pendiente |
| HU-17 | P1 | Como cliente, quiero registrar feedback después de finalizar mi visita. | RF-13, RN-07 | Backend completo; UI pendiente |
| HU-18 | P1 | Como administrador, quiero consultar ocupación y no-show para supervisar la operación. | RF-14, RF-20 | Backend completo; UI pendiente |
| HU-19 | P1 | Como cliente, quiero recibir notificaciones relevantes de mis reservas. | Experiencia móvil | API de consulta lista; experiencia móvil pendiente |
| HU-20 | P2 | Como administrador, quiero visualizar comentarios clasificados por tema y sentimiento. | RNF-13 | IA aplazada por el usuario |

## Criterios de aceptación por historia

### HU-01 — Autenticación y permisos

- Dado un usuario activo y contraseña correcta, cuando inicia sesión, entonces recibe un JWT y contexto sin hash.
- Dado un usuario inexistente, inactivo o contraseña incorrecta, entonces recibe el mismo error `401` sin revelar el motivo específico.
- Dado un usuario autenticado sin rol suficiente, entonces recibe `403` JSON.

### HU-02 — Consultar disponibilidad

- La respuesta muestra intervalo, capacidad reservada/disponible del turno y mesas activas suficientes.
- Una mesa con reserva activa solapada no aparece.
- Si el turno no tiene capacidad suficiente, la lista de mesas queda vacía.

### HU-03 — Crear reserva

- Una solicitud válida crea una reserva `SOLICITADA` y un evento de historial.
- Superar capacidad de mesa o turno produce `422`.
- Solapar una mesa produce `422`.
- Una solicitud concurrente no puede violar capacidad ni solapamiento.

### HU-04 — Gestionar zonas y mesas

- Sólo administrador puede crear/actualizar.
- Nombres de zona y números de mesa duplicados producen `409`.
- No se permite capacidad no positiva ni reducirla por debajo de una reserva activa.

### HU-05 — Gestionar turnos

- Sólo administrador puede crear/actualizar; host y mesero pueden consultar.
- Inicio y fin iguales producen `422`; un turno nocturno es válido.
- La capacidad no puede quedar por debajo de reservas activas de una fecha.

### HU-06 — Gestión operativa de clientes/reservas

- Administrador y host pueden crear, filtrar, actualizar y dar de baja lógicamente clientes. DELETE conserva id, reservas, historial y cuenta vinculada; PUT con activo=true permite reactivar. Las nuevas reservas operativas exigen cliente activo.
- Un cliente presencial puede vincularse posteriormente con una cuenta usando su correo único.
- La reserva operativa registra al actor que la creó.
- Guía 07: administrador/host puede corregir cliente activo y observaciones exclusivamente en SOLICITADA, con motivo y versión vigente. Se conserva id, creador, fecha, intervalo, turno, mesa y cantidad; un evento guarda cliente/observaciones anteriores y nuevos. Una versión obsoleta que intenta otro cambio da 409 sin sobrescribir. Repetir el mismo resultado no duplica eventos.
- Web administrativa: selector por nombre/id, filtro local Cliente 1:N, GET detalle/historial y confirmación de cancelación. La cancelación usa la transición existente de SOLICITADA/CONFIRMADA a CANCELADA, mantiene fila e historial y no se revierte. No hay DELETE físico de reservas ni reprogramación genérica.

### HU-07 — Check-in, asignación y reasignación

- Sólo una reserva `CONFIRMADA` puede pasar a `SENTADA` mediante check-in.
- La mesa debe estar activa, ser suficiente y no estar solapada.
- Toda reasignación registra mesa anterior/nueva, motivo, usuario y fecha.

### HU-08 y HU-16 — Carta

- Administrador puede crear/actualizar producto, precio y disponibilidad.
- Cliente y mesero sólo consultan productos disponibles.
- Precio inválido o nombre duplicado produce error controlado.

**Evidencia backend:** Flyway V6, `ProductoMenuService`, `ProductoMenuController`, `CartaController`, pruebas de servicio/permisos y `carta.http`. GET `/api/carta` sólo muestra disponibles; `/api/productos-menu` permite administración completa. La baja es lógica mediante disponibilidad. Las pantallas web/móvil siguen pendientes.

### HU-09 y HU-10 — Pedido, ítems y total

- El pedido sólo nace para reserva `SENTADA` o mesa abierta.
- Agregar ítem copia el precio actual como precio histórico.
- Cambiar el producto no altera ítems previos.
- El total es la suma de cantidad por precio histórico y no genera información fiscal.

### HU-11 y HU-12 — Atención y finalización

- Mesero autenticado puede aplicar únicamente transiciones permitidas.
- Cada transición registra actor y fecha.
- Finalizar la visita deja la reserva `FINALIZADA` y habilita feedback.

### HU-13 y HU-14 — Mis reservas y cancelación

- El cliente sólo consulta/cancela reservas propias.
- Sólo `SOLICITADA` o `CONFIRMADA` pueden cancelarse por el cliente.
- Cancelar libera capacidad e intervalo y genera historial.

### HU-15 — Historial

- Los eventos se devuelven cronológicamente.
- Cada evento informa estado anterior/nuevo, motivo, actor y fecha.
- Las reasignaciones informan mesa anterior y nueva.

### HU-17 — Feedback

- Sólo el propietario de una reserva `FINALIZADA` puede registrar feedback.
- Una reserva no finalizada o ajena produce error controlado.
- La ausencia del proveedor de IA no impide guardar el feedback.

### HU-18 — Indicadores

- El administrador puede filtrar por rango de fechas.
- Ocupación y no-show se calculan desde datos persistidos y tienen pruebas con resultados conocidos.
- Una consulta sin datos devuelve una respuesta válida y estado vacío comprensible.

### HU-19 — Notificaciones

- La aplicación móvil informa cambios relevantes sin impedir consultar directamente el estado real.
- Un fallo de notificación no revierte una reserva confirmada correctamente.

### HU-20 — Análisis con IA

- La clasificación muestra tema, sentimiento operativo y resumen administrativo.
- El proveedor tiene timeout y fallback.
- El contenido generado se identifica como probabilístico; las métricas deterministas no dependen de él.

## Backlog técnico del Parcial 1

| ID | Prioridad | Trabajo | Criterio de cierre | Estado |
|---|:---:|---|---|---|
| BT-01 | P0 | Consolidar documentación académica. | Visión, actores, RF/RNF, RN, casos, DER, arquitectura y backlog dentro de `docs`. | Completo |
| BT-02 | P0 | Inicializar Git y publicar GitHub. | Repositorio operativo, historial limpio y ramas documentadas. | Git local operativo; publicación remota no reverificada en este incremento |
| BT-03 | P0 | Dockerizar backend/PostgreSQL. | `docker compose up` inicia servicios y aplica migraciones. | Aplazado a petición del usuario |
| BT-04 | P0 | Entregar colección de API. | Colección Postman/Bruno/Insomnia ejecuta autenticación y tres casos de uso con errores. | Pendiente |
| BT-05 | P0 | Crear shell React + TypeScript. | Compila, autentica y presenta 1–2 pantallas conectadas con carga/error/vacío. | Proyecto separado existente; integración no verificada aquí |
| BT-06 | P0 | Crear shell React Native + TypeScript. | Compila, navega y consume o simula un contrato API. | Pendiente; Node/npm ya disponibles |
| BT-07 | P1 | Crear GitHub Actions. | Backend y futuros clientes se verifican en CI. | Aplazado a petición del usuario |

## Evidencia de cierre backend — 2026-10-08

- HU-09/HU-10: V7, PedidoService, ítems con precio histórico y total; DTO, permisos y casos de error.
- HU-11/HU-12: estados de atención, auditoría, responsable y bloqueo de finalización con pedido pendiente.
- HU-17: V8, FeedbackService, propiedad/estado y unicidad, incluida concurrencia.
- HU-18: ReporteService, ocupación de una sola lectura y no-show con denominador explícito.
- HU-19: consulta privada de eventos por cursor; no es push ni demuestra aún experiencia móvil.
- Pruebas HTTP con JWT real, CORS, errores uniformes y pruebas simultáneas en dos transacciones; H2 y PostgreSQL temporal.
- Recorrido ejecutable: [recorrido-backend.http](../recorrido-backend.http). Guía: [09-backend-handoff.md](09-backend-handoff.md).
- La definición de terminado de la historia completa sigue exigiendo web/móvil cuando corresponde; se cierra sólo su componente backend.

## Definición de terminado

Una historia sólo se considera terminada cuando:

1. El contrato y las reglas están identificados.
2. La migración/modelo existe cuando corresponde.
3. El backend aplica permisos y validación final.
4. Existen pruebas de éxito y error.
5. Web/móvil consumen el contrato cuando la historia requiere experiencia de usuario.
6. Los estados de carga, vacío, validación y error son visibles.
7. README, matrices y evidencia se actualizan.
8. No se exponen secretos ni entidades JPA.
