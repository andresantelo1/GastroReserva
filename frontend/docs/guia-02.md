# Guía 02 adaptada a GastroReserva

Fuente: ParkFlow360_Guia_02_Estudiante_Routing_Layout_Menu_React.docx. Se adapta Vehículos a Reservas y se conserva el módulo Mesas de GastroReserva. No se incorpora negocio de estacionamiento.

## Qué quedó implementado

- [x] `react-router` instalado (8.4.0); package.json y lockfile actualizados.
- [x] `BrowserRouter` envuelve App en main.tsx.
- [x] App delega a AppRouter.
- [x] Rutas `/`, `/clientes`, `/mesas`, `/reservas`, `/acerca` y `*` para 404.
- [x] MainLayout reúne Sidebar, Header y Outlet.
- [x] NavLink señala la opción activa con fondo, subrayado y aria-current.
- [x] Páginas separadas para inicio y cada módulo; componentes tipados de la guía 01 conservados.
- [x] Página 404 dentro del mismo layout y enlace de regreso.
- [x] Menú horizontal desplazable en pantallas angostas.
- [x] AcercaPage y su enlace en el menú.
- [x] Ejercicios de ruta alterada y Outlet ausente comprobados y restaurados.
- [x] Flujo escrito en menos de ocho líneas.
- [ ] Estudiante: practicar y explicar el flujo sin leer el código.

El último punto es una habilidad del estudiante; no queda demostrado sólo porque el código funcione. Esta etapa no usa Axios, fetch, formularios ni llamadas al backend. El texto «Administración» describe el layout, no una sesión autenticada ni permisos otorgados.

## Mapa de archivos

| Archivo | Responsabilidad |
|---|---|
| `src/main.tsx` | Monta React con StrictMode y BrowserRouter. |
| `src/app/App.tsx` | Delega a AppRouter, sin mezclar páginas. |
| `src/routes/AppRouter.tsx` | Relaciona rutas con componentes dentro de MainLayout. |
| `src/layouts/MainLayout.tsx` | Conserva el menú y el encabezado; Outlet recibe la página hija. |
| `src/components/common/Sidebar.tsx` | Declara enlaces y pinta el activo. |
| `src/components/common/Header.tsx` | Encabezado común y aviso de demostración. |
| `src/pages/DashboardPage.tsx` | Inicio con accesos a los módulos. |
| `src/pages/AcercaPage.tsx` | Explica propósito, tecnología y límites de la etapa. |
| `src/pages/NotFoundPage.tsx` | Informa rutas inexistentes y permite volver al inicio. |
| `src/features/*/pages/*Page.tsx` | Página de Clientes, Mesas o Reservas. |
| `src/styles/global.css` | Layout, menú activo, foco visible y diseño adaptable. |

## Flujo del clic en Clientes en seis líneas

1. El usuario pulsa Clientes en el menú.
2. NavLink cambia la URL a `/clientes` sin pedir otro documento HTML completo.
3. BrowserRouter comunica la nueva ubicación.
4. Routes encuentra la Route cuyo path es `clientes`.
5. MainLayout conserva Sidebar y Header.
6. Outlet muestra ClientesPage y NavLink marca Clientes como activo.

## Ejercicios comprobados

**Cambiar `clientes` por `clientes2`.** El enlace del menú siguió apuntando a `/clientes` y mostró 404. Al abrir `/clientes2` se mostró ClientesPage, pero el enlace Clientes ya no correspondía a esa dirección. Después se restauró `path="clientes"`.

**Quitar Outlet.** La URL y la opción activa siguieron cambiando; el encabezado y el menú permanecieron. El área principal quedó vacía porque no había dónde renderizar la página hija. Se restauraron la importación y `<Outlet />`, y volvió a aparecer ClientesPage.

**Acerca.** Se creó su componente, se agregó la ruta y luego el NavLink. Se verificó tanto con clic como activando el enlace con Enter.

## Cómo probarlo vos

1. En WebStorm abrir la terminal de `gastroreserva1` y ejecutar `npm.cmd run dev` si todavía no está encendido.
2. Abrir la URL de Vite. No encender PostgreSQL ni el backend para esta guía.
3. Recorrer Inicio → Clientes → Mesas → Reservas → Acerca. Observar que sólo una opción del menú queda activa.
4. Usar Atrás y Adelante del navegador.
5. Abrir directamente `/clientes` y recargar; sigue funcionando en Vite.
6. Probar `/ruta-inexistente` y usar Volver al inicio.
7. Reducir el ancho: el menú pasa arriba. Desplazarlo horizontalmente para ver todas las opciones.
8. Ejecutar `npm.cmd run lint` y `npm.cmd run build` en otra terminal.

Si Vite dice que 5173 está ocupado, suele haber otro servidor ya abierto: probar su URL o mirar la terminal existente antes de iniciar otro. No hace falta apagar servicios ajenos.

## Verificaciones de esta entrega

Fecha: 2026-10-08. Comprobación en el proyecto real de WebStorm, no sólo en una copia.

