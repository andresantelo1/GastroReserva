# GastroReserva (PA-03)

## Para descargar y probar en grupo

Este repositorio reúne **backend + frontend**: Spring Boot permanece en la raíz y React está en [`frontend/`](frontend/README.md). Empezar por **[INICIO-EQUIPO.md](INICIO-EQUIPO.md)** para instalar dependencias, configurar la base propia e ingresar. El código no incluye los datos ni las contraseñas de la computadora del autor. Descargar el repositorio no publica la aplicación en Internet.

La carpeta `GastroReserva/` contiene documentación inicial del curso, no otro backend. Las rutas personales citadas en evidencias históricas describen dónde se verificó el proyecto; cada compañero debe usar su propia carpeta de descarga.

Backend incremental del proyecto integrador **GastroReserva**. El contrato completo exige Java 21, Spring Boot, PostgreSQL, React + TypeScript, React Native + TypeScript, Docker y GitHub Actions. Los entregables académicos están indexados en [`docs/README.md`](docs/README.md) y el estado de cobertura se mantiene en [`docs/PA-03-status.md`](docs/PA-03-status.md).

El backend principal incluye el recorrido completo hasta pedidos, finalización y feedback. **Web/móvil, Docker, GitHub Actions e IA no se consideran terminados**. Docker, CI e IA quedan aplazados por decisión del usuario. Para integrar el frontend y probar el cierre, ver [`docs/09-backend-handoff.md`](docs/09-backend-handoff.md).

## Requisitos locales

- JDK 21.
- PostgreSQL accesible (por defecto en `localhost:5432`).
- Una base de datos llamada `gastroreserva`.

En este equipo IntelliJ ya tiene configurado Temurin 21 en `C:\Users\antel\.jdks\temurin-21.0.12`, aunque el `java` global sigue apuntando a Java 8. Para una sesión de PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Users\antel\.jdks\temurin-21.0.12'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Compruebe la versión activa:

```powershell
java -version
.\mvnw.cmd -version
```

## Configuración

La aplicación lee estas variables de entorno:

| Variable | Obligatoria | Valor por defecto |
|---|---:|---|
| `DB_PASSWORD` | Sí | — |
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/gastroreserva` |
| `DB_USERNAME` | No | `postgres` |
| `SERVER_PORT` | No | `8080` |
| `JPA_SHOW_SQL` | No | `false` |
| `JWT_SECRET` | Sí | —; mínimo 32 caracteres aleatorios |
| `JWT_EXPIRATION_SECONDS` | No | `3600` |
| `RESTAURANT_TIME_ZONE` | No | `America/La_Paz`; horarios operativos locales |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173,http://127.0.0.1:5173`; lista separada por comas |
| `ADMIN_NAME` | No | `Administrador` |
| `ADMIN_EMAIL` | Sólo para crear el primer administrador | Vacío |
| `ADMIN_PASSWORD` | Sólo para crear el primer administrador | Vacío; entre 8 y 72 caracteres |

Use `.env.example` sólo como referencia; Spring Boot no carga archivos `.env` por sí mismo. En PowerShell:

```powershell
$env:DB_PASSWORD = 'su_clave_local'
$env:JWT_SECRET = 'una-clave-aleatoria-de-al-menos-32-caracteres'
$env:ADMIN_EMAIL = 'admin@gastroreserva.local'
$env:ADMIN_PASSWORD = 'una-contraseña-administrativa-segura'
```

No confirme credenciales reales en el repositorio.

`ADMIN_EMAIL` y `ADMIN_PASSWORD` se usan juntos para crear el primer administrador si ese correo todavía no existe. Después del primer arranque correcto puede retirarlos de la configuración; `DB_PASSWORD` y `JWT_SECRET` sí deben permanecer disponibles en cada inicio.

## Base de datos y arranque

Flyway administra el esquema y Hibernate lo valida; la aplicación ya no modifica tablas con `ddl-auto=update`. Las migraciones V1 a V9 crean usuarios, clientes, zonas, mesas, turnos, reservas, carta, mesas abiertas, pedidos, ítems, historiales y feedback. V9 amplía el historial para auditar correcciones de cliente/observaciones de reservas solicitadas. No se reescriben migraciones aplicadas; la actualización V8→V9 conserva reservas y eventos anteriores. Para instalaciones heredadas de Hibernate, una base no vacía sin historial usa baseline en V1; esto presupone que su esquema V1 ya es compatible. No se debe usar baseline para saltar errores de una base desconocida. Respaldar datos importantes antes de actualizar y reiniciar Spring Boot para aplicar las migraciones pendientes.

```powershell
.\mvnw.cmd spring-boot:run
```

