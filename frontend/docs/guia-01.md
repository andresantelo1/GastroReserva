# Guía 01 adaptada a GastroReserva

Fuente: ParkFlow360_Guia_01_Estudiante_Estructuracion_Frontend_React_TS_Vite.pdf (11 páginas). Se adapta el dominio, no se incorpora el negocio de estacionamiento.

## Alcance

Estructura, componentes simples, tipos, configuración pública y arranque. Sin Router, Axios, formularios, persistencia ni login. No se modifican el backend ni sus datos.

## Checklist

- [x] Reutilizar el proyecto React + TypeScript + Vite existente en WebStorm, separado de Java.
- [x] Ubicar App en `src/app/App.tsx` y actualizar su importación en `src/main.tsx`.
- [x] Preparar carpetas comunes y módulos por funcionalidad.
- [x] Adaptar clientes/vehículos a clientes/reservas; conservar mesas y auth.
- [x] Componentes ClienteCard, MesaCard y ReservaCard con tipos mínimos y datos de ejemplo visibles.
- [x] Limpiar recursos de la demo Vite y centralizar estilos en `src/styles/global.css`.
- [x] Documentar URL pública en `.env.example`, sin credenciales.
- [x] Explicar más de cinco carpetas en `README-front.md`.
- [x] Mantener `.gitignore` para dependencias, compilados y secretos locales.
- [x] Verificar instalación, lint, build, navegador y recarga en caliente; resultados abajo.
- [ ] Estudiante: comprobar los plugins React/Vite en su WebStorm.
- [ ] Estudiante: cerrar y reabrir WebStorm y arrancar desde su terminal.
- [ ] Estudiante: capturar el árbol de carpetas expandido en WebStorm para la entrega.

Los tres últimos puntos son actividades del IDE del estudiante, no funciones pendientes de programación. No se afirma haberlos realizado automáticamente.

## Cómo demostrarlo en clase

1. Abrir `gastroreserva1` en WebStorm y mostrar `src/app`, `features` y `services/http`.
2. Abrir la terminal y ejecutar `npm.cmd run dev`.
3. Abrir la URL que informa Vite. Mostrar las tarjetas y el aviso de datos de ejemplo.
4. Editar una frase de `src/app/App.tsx`, guardar y ver el cambio automático; luego deshacer ese cambio.
5. Mostrar un tipo, por ejemplo `Cliente`, y dónde se usa en las propiedades de `ClienteCard`.
6. En otra terminal ejecutar `npm.cmd run build`; explicar que `dist` contiene el resultado para publicación.
7. Mostrar `.env.example` y explicar por qué no contiene la contraseña de PostgreSQL.

Para la captura del IDE, expandir `src`, `app` y los módulos dentro de `features`. Para la captura del navegador, abrir la app funcionando. Guardar ambas en `docs/evidencias/guia-01`.

## Preguntas para practicar (adaptadas)

1. **¿Qué son Node, npm, Vite, React y TypeScript?** Node ejecuta herramientas JavaScript en la computadora; npm instala paquetes y ejecuta comandos; Vite arranca y compila el frontend; React construye la interfaz con componentes; TypeScript ayuda a detectar errores de tipos antes de ejecutar.
2. **¿Qué problema resuelve Vite?** Nos da un servidor rápido para desarrollar y prepara los archivos para publicar, sin configurar todo desde cero.
3. **¿Para qué sirve main.tsx?** Es la entrada: busca el elemento `root` del HTML, inicia React y muestra App.
4. **¿Qué contiene package.json?** El nombre del proyecto, sus dependencias y comandos como dev, build y lint.
5. **¿Por qué no subir node_modules?** Tiene dependencias descargadas y puede ser enorme. Cada compañero las instala usando package.json y el lockfile.
6. **¿Qué hace npm run dev?** Enciende Vite para trabajar y ver cambios locales en el navegador.
7. **¿Qué hace npm run build?** Comprueba TypeScript y crea los archivos de producción en dist. No crea nuevas funcionalidades.
8. **¿Por qué separar por features?** Para que lo de clientes quede junto y lo de reservas también. Así encontramos y cambiamos cada tema sin mezclar todo.
9. **¿Qué diferencia hay entre common y un componente de feature?** Un botón compartido puede ir en common; una tarjeta que muestra una reserva pertenece a reservas.
10. **¿Por qué React no se conecta a PostgreSQL directamente?** Expondría credenciales y permitiría saltarse permisos/reglas. React llama al backend y éste controla el acceso a los datos.
11. **¿Por qué no guardar contraseñas en variables VITE_?** Se incorporan al código que recibe el navegador, así que no son secretas.
12. **¿Qué ventaja tiene TypeScript?** Detecta, por ejemplo, enviar texto donde se esperaba un número. No reemplaza las validaciones del backend.
13. **¿Qué pasa si movemos App.tsx?** Hay que corregir las rutas de importación que apuntan a él, especialmente en main.tsx, y verificar el build.
14. **¿Qué es HMR?** Es la actualización en caliente: al guardar un componente, Vite actualiza la pantalla sin reiniciar el servidor; puede conservar el estado según el cambio.
15. **¿Cómo se conectará este frontend con el resto?** Navegador con React → solicitudes HTTP a Spring Boot → PostgreSQL. En esta guía sólo se construye la primera parte con datos locales.

## Evidencia técnica

Verificado el 2026-10-08 en `C:\Users\antel\WebstormProjects\gastroreserva1`:

| Comprobación | Resultado |
|---|---|
| Dependencias existentes | `npm.cmd install --offline --ignore-scripts --no-audit --no-fund`: up to date; sin agregar librerías. Se aprovechó la instalación local; no es una prueba en una computadora limpia. |
| Revisión de código | `npm.cmd run lint`: salida 0, sin errores reportados. |
| Compilación final | `npm.cmd run build`: salida 0, TypeScript estricto y Vite, 19 módulos transformados. |
| Desarrollo | Vite arrancó en `http://127.0.0.1:5173/`. |
| HMR | Cambio temporal de texto visible automáticamente en el navegador sin recargar; texto original restaurado. |
| Detención y reinicio | Se detuvo con Ctrl+C y se inició nuevamente; página comprobada tras reiniciar. |
| Navegador | Tres tarjetas visibles; no se observaron errores de consola. |
| Adaptación visual | 1366 px: tres columnas; 390 px: una columna. Sin desbordamiento horizontal en ambos tamaños. |

Capturas reales de la página:

- [Escritorio](evidencias/guia-01/navegador-escritorio.jpg).
- [Vista móvil del sitio web](evidencias/guia-01/navegador-movil.jpg). No es una app React Native.
- [Estructura comprobada](evidencias/guia-01/estructura.md). Este texto no sustituye la captura del IDE solicitada al estudiante.

Se retiraron sólo recursos iniciales sin uso: `src/App.css`, `src/index.css`, `src/assets/hero.png`, `src/assets/react.svg`, `src/assets/vite.svg` y `public/icons.svg`. Los archivos anteriores y los recursos retirados se conservaron en:

`C:\Users\antel\AppData\Local\Temp\gastroreserva1-guia01-backup-ac868c432c744e9fa37d55518024a59d`

No confundir un build exitoso con tener login, reservas reales o conexión al backend. Tampoco se verificó el cierre/reapertura de WebStorm ni sus plugins; siguen como actividad manual del checklist.
