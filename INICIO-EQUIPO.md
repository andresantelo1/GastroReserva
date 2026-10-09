# GastroReserva — probar en la computadora de cada compañero

## 1. Descargar

En GitHub, abrir [andresantelo1/GastroReserva](https://github.com/andresantelo1/GastroReserva), elegir la rama `main` y **Code → Download ZIP**. Extraer el ZIP antes de abrirlo. Si ya usan Git:

```powershell
git clone https://github.com/andresantelo1/GastroReserva.git
cd GastroReserva
```

Backend en la raíz (`pom.xml`, `src/`, `mvnw.cmd`); frontend en `frontend/` (`package.json`, `src/`). `GastroReserva/` es documentación histórica, no la carpeta que se ejecuta.

## 2. Preparar los programas

- JDK **21** para Spring Boot. Seleccionarlo en IntelliJ y comprobar `java -version` si se usa terminal.
- PostgreSQL en ejecución; se verificó con PostgreSQL 17.
- Node.js/npm para React; se verificó con Node **26.10.0**, incluido el runner TypeScript de las pruebas Node. No se garantiza ese runner en versiones anteriores.
- IntelliJ y WebStorm son opcionales: también se puede usar terminal y otro editor.

No se necesitan Docker ni Maven global para este arranque: el proyecto incluye Maven Wrapper. El primer arranque requiere Internet para descargar dependencias.

## 3. Base propia y backend

En PostgreSQL/DataGrip crear una **base vacía** llamada `gastroreserva` en tu instancia local. Si aún no existe, ejecutar como un usuario que pueda crear bases:

```sql
CREATE DATABASE gastroreserva;
```

No crear tablas manualmente ni importar una base ajena. Al arrancar, Flyway aplica V1–V9 y Hibernate valida el esquema. Si ya tienen una base con datos, hacer respaldo antes de actualizar; no borrar ni forzar migraciones para ocultar errores.

Abrir la raíz en IntelliJ y configurar las variables del proceso Spring Boot en **Run → Edit Configurations → Environment variables**:

| Variable | Qué poner |
|---|---|
| DB_URL | `jdbc:postgresql://localhost:5432/gastroreserva` (ajustar si tu puerto/base es otro). |
| DB_USERNAME | Tu usuario PostgreSQL local. |
| DB_PASSWORD | La contraseña de ese usuario PostgreSQL. |
| JWT_SECRET | Un secreto aleatorio propio de al menos 32 caracteres; no compartir ni subir. |
| ADMIN_EMAIL | El correo del administrador de prueba que quieras crear. |
| ADMIN_PASSWORD | Una contraseña propia, entre 8 y 72 caracteres. |
| ADMIN_NAME | Nombre visible, por ejemplo Administrador. |

`.env.example` sólo explica las variables: **Spring Boot no lo carga automáticamente**. No dejar vacíos los secretos obligatorios. El administrador se crea si ese correo no existe; cambiar las variables después no cambia la contraseña de una cuenta existente.

Ejecutar `GastroreservaBackend1Application`. Alternativamente, con JDK 21 y las variables ya definidas en esa terminal:

```powershell
.\mvnw.cmd spring-boot:run
```

Dejarlo funcionando. Abrir http://localhost:8080/api/health: debe responder que la API funciona.

## 4. Frontend

Abrir **la carpeta frontend** en WebStorm, o una segunda terminal desde la raíz:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Abrir la dirección de Vite, normalmente http://localhost:5173. La URL de API pública está configurada en `.env.development` como `http://localhost:8080/api`. Si se cambia, reiniciar Vite.

Ingresar con **ADMIN_EMAIL/ADMIN_PASSWORD de GastroReserva**, no con las credenciales de PostgreSQL. La sesión se mantiene en memoria; recargar exige iniciar sesión otra vez.

Si Vite cambia a otro puerto porque 5173 está ocupado, liberar tu proceso anterior o configurar ese origen exacto en `CORS_ALLOWED_ORIGINS` del backend y reiniciar Spring. Los orígenes iniciales permitidos son localhost y 127.0.0.1 en 5173. No abrir CORS a cualquier origen.

## 5. Qué probar y cómo preparar datos

La base de cada compañero empieza vacía, salvo su administrador. **No se copiaron la base, cuentas, reservas ni contraseñas del autor.** Las tablas iguales vienen de Flyway; los registros se crean en cada equipo.

1. Inicio: indicadores; Clientes: crear y editar un perfil. Crear un perfil no crea una cuenta de acceso.
2. Para reservar, preparar una zona, una mesa y un turno activos: usar `zonas.http`, `mesas.http` y `turnos.http` del backend como administrador. Usar los ids devueltos, no suponer que todos son 1.
3. Reservas: crear con fecha futura, consultar historial, filtrar, corregir una SOLICITADA o cancelar una reserva ficticia.
4. Cancelar es terminal; dar de baja cliente es lógico y permite reactivarlo. El backend valida capacidad, solapamiento, permisos y estado.

Para los archivos HTTP, copiar `http-client.private.env.json.example` como `http-client.private.env.json`, completar credenciales propias y seleccionar **Run with: local**. Ejecutar primero el login de `auth.http`. Para probar como cliente, registrar con `auth.http` y luego iniciar sesión con `clientes.http`. El archivo privado y `*.local.http` no se suben a Git.

Guías y 50 respuestas: [frontend/docs/guia-10.md](frontend/docs/guia-10.md). Estado real: [docs/PA-03-status.md](docs/PA-03-status.md). Hay administración de clientes/reservas, consulta de mesas y dashboard; el resto de interfaces, móvil e infraestructura siguen pendientes.

## 6. Verificación

Backend desde la raíz con JDK 21:

```powershell
.\mvnw.cmd verify
```

Frontend desde `frontend/`:

```powershell
npm.cmd run check
```

Referencia del cierre de guía 10: 129 pruebas backend H2 y 98 frontend (79 Node + 19 Vitest), además del recorrido manual PostgreSQL. Las pruebas no usan la base habitual. `npm run build` sólo genera `dist`; para ver código mientras trabajan se usa `npm run dev`.

## 7. Trabajo en equipo y seguridad

- No subir `node_modules`, `dist`, `target`, `.idea`, archivos privados ni credenciales. Las dependencias se reconstruyen con `npm ci` y Maven Wrapper.
- Quien use ZIP puede descargarlo otra vez para recibir cambios; quien use Git puede actualizar con `git pull` cuando no tenga cambios locales sin guardar. No usar force push ni borrar el trabajo de otro compañero.
- GitHub comparte el **código**, no hospeda automáticamente Spring Boot/PostgreSQL ni sincroniza las bases de las computadoras.
- En esta computadora se conserva el frontend original de WebStorm; la carpeta `frontend/` del repositorio es la copia incluida para el grupo. Si se sigue editando el original, sincronizar sus cambios revisados antes de un futuro commit. Los compañeros pueden trabajar directamente sobre `frontend/`.
- Se retiraron credenciales de los ejemplos compartidos, pero una contraseña administrativa estaba en el historial anterior. Si sigue vigente, su titular debe cambiarla desde el procedimiento autorizado de la aplicación. No se reescribió el historial ni se cambiaron contraseñas automáticamente.
