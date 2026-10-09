# Backend principal — cierre e integración frontend

Fecha: 2026-10-08. Alcance: RF-01 a RF-18 y soporte API de RF-19/RF-20, RN-01 a RN-08. No significa que esté terminado todo el proyecto académico: faltan las experiencias web/móvil y se aplazan Docker, GitHub Actions e IA.

## Cómo probarlo en IntelliJ

1. Detener y volver a iniciar Spring Boot con Java 21 y las variables que ya usa el proyecto. Flyway aplica únicamente las versiones que falten hasta V8. Para una base importante, hacer respaldo antes del cambio de versión.
2. Ejecutar el login administrador de `auth.http`: guarda `auth_token`.
3. Ejecutar el login cliente de `clientes.http`: guarda `client_token`. Usar una cuenta cliente registrada; no la del administrador.
4. Abrir [recorrido-backend.http](../recorrido-backend.http) y ejecutar sus solicitudes **de arriba hacia abajo**. Crea una zona, mesa, turno y producto de demostración; guarda cada id automáticamente. No cambia sus registros existentes.
5. El recorrido reserva, confirma, hace check-in, registra dos platos, verifica precio histórico, prepara/sirve/cierra el pedido, finaliza la visita, registra opinión y consulta historial/reportes.
6. Los pasos rotulados `Error esperado` deben devolver 422 o 403; son comprobaciones de protección, no errores del proyecto.
7. Opcional: [mesas-abiertas.http](../mesas-abiertas.http) muestra atención sin reserva, cancelación de pedido y liberación de mesa.

Las solicitudes manuales sí escriben datos en el backend seleccionado. No ejecutar la colección contra producción. Las cuentas y tokens no se incluyen en estos archivos; los archivos HTTP antiguos modificados por el usuario se preservaron y deben revisarse antes de publicar en GitHub. Para credenciales locales se puede usar `http-client.private.env.json`, ignorado por Git.

## Contratos principales para React y React Native

- URL local: `http://localhost:8080/api`; un teléfono físico necesita la IP accesible del equipo, no su propio localhost.
- JSON, `Authorization: Bearer <accessToken>`. No usa cookies. Un 401 requiere volver a iniciar sesión; 403 es falta de permiso.
- `GET /auth/me` recupera el usuario actual y su rol; nunca devuelve hash. No confiar sólo en ocultar botones: el backend revalida permisos.
- Para reservar: `GET /disponibilidad/turnos`, `/disponibilidad/zonas`, `/disponibilidad?fecha=AAAA-MM-DD&turnoId=...&cantidadPersonas=...`, después `POST /reservas`.
- CORS admite localhost:5173 y 127.0.0.1:5173. Si Vite usa otro puerto/origen, agregarlo a `CORS_ALLOWED_ORIGINS` y reiniciar; no habilitar `*` por comodidad.
- Horarios de reservas/aperturas: fecha/hora **local** del restaurante, zona `RESTAURANT_TIME_ZONE` (America/La_Paz por defecto). Marcas de auditoría: instantes ISO con zona/UTC.
- Errores: `{timestamp,status,code,message,path,fieldErrors}`. 400 datos inválidos, 401 sesión, 403 permiso, 404 inexistencia/propiedad, 409 duplicado o cambio concurrente, 422 regla de negocio. Mostrar `message` y errores por campo.

## Pedido y atención

`POST /pedidos` recibe **exactamente uno**: `{"reservaId":123}` o `{"mesaAbiertaId":456}`. La reserva debe estar SENTADA o la apertura seguir vigente. Existe un solo pedido por visita/apertura; un pedido cancelado no permite crear otro para ese mismo origen.

El creador queda como responsable. Administrador puede reasignar con `PATCH /pedidos/{id}/responsable` y `{"usuarioId":idMesero}`. Mesero sólo modifica el pedido que tiene asignado; host sólo consulta. El administrador puede operar cualquier pedido.

`POST /pedidos/{id}/items`: `{"productoId":1,"cantidad":2}`. `PUT /pedidos/{id}/items/{itemId}`: `{"cantidad":3}`. `DELETE` en la misma ruta retira el ítem y deja evento de auditoría. Cantidad 1–999, producto disponible, sólo estado ABIERTO. Cada alta copia nombre y precio y crea una línea nueva, aunque el producto se repita.

Estados permitidos:

```text
ABIERTO → EN_PREPARACION → SERVIDO → CERRADO
ABIERTO o EN_PREPARACION → CANCELADO (motivo obligatorio)
```

Se cambia con `PATCH /pedidos/{id}/estado` y `{"estado":"SERVIDO","motivo":"opcional"}`. Preparar exige al menos un ítem; CERRADO/CANCELADO son terminales. Cada cambio de estado, ítem o responsable conserva actor y fecha en `/historial`.

El total es suma de cantidad × precio histórico (BigDecimal). No hay pagos ni facturación. Una visita no puede finalizar si existe un pedido ABIERTO, EN_PREPARACION o SERVIDO. Cerrar el pedido y finalizar la visita son pasos separados; finalizar libera la mesa.

Un cambio de mesa de la reserva no pierde el pedido: la mesa se obtiene de su origen actual. El historial de reserva registra ambas mesas y el motivo (RN-08).

## Mesa abierta sin reserva

`POST /mesas-abiertas`: `{"mesaId":1,"cantidadPersonas":2,"finPrevisto":"2030-01-15T14:00:00"}`. El fin debe ser futuro respecto al reloj del restaurante. Sólo admite mesa/zona activa, capacidad suficiente y mesa disponible, sin reservas activas en ese intervalo. Reservas nuevas también respetan la apertura existente.

`finPrevisto` sirve para planificar disponibilidad, **no libera automáticamente la mesa**. El personal debe cerrar la atención en `/mesas-abiertas/{id}/finalizar`. Mientras siga abierta, el estado operativo impide otro check-in/apertura en esa mesa. No se ofrece reasignación de una apertura sin reserva en este MVP.

## Feedback e IA aplazada

`POST /feedback`: `{"reservaId":123,"puntuacion":5,"comentario":"Buena atención"}`. Sólo el cliente propietario de una reserva FINALIZADA, una valoración por reserva, puntuación 1–5 y comentario no vacío hasta 1000 caracteres. No hay edición/eliminación en el MVP. `/feedback/mios` es privado; el administrador filtra `/feedback?puntuacion=5&desde=...&hasta=...` por fecha del comentario.

La IA futura clasificaría el comentario por tema/sentimiento y produciría un resumen administrativo. Por ejemplo, “tardaron en servir, pero la comida fue buena” puede referirse a demora y comida con valoración mixta. **Actualmente no hay clasificación automática ni llamadas externas.** Guardar una opinión no requiere una clave de IA. La futura integración tendrá puerto, timeout y fallback; nunca decidirá disponibilidad ni permisos.

## Indicadores y notificaciones

- `/reportes/ocupacion-actual`: mesas activas de zonas activas, cuántas están OCUPADAS y porcentaje; única consulta para evitar mezclar dos instantes. Incluye atención con/sin reserva.
- `/reportes/reservas?desde=...&hasta=...`: fechas inclusivas (hasta 366 días), sólo reservas, agrupadas por fecha/turno/estado. Atendidas = SENTADA + FINALIZADA. Tasa no-show = NO_SHOW / (NO_SHOW + atendidas) × 100; canceladas y sin resolución no entran en el denominador. Sin denominador devuelve 0.00.
- Capacidad por turno = personas de reservas SOLICITADA/CONFIRMADA/SENTADA frente a la **capacidad actual** del turno; no es aforo histórico ni estadística de walk-ins. Una reserva no pasa automáticamente a NO_SHOW por reloj: el personal la marca explícitamente.
- `/notificaciones?despuesDeId=0&limite=50`: consulta de eventos propios, límite 1–100, respuesta `{eventos,ultimoId,hayMas}`. No expone notas internas. El cliente puede consultar periódicamente y actualizar también `/reservas/mias` como fuente del estado real. No hay push, email ni WhatsApp; la entrega visual en móvil sigue pendiente.

## CRUD administrativo de clientes — guía 06

ADMINISTRADOR/HOST pueden listar y crear en `/clientes`, consultar/editar en `/clientes/{id}` y realizar una baja lógica mediante `DELETE /clientes/{id}`. POST envía `{nombre,email,telefono}`; PUT exige además `activo` booleano. POST responde 201 con DTO; GET/PUT, 200 con DTO; DELETE, **204 sin body**.

DELETE cambia `activo=false` dentro de una transacción; no borra la fila, reservas, historial ni cuenta vinculada. Se puede repetir sobre un perfil inactivo existente y obtener 204. Un id inexistente da 404, un id no positivo 400. Un inactivo sigue disponible para GET administrativo y PUT con activo=true permite reactivarlo. El correo continúa siendo único. No se usa eliminación en cascada.