Comprobación de salud:

```text
GET http://localhost:8080/api/health
```

## Pruebas

Las pruebas usan una base H2 efímera en modo PostgreSQL y ejecutan la migración desde cero; no requieren la contraseña ni alteran la base local.

```powershell
.\mvnw.cmd test
```

La verificación de entrega es `.\mvnw.cmd verify` con JDK 21. También se incluye una verificación **con PostgreSQL real temporal**, sin Docker y sin modificar el servicio ni la base habitual:

```powershell
.\scripts\verify-postgres.ps1
```

El script permite indicar `-PostgresBin`, `-JavaHome`, `-MavenRepository` y `-Offline`. Crea una instancia aislada sólo en loopback con puerto temporal, ejecuta las pruebas y la apaga en `finally`; conserva los datos/logs de prueba en la ruta temporal que informa. Nunca use el perfil `postgres-test` contra su base real: algunas pruebas limpian sus tablas de prueba.

## API actual

Los controladores usan DTO y servicios; las entidades JPA no forman parte del contrato HTTP.

| Método y ruta | Propósito |
|---|---|
| `GET /api/health` | Comprobar que el backend responde. |
| `POST /api/auth/register` | Registrar una cuenta `CLIENTE` y crear/vincular su perfil de negocio. |
| `POST /api/auth/login` | Autenticar y emitir un JWT Bearer. |
| `GET /api/auth/me` | Recuperar identidad y rol vigentes, sin contraseña. |
| `GET/POST /api/usuarios` | Listar/crear usuarios; sólo administrador. |
| `GET/PUT /api/usuarios/{id}` | Consultar/actualizar usuario; sólo administrador. |
| `PATCH /api/usuarios/{id}/password` | Reemplazar contraseña; sólo administrador. |
| `GET /api/clientes?activo=true&nombre=ana&email=example.com` | Listar y filtrar clientes; administrador o host. |
| `POST /api/clientes` | Crear un cliente; administrador o host. |
| `GET/PUT /api/clientes/{id}` | Consultar o actualizar clientes; administrador o host. |
| `DELETE /api/clientes/{id}` | Baja lógica de cliente; administrador o host. 204 sin body, conserva reservas/historial/cuenta. GET sigue disponible; PUT con activo=true reactiva. |
| `GET/POST/PUT /api/clientes/me` | Consultar, asegurar o actualizar el perfil propio; sólo cliente. |
| `GET /api/zonas?activa=true&nombre=terra` | Listar y filtrar zonas. |
| `GET /api/zonas/{id}` | Consultar una zona. |
| `POST /api/zonas` | Crear una zona activa. |
| `PUT /api/zonas/{id}` | Actualizar una zona. |
| `GET /api/mesas?zonaId=1&activa=true&capacidadMinima=4&estado=DISPONIBLE` | Listar y filtrar mesas. |
| `GET /api/mesas/{id}` | Consultar una mesa. |
| `POST /api/mesas` | Crear una mesa disponible mediante `zonaId`. |
| `PUT /api/mesas/{id}` | Actualizar datos administrativos de una mesa. |
| `GET /api/turnos?activo=true&nombre=almuerzo` | Listar y filtrar turnos; administrador, host o mesero. |
| `GET /api/turnos/{id}` | Consultar un turno. |
| `POST /api/turnos` | Crear un turno; sólo administrador. |
| `PUT /api/turnos/{id}` | Actualizar horario, capacidad y estado; sólo administrador. |
| `GET /api/disponibilidad?fecha=2026-12-20&turnoId=1&cantidadPersonas=4&zonaId=1` | Consultar capacidad de turno y mesas libres; cualquier usuario autenticado. |
| `GET /api/disponibilidad/turnos` y `/zonas` | Opciones activas para el formulario de reservas; cualquier usuario autenticado. |
| `POST /api/reservas` | Crear una reserva para el cliente autenticado. |
| `GET /api/reservas/mias` | Consultar las reservas del cliente autenticado. |
| `PATCH /api/reservas/{id}/cancelar` | Cancelar una reserva propia solicitada o confirmada. |
| `GET /api/reservas?...` | Consultar la agenda con filtros; administrador, host o mesero. |
| `GET /api/reservas/{id}` | Consultar detalle vigente; administrador, host o mesero. Incluye `version`. |
| `POST /api/reservas/operativas` | Registrar una reserva para un cliente; administrador o host. |
| `PUT /api/reservas/{id}/datos-cliente` | Corregir cliente/observaciones sólo en SOLICITADA; administrador o host. Exige cliente activo, motivo y versión vigente; audita antes/después. No reprograma. |
| `PATCH /api/reservas/{id}/estado` | Aplicar una transición de estado válida; administrador, host o mesero. |
| `PATCH /api/reservas/{id}/check-in` | Pasar una reserva confirmada a sentada, conservando o cambiando la mesa; administrador o host. |
| `PATCH /api/reservas/{id}/mesa` | Reasignar una reserva confirmada o sentada con motivo y trazabilidad; administrador o host. |
| `GET /api/reservas/{id}/historial` | Consultar quién realizó cada cambio de estado y cuándo. |
| `GET /api/carta?nombre=hamburguesa` | Consultar sólo productos disponibles; cualquier rol autenticado. |
| `GET /api/carta/{id}` | Consultar un producto disponible; `404` si no existe o no está disponible. |
| `GET /api/productos-menu?disponible=false&nombre=hamburguesa` | Catálogo administrativo, incluidos los no disponibles; sólo administrador. |
| `GET /api/productos-menu/{id}` | Consultar un producto del catálogo; sólo administrador. |
| `POST /api/productos-menu` | Crear un producto disponible; sólo administrador. |
| `PUT /api/productos-menu/{id}` | Actualizar nombre, descripción, precio y disponibilidad; sólo administrador. |
| `GET/POST /api/mesas-abiertas` | Consultar/abrir atención sin reserva; administrador, host o mesero. |
| `GET /api/mesas-abiertas/{id}` | Consultar una atención presencial. |
| `PATCH /api/mesas-abiertas/{id}/finalizar` | Cerrar atención y liberar mesa si no tiene pedido pendiente. |
| `GET /api/pedidos`, `/mios`, `/{id}`, `/{id}/historial` | Consultar pedidos y auditoría; administrador, host o mesero. |
| `POST /api/pedidos` | Crear pedido para reserva sentada o mesa abierta; administrador o mesero. |
| `POST /api/pedidos/{id}/items` | Agregar producto, cantidad y precio histórico. |
| `PUT/DELETE /api/pedidos/{id}/items/{itemId}` | Cambiar cantidad/retirar ítem mientras está abierto. |
| `PATCH /api/pedidos/{id}/estado` | Transición permitida; administrador o mesero responsable. |
| `PATCH /api/pedidos/{id}/responsable` | Asignar a un mesero activo; sólo administrador. |
| `POST /api/feedback` | Valoración única de reserva propia finalizada; cliente. |
| `GET /api/feedback/mios` | Opiniones propias; cliente. |
| `GET /api/feedback` | Filtros por puntuación y fecha de comentario; administrador. |
| `GET /api/feedback/{id}` | Administrador o autor; otro cliente recibe 404. |
| `GET /api/reportes/ocupacion-actual` | Mesas activas/ocupadas y porcentaje; administrador o host. |
| `GET /api/reportes/reservas?desde=2030-01-01&hasta=2030-01-31` | Reservas por fecha/turno/estado y tasa de no-show; administrador o host. |
| `GET /api/notificaciones?despuesDeId=0&limite=50` | Consultar cambios de reservas propias por cursor; cliente. No es push. |

