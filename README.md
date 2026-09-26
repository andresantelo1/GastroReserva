# GastroReserva (PA-03)

Backend incremental del proyecto integrador **GastroReserva**. El contrato completo exige Java 21, Spring Boot, PostgreSQL, React + TypeScript, React Native + TypeScript, Docker y GitHub Actions. Los entregables académicos están indexados en [`docs/README.md`](docs/README.md) y el estado de cobertura se mantiene en [`docs/PA-03-status.md`](docs/PA-03-status.md).

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

Flyway administra el esquema y Hibernate lo valida; la aplicación ya no modifica tablas con `ddl-auto=update`. En una base nueva, las migraciones V1 a V5 crean usuarios, clientes, zonas, mesas, turnos, reservas y un historial que también conserva check-in y reasignaciones de mesa. Para conservar instalaciones locales creadas previamente por Hibernate, una base no vacía sin historial se registra como línea base en la versión 1 y luego ejecuta las migraciones posteriores antes de que Hibernate valide las entidades.

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

## API actual

Los controladores usan DTO y servicios; las entidades JPA no forman parte del contrato HTTP.

| Método y ruta | Propósito |
|---|---|
| `GET /api/health` | Comprobar que el backend responde. |
| `POST /api/auth/register` | Registrar una cuenta `CLIENTE` y crear/vincular su perfil de negocio. |
| `POST /api/auth/login` | Autenticar y emitir un JWT Bearer. |
| `GET/POST /api/usuarios` | Listar/crear usuarios; sólo administrador. |
| `GET/PUT /api/usuarios/{id}` | Consultar/actualizar usuario; sólo administrador. |
| `PATCH /api/usuarios/{id}/password` | Reemplazar contraseña; sólo administrador. |
| `GET /api/clientes?activo=true&nombre=ana&email=example.com` | Listar y filtrar clientes; administrador o host. |
| `GET/POST/PUT /api/clientes/{id}` | Consultar, crear o actualizar clientes; administrador o host. |
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
| `POST /api/reservas` | Crear una reserva para el cliente autenticado. |
| `GET /api/reservas/mias` | Consultar las reservas del cliente autenticado. |
| `PATCH /api/reservas/{id}/cancelar` | Cancelar una reserva propia solicitada o confirmada. |
| `GET /api/reservas?...` | Consultar la agenda con filtros; administrador, host o mesero. |
| `POST /api/reservas/operativas` | Registrar una reserva para un cliente; administrador o host. |
| `PATCH /api/reservas/{id}/estado` | Aplicar una transición de estado válida; administrador, host o mesero. |
| `PATCH /api/reservas/{id}/check-in` | Pasar una reserva confirmada a sentada, conservando o cambiando la mesa; administrador o host. |
| `PATCH /api/reservas/{id}/mesa` | Reasignar una reserva confirmada o sentada con motivo y trazabilidad; administrador o host. |
| `GET /api/reservas/{id}/historial` | Consultar quién realizó cada cambio de estado y cuándo. |

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

Los ejemplos completos están en `auth.http`, `usuarios.http`, `clientes.http`, `zonas.http`, `mesas.http`, `turnos.http` y `reservas.http`.

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
| Cambiar estado de reserva | Sí | Sí | Sí | Sólo cancelar la propia |
| Check-in y reasignar mesa | Sí | Sí | No | No |

Para probar desde IntelliJ:

1. Ejecute el login de `auth.http`; su script guarda `auth_token` como variable global del cliente HTTP.
2. Ejecute solicitudes de `usuarios.http`, `clientes.http`, `zonas.http`, `mesas.http`, `turnos.http` o `reservas.http`, que envían `Authorization: Bearer {{auth_token}}`.
3. Para probar el autoservicio, registre el cliente con `auth.http` y ejecute primero el login de `clientes.http`; éste guarda `client_token` para las rutas `/api/clientes/me`.

Una respuesta `401` significa que falta un token válido. Una respuesta `403` significa que el usuario sí está autenticado, pero su rol no permite la operación.