Los nuevos pedidos de reserva operativa requieren un perfil activo. Los endpoints de autoservicio que ya exigen perfil activo mantienen esa política; esta baja no es cancelación de reservas ni desactivación de credenciales. Actualizar nombre/email de un cliente con usuario vinculado sigue sincronizando esos campos de la cuenta, sin cambiar su contraseña.

Reiniciar el backend para cargar el nuevo endpoint. No hay migración ni cambio de permisos/CORS: activo y las reglas de acceso ya existían. La interfaz web implementa GET de detalle previo, un formulario de alta/edición, confirmación, manejo de 204, filtros por estado y reactivación. Conserva los datos escritos y no anuncia cambios si el servidor rechaza la operación.

## Relación Cliente 1:N Reserva — guía 07

ADMINISTRADOR/HOST pueden corregir el titular y las observaciones exclusivamente en SOLICITADA. Leer primero `GET /reservas/{id}` y conservar `version` (0 también es válida). Enviar `PUT /reservas/{id}/datos-cliente`:

```json
{"clienteId":2,"observaciones":"Nota corregida","motivo":"Corrección solicitada por recepción","version":0}
```

Respuesta 200: ReservaResponse completo con nueva versión y cliente anidado. Cliente debe existir y estar activo; motivo obligatorio hasta 500 caracteres; observaciones opcionales hasta 500. DTO inválido → 400; cliente/reserva inexistente → 404; versión vieja intentando otro cambio → 409; inactivo/estado no editable → 422; sin sesión → 401; mesero/cliente → 403. Repetir un resultado idéntico no duplica eventos aunque su versión sea anterior; no implica permitir otra modificación obsoleta.

ReservaClienteService bloquea reserva/cliente, conserva programación/creador y registra CORRECCION_CLIENTE: estado, cliente y observaciones antes/después, motivo, actor y fecha. El dueño anterior deja de verla en sus reservas; el nuevo pasa a ser el dueño. GET historial es para personal autorizado. V9 añade campos/FKs/checks del historial; se probó actualizar V8 conservando registros.

La web obtiene reservas/clientes en paralelo (ADMINISTRADOR/HOST), resuelve nombres con Map y filtra localmente. MESERO no llama a `/clientes` sin permiso: usa la proyección del cliente incluida en la agenda. Crear usa POST /reservas/operativas; editar usa el PUT anterior; cancelar usa PATCH /reservas/{id}/estado con `{estado:"CANCELADA",motivo:"..."}` y respuesta 200. No hay DELETE de reservas. CANCELADA se conserva como estado terminal; no es una baja reactivable como Cliente. La web exige confirmación y motivo para cancelar. No ofrece reprogramación, cambios genéricos de turno/mesa/personas ni check-in todavía.

Reiniciar el backend antes de usar esta web: el contrato de reservas ahora incluye `version`. Respaldar datos importantes antes de aplicar migraciones. La base habitual no fue alterada durante las pruebas.

## Verificación reproducible

`mvnw.cmd verify` con JDK 21 ejecuta H2 temporal. `scripts/verify-postgres.ps1` crea y apaga una instancia PostgreSQL temporal en un puerto separado; no usa el servicio local ni requiere credenciales reales. Su autenticación trust es sólo para esa instancia efímera de pruebas en loopback, nunca para el PostgreSQL habitual.

La suite actual tiene 129 pruebas aprobadas en PostgreSQL 17 temporal. Incluye éxito/error de negocio, JWT y permisos HTTP, CORS, datos ajenos, precios históricos, indicadores conocidos, migraciones desde cero y actualizaciones V5→V9 y V8→V9 conservando datos. Las pruebas concurrentes usan dos hilos con transacciones reales y un esquema separado para capacidad/solapamiento, pedido único, ítems simultáneos, apertura única, feedback único y crear pedido vs. finalizar visita. La corrección de titular añade pruebas de versión obsoleta, idempotencia, estados, roles y propiedad; no se afirma una nueva prueba de carrera a dos hilos específica de esa corrección.

No se promete ausencia absoluta de errores ni capacidad productiva bajo cualquier carga. La evidencia y el alcance de cierre se registran en [PA-03-status.md](PA-03-status.md). El siguiente incremento es conectar las pantallas del frontend a estos contratos.