Los cuerpos inválidos producen `400`, los recursos inexistentes `404`, los nombres/correos/números duplicados `409` y las reglas de negocio incumplidas `422`. El formato uniforme incluye `code`, `message`, `path` y, cuando corresponde, `fieldErrors`.

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La solicitud contiene datos inválidos",
  "path": "/api/mesas",
  "fieldErrors": {
    "capacidad": "La capacidad debe ser mayor que cero"
  }
}
```

Los ejemplos están en `auth.http`, `usuarios.http`, `clientes.http`, `zonas.http`, `mesas.http`, `turnos.http`, `reservas.http`, [`carta.http`](carta.http), [`recorrido-backend.http`](recorrido-backend.http) y [`mesas-abiertas.http`](mesas-abiertas.http). Los dos últimos contienen los pasos de cierre; guardan ids automáticamente y no incluyen contraseñas reales.

### Carta básica (RF-10, HU-08 y HU-16)

La carta es un catálogo, no un pedido ni un pago. Un producto tiene `nombre` (obligatorio, hasta 120 caracteres), `descripcion` opcional (hasta 500), `precio` decimal positivo (hasta 10 enteros y 2 decimales) y `disponible`. Al crearlo queda disponible. Al actualizarlo mediante PUT se deben enviar nombre, precio y disponibilidad; omitir la descripción la deja vacía. No se redondean precios inválidos: se rechazan con `400`.

Sólo el administrador accede al catálogo completo `/api/productos-menu`. Cliente, mesero y host consultan `/api/carta`, que siempre aplica disponibilidad, incluso si se envía `disponible=false`. Los filtros sin coincidencias devuelven `200` con `[]`. Los nombres repetidos se rechazan con `409`, ignorando mayúsculas y espacios exteriores, incluso si el producto existente no está disponible. Una clave normalizada única en base de datos también protege frente a altas concurrentes; no se expone en el DTO.

No hay eliminación física de productos: para retirar uno se actualiza `disponible` a `false`. Los ítems de pedido copian nombre y precio al agregarse; modificar después el catálogo no altera el consumo registrado (RN-05). RN-04 a RN-06 se implementan en `PedidoService` y V7.

Para probar: reinicie el backend, ejecute los logins de administrador y cliente, y siga [`carta.http`](carta.http) en orden. La primera solicitud guarda automáticamente el id del producto. Los ejemplos modifican su base local sólo cuando usted los ejecuta.

Las reservas nacen en `SOLICITADA` y sólo aceptan las transiciones definidas por RN-03. El paso `CONFIRMADA → SENTADA` se realiza exclusivamente mediante check-in, que vuelve a bloquear la reserva y las mesas involucradas, valida actividad, capacidad y solapamiento, y marca la mesa como `OCUPADA`. Al finalizar la visita queda `DISPONIBLE`. Cada reasignación registra mesa anterior/nueva, motivo, actor y fecha. Los estados que ocupan capacidad del turno son `SOLICITADA`, `CONFIRMADA` y `SENTADA`; al cancelar, finalizar o marcar `NO_SHOW`, esa capacidad vuelve a quedar libre.

## Autenticación y permisos

Las contraseñas se almacenan con BCrypt y nunca se devuelven en la API. Los tokens JWT usan HS256, duran una hora por defecto y cada petición vuelve a comprobar en PostgreSQL el estado y rol vigentes del usuario.

| Operación | ADMINISTRADOR | HOST | MESERO | CLIENTE / anónimo |
|---|---:|---:|---:|---:|
| Salud, login y registro de cliente | Sí | Sí | Sí | Sí |
| Consultar zonas y mesas | Sí | Sí | Sí | No |
| Crear o actualizar zonas/mesas | Sí | No | No | No |
| Administrar usuarios | Sí | No | No | No |
| Consultar y administrar clientes | Sí | Sí | No | Sólo su propio perfil |
| Consultar turnos | Sí | Sí | Sí | No |
| Crear o actualizar turnos | Sí | No | No | No |
| Consultar disponibilidad | Sí | Sí | Sí | Sí, autenticado |
| Consultar agenda de reservas | Sí | Sí | Sí | Sólo sus reservas |
| Crear reserva | Para un cliente | Para un cliente | No | Para sí mismo |
| Corregir cliente/observaciones en SOLICITADA | Sí, con motivo y versión | Sí, con motivo y versión | No | No |
| Cambiar estado de reserva | Sí | Sí | Sí | Sólo cancelar la propia |
| Check-in y reasignar mesa | Sí | Sí | No | No |
| Consultar carta disponible | Sí | Sí | Sí | Sí, autenticado |
| Gestionar catálogo completo de productos | Sí | No | No | No |
| Consultar pedidos/historial | Sí | Sí | Sí | No |
| Crear pedidos | Sí | No | Sí | No |
| Modificar ítems/estados de pedido | Sí | No | Sólo responsable | No |
| Asignar responsable de pedido | Sí | No | No | No |
| Abrir/cerrar atención sin reserva | Sí | Sí | Sí | No |
| Crear feedback | No | No | No | Sólo reserva propia finalizada |
| Consultar feedback | Todos | No | No | Sólo propio |
| Consultar reportes | Sí | Sí | No | No |
| Consultar notificaciones propias | No | No | No | Sí, autenticado |

Para probar desde IntelliJ:

1. Copie `http-client.private.env.json.example` a `http-client.private.env.json`, complete allí sus credenciales locales y elija **Run with: local** en IntelliJ. Ese archivo privado está ignorado por Git. Ejecute el login de `auth.http`; su script guarda `auth_token` como variable global del cliente HTTP.
2. Ejecute solicitudes de `usuarios.http`, `clientes.http`, `zonas.http`, `mesas.http`, `turnos.http` o `reservas.http`, que envían `Authorization: Bearer {{auth_token}}`.
3. Para probar el autoservicio, registre el cliente con `auth.http` y ejecute primero el login de `clientes.http`; éste guarda `client_token` para las rutas `/api/clientes/me`.

Una respuesta `401` significa que falta un token válido. Una respuesta `403` significa que el usuario sí está autenticado, pero su rol no permite la operación.
