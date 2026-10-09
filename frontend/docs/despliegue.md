# Build, preview y preparación de publicación

No hay un sitio público desplegado. Docker, GitHub/CI y hosting siguen aplazados. Esta guía prepara los archivos y explica lo necesario sin inventar dominios ni credenciales.

## Demostración local del build

1. Encender PostgreSQL y Spring Boot en 8080 con las variables privadas del backend. No ponerlas en React.
2. En la carpeta del frontend, ejecutar `npm.cmd ci` si faltan dependencias y `npm.cmd run check`.
3. Detener **tu servidor Vite dev** con Ctrl+C en su terminal para liberar 5173. No detener procesos ajenos.
4. Ejecutar:

```powershell
npm.cmd run preview -- --host 127.0.0.1 --port 5173 --strictPort
```

5. Abrir http://127.0.0.1:5173, iniciar sesión, ir a Inicio/Clientes/Reservas. Abrir directamente y recargar `/clientes` y `/reservas`: deben mostrar la aplicación, no un 404 del hosting. La sesión es en memoria; una recarga completa vuelve a pedir ingreso.

Usamos 5173 porque el backend ya permite ese origen. `npm.cmd run preview` sin opciones suele usar 4173: si se elige otro puerto, añadir **ese origen exacto** a `CORS_ALLOWED_ORIGINS` del backend y reiniciar Spring. No utilizar `*` ni desactivar autenticación. Preview sólo sirve el último `dist`; después de cambios en código/configuración hay que repetir build.

## Configuración pública

- `.env.example`: referencia.
- `.env.development`: URL de desarrollo `http://localhost:8080/api`.
- `.env.production`: misma URL **para probar el build localmente**, con advertencia explícita. No sirve tal cual para publicar en Internet.
- `src/api/apiClient.ts` lee `VITE_API_URL` una sola vez. El cliente compartido valida URL y muestra errores de configuración.
- La configuración queda incorporada al compilar; cambiarla después en el servidor estático no cambia un `dist` existente.

Para un destino real, reemplazar la URL pública en `.env.production` o establecer `VITE_API_URL` en el proceso que compila. Ejemplo de forma (no ejecutar hasta disponer del destino):

```powershell
$env:VITE_API_URL = 'https://TU-DOMINIO-API/api'
npm.cmd run build
Remove-Item Env:VITE_API_URL
```

El entorno del proceso tiene prioridad. Usar valores reales sin contraseñas. Nunca incorporar secretos en archivos VITE_, código, capturas, `dist` o Git. El JWT se obtiene al ingresar y queda en memoria; no es una variable de compilación.

## Hosting SPA

Publicar **el contenido de dist** en un servidor estático HTTPS. `deploy/nginx.conf.example` es sólo una plantilla local para revisar, no instalada: sirve archivos existentes, usa index.html para rutas SPA, devuelve 404 para assets inexistentes y no enruta `/api/` a React. La variante asume que Spring está en otro origen.

Antes de usarla de verdad ajustar ruta raíz, dominio, certificados y HTTPS. No exponer una API HTTP desde una web HTTPS: el navegador puede bloquear contenido mixto. Si el hosting usa subcarpeta, diseñar y verificar `base`/basename; el proyecto actual supone publicación en la raíz.

## Lista pendiente antes de una publicación real

- Elegir hosting y dominios, autorizar publicación y costes si existen.
- Backend/PostgreSQL desplegados con secretos fuera del repositorio, respaldo y migraciones controladas.
- URL API HTTPS real al construir; CORS con el origen HTTPS exacto del frontend.
- Certificados y fallback SPA configurados sin interceptar API/assets.
- Repetir login, CRUD, relación, permisos, errores y recargas desde el dominio público.
- Verificar consola/Network sin publicar tokens; preparar estrategia productiva de sesión y observabilidad.

`npm run build` exitoso y pruebas locales no demuestran por sí solos que esa infraestructura exista o esté bien configurada.