| Caso | Resultado observado |
|---|---|
| Instalación | npm agregó React Router y dos dependencias transitivas; sin agregar Axios ni react-router-dom. |
| Build y lint | Ambos terminaron con código 0. TypeScript y Vite compilaron 105 módulos. |
| Cinco opciones del menú | Cada URL muestra su página y una sola opción activa. |
| Historial | Atrás vuelve a Acerca y Adelante a Inicio, manteniendo el layout. |
| Acceso directo y recarga | `/clientes` sigue mostrando ClientesPage después de recargar. |
| Ruta inválida | `/ruta-inexistente` muestra 404 y permite volver a Inicio. |
| Ejercicios temporales | Fallos esperados observados y código correcto restaurado. |
| Escritorio y móvil | Sin desbordamiento horizontal del documento a 1366, 390 y 320 px; menú horizontal desplazable en móvil. |
| Teclado | Enter sobre Acerca abre AcercaPage. |
| Consola | Sin errores de aplicación observados durante la navegación normal. |

Se reutilizó el Vite que ya estaba escuchando en el puerto 5173; no se detuvo ni reemplazó ese proceso. Las pantallas siguen usando ejemplos locales, no datos reales. Los resultados no son una prueba de CRUD, permisos ni login.

Capturas: [Inicio en escritorio](evidencias/guia-02/inicio-escritorio.jpg), [Reservas en móvil](evidencias/guia-02/reservas-movil.jpg).

## Respuestas sencillas para practicar la defensa

1. **¿Qué es una SPA?** Una aplicación que carga una base HTML y cambia las vistas con JavaScript. En nuestro menú no necesitamos cargar otro documento completo para cada sección.
2. **¿Qué resuelve React Router?** Relaciona la dirección del navegador con la página que React debe mostrar y permite navegar usando el historial.
3. **¿Por qué BrowserRouter está en la raíz?** Para que todas las pantallas y enlaces que están dentro puedan usar la navegación.
4. **¿Qué hace main.tsx?** Inicia React y monta App con BrowserRouter en el elemento root del HTML.
5. **¿Qué hace App.tsx ahora?** Devuelve AppRouter, sin decidir por sí mismo qué página mostrar.
6. **¿Por qué separar AppRouter?** Para tener juntas las direcciones de la aplicación y no mezclarlas con su presentación.
7. **¿Qué hace Routes?** Selecciona las rutas que coinciden con la ubicación actual.
8. **¿Qué hace Route?** Declara una ruta y el componente correspondiente, o agrupa rutas hijas.
9. **¿Qué es index?** La página por defecto del padre; en este proyecto es Inicio en `/`.
10. **¿Qué hace path="*"?** Recibe las direcciones que no coinciden con nuestras páginas y muestra NotFoundPage.
11. **¿Por qué `clientes` no empieza con /?** Es una ruta hija relativa al padre. El layout sin path no añade un segmento, por eso queda `/clientes`.
12. **¿Qué es el layout?** La estructura que comparten varias páginas. No pertenece sólo a Clientes, por eso vive en layouts.
13. **¿Qué hace Outlet?** Es el lugar donde el layout muestra la página hija que coincide con la ruta.
14. **¿Qué pasa sin Outlet?** Se ve el marco, pero no la página hija, aunque cambie la URL.
15. **¿Link y NavLink?** Ambos sirven para navegar dentro de la app. NavLink además informa si su destino está activo.
16. **¿Qué es isActive?** Un valor verdadero o falso que indica si ese enlace coincide con la ubicación según sus opciones.
17. **¿Por qué no un a href normal para el menú?** Normalmente pediría otro documento al servidor. Link y NavLink permiten la navegación del router. El enlace de salto a un fragmento sí usa un ancla normal.
18. **¿Flujo al pulsar Clientes?** NavLink cambia la URL, BrowserRouter comunica la ubicación, Routes encuentra la Route y Outlet muestra ClientesPage dentro del layout.
19. **¿Dónde agregaría /reportes?** Declararía la ruta en AppRouter e importaría su nueva página; no se ha agregado en esta guía.
20. **¿Y para mostrarlo en el menú?** Además agregaría una entrada en menuItems de Sidebar.
21. **¿Por qué no llamamos todavía al backend?** Para aprender y probar primero la navegación sin mezclar problemas de sesión, red o datos.
22. **¿Cómo compruebo que no recarga todo?** En DevTools, al pulsar un NavLink normal no debe aparecer una nueva petición de tipo Document; cambia la URL y la vista. Recargar con F5 sí vuelve a pedir el documento.
23. **¿URL y archivo son lo mismo?** No. `/clientes` es una dirección; ClientesPage.tsx es un componente. La Route relaciona los dos.
24. **¿Por qué atender las rutas inexistentes?** Para explicar qué pasó y permitir recuperarse, sin dejar una pantalla vacía.
25. **¿Qué hace el CSS responsive?** Acomoda el layout según el ancho; el menú lateral pasa a horizontal arriba y el contenido sigue siendo legible.

## Referencias y límites

Se siguió el modo declarativo de la guía, contrastado con [instalación](https://reactrouter.com/start/declarative/installation), [rutas](https://reactrouter.com/start/declarative/routing) y [navegación](https://reactrouter.com/start/declarative/navigating) oficiales de React Router.

La 404 es una vista del cliente; el hosting futuro tendrá que servir index.html en las rutas de la SPA. No se configuró despliegue ni se cambió el backend. Los requisitos de capacidad, solapamiento, estados y permisos siguen siendo autoridad del servidor, no del menú.
