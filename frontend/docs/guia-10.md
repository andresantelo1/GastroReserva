# Guía 10 — integración final de GastroReserva

Fecha: 2026-10-09. Adaptación de la guía del docente de ParkFlow360 a **Cliente 1:N Reserva**. Cierra la secuencia didáctica de estas diez guías; no declara terminado todo el producto GastroReserva ni acredita la defensa oral.

## Qué se incorporó

- Inicio ahora consulta la API y muestra cuatro indicadores: clientes registrados, clientes activos, reservas registradas y reservas activas. También muestra los seis estados de reserva.
- `MetricCard` es reutilizable. Los valores se calculan a partir de las listas, no se guardan como otro estado. Activas significa SOLICITADA + CONFIRMADA + SENTADA; incluye todas las fechas cargadas, **no significa ocupación actual**.
- El dashboard usa el hook existente `useReservas(true)`, que obtiene clientes y reservas en paralelo. Sólo ADMINISTRADOR/HOST lo montan; no solicita `/clientes` con el rol MESERO. No hay fetch por fila ni duplicación de la carga de clientes.
- Carga, error con reintento y listas vacías se distinguen. Un fallo no se representa como cuatro ceros ni se reemplaza por mocks.
- `AppErrorBoundary`, fuera de BrowserRouter, muestra una recuperación si un descendiente falla al renderizar. Reintentar vuelve a montar sus hijos; volver al inicio recarga y pierde la sesión en memoria. No registra telemetría externa ni muestra trazas en producción.
- Vitest, jsdom y Testing Library agregan 19 pruebas de componentes, formularios, integración de página y errores. Se conservan las 79 pruebas Node anteriores.
- Configuración pública de producción para **demostración local**, plantilla de servidor estático y procedimiento de build/preview. No se publicó ningún sitio.

## Archivos para estudiar

| Archivo | Para qué sirve |
|---|---|
| `src/pages/DashboardPage.tsx` | Página de inicio, acceso por rol y módulos existentes. |
| `src/features/dashboard/components/DashboardData.tsx` | Coordina carga/reintento del resumen. |
| `src/features/dashboard/components/DashboardSummary.tsx` | Presenta indicadores, estados y vacío. |
| `src/features/dashboard/utils/dashboardMetrics.ts` | Cálculos puros, sin HTTP ni modificación de las listas. |
| `src/components/ui/MetricCard.tsx` | Tarjeta genérica sin conocimiento de clientes o reservas. |
| `src/app/AppErrorBoundary.tsx` | Recuperación de errores de render. |
| `src/main.tsx` | Monta ErrorBoundary, StrictMode y Router. |
| `vitest.config.ts`, `src/test/setup.ts` | jsdom, aserciones DOM, limpieza y bloqueo de HTTP real en pruebas. |
| `src/**/*.test.tsx` | 19 pruebas con React y Testing Library. |
| `tests/error-boundary.html` | Demostración aislada de un fallo; sólo Vite dev, no incluida en `dist`. |
| `.env.production`, `deploy/nginx.conf.example` | Configuración pública y ejemplo de hosting; no un despliegue activo. |

## Errores: tres situaciones distintas

1. **Transporte/HTTP:** la API no responde o devuelve 401/403/404/500. El cliente HTTP y los hooks los convierten en mensajes/reintentos. Fetch no rechaza automáticamente por 404: hay que revisar `response.ok`.
2. **Negocio:** el backend rechaza correo duplicado (409), versión obsoleta (409), capacidad/solapamiento/estado (422). El formulario conserva los datos y no inventa un éxito local.
3. **Render de React:** una pantalla lanza una excepción al dibujarse. ErrorBoundary muestra su fallback. No sustituye try/catch de handlers, servicios o promesas, y no garantiza capturar errores de eventos o asincronía.

La prueba del boundary se abre con `npm.cmd run dev`, entrando a `/tests/error-boundary.html`: Provocar fallo → aparece el mensaje; Retirar fallo → Reintentar pantalla → reaparece el contenido. El error intencional de consola es esperado sólo en esa práctica.

## Pruebas y compilación

Desde la carpeta que contiene `package.json`:

```powershell
npm.cmd ci
npm.cmd run lint
npm.cmd run test:run
npm.cmd run build
```

`npm.cmd test` abre Vitest en vigilancia; `test:unit` ejecuta sólo las 79 Node; `test:run` ejecuta las 79 Node y las 19 Vitest una vez. `npm.cmd run check` reúne lint, ambas suites y build. Las 8 UI y 12 hooks de las guías 08–09 conservan sus páginas de práctica manual: no se suman a las 98 actuales ni se presentan como reejecutadas aquí.

Las pruebas Vitest usan dobles de servicios/transporte, sin base real. Cubren MetricCard (incluido cero), Badge, Button, búsqueda controlada, paginación/límites/tamaño/vacío, métricas derivadas, carga/roles/error/reintento, validación/normalización/409 y búsqueda/filtro de ClientesPage, además del boundary. MSW era opcional; no se agregó. La prueba fullstack real complementa estos dobles, no se sustituye por ellos.

Ver [matriz fullstack y evidencias](evidencias/guia-10/matriz-fullstack.md) y [procedimiento de despliegue](despliegue.md).

## Adaptaciones obligatorias al negocio

| Ejemplo ParkFlow | GastroReserva y criterio de aceptación |
|---|---|
| Cliente → vehículo | Cliente → reserva, con clienteId numérico y FK existente. No se agregan vehículos ni campos ajenos al contrato. |
| Editar dueño libremente | Sólo corregir cliente/observaciones de SOLICITADA, con motivo, versión e historial. No cambia mesa/fecha/personas. |
| DELETE vehículo | Cancelar reserva con PATCH y motivo. Conserva fila, id e historial; es terminal. |
| Borrar cliente con dependencias da 409 | DELETE cliente es **baja lógica 204**, incluso con reservas. Conserva relaciones; no se cambió el backend para copiar ParkFlow. El 409 se prueba con correo duplicado. |
| Cuatro cifras de vehículos | Cuatro indicadores derivados de clientes/reservas y desglose por estado. No son aún el panel RF-14/RF-20 completo. |
| Swagger | Se contrastaron Controller, DTO, respuestas HTTP y SQL. Swagger UI no está instalado. |
| Publicación | Build/preview y plantilla preparados. Dominio, hosting, HTTPS y comprobación pública pendientes. |

Trazabilidad: RF-02/RNF-09 (lecturas y mensajes), RF-03/RF-05/HU-06 (CRUD de clientes), RF-04/RF-08/HU-15 (gestión limitada e historial), RNF-08 (arranque/configuración reproducibles). RF-14/RF-20/HU-18 permanecen **parciales en web**: el backend tiene reportes, pero este resumen no consulta ocupación/no-show ni filtros por rango. RN-01/RN-02/RN-03/RN-08 siguen bajo autoridad de Spring, sin cambios Java, SQL ni permisos en esta guía.

## Demostración sugerida al docente

1. Encender PostgreSQL y Spring Boot habituales; abrir Vite. Ingresar con una cuenta administrativa de GastroReserva, no de PostgreSQL.
2. Inicio: explicar de dónde salen las cuatro cifras y por qué no son ocupación actual.
3. Clientes: intentar guardar vacío, crear un perfil, editar teléfono, buscar y paginar.
4. Reservas: crear para ese cliente con fecha futura/mesa/turno válidos; explicar el id relacionado. El backend decide disponibilidad.
5. Corregir el titular de una SOLICITADA con motivo; abrir historial. Cancelar sólo una reserva de práctica, porque no se puede reactivar.
6. Mostrar un correo duplicado (409) sin perder lo escrito; explicar la diferencia con baja lógica de cliente (204).
7. Ejecutar `npm.cmd run test:run` y `npm.cmd run build`; mostrar preview según la guía de despliegue.
8. Si pide el flujo, abrir Network del navegador y mostrar método, URL, Request Payload, status y Response. Luego mostrar Controller → Service → Repository → PostgreSQL. Las capturas incluidas son UI, no Network.

No detener ni modificar una base compartida para demostrar errores. Usar datos de práctica, anotar su propósito y respaldar antes de cambios importantes. No mostrar tokens, contraseñas ni cabeceras Authorization en capturas para entregar.

## 50 preguntas y respuestas para defenderlo

### 1. ¿Qué ocurre desde Guardar cliente hasta PostgreSQL?

El formulario valida los campos, el service manda POST con JSON y JWT, Spring recibe el DTO, valida permisos y reglas, el Repository guarda y PostgreSQL asigna el id. La respuesta vuelve a React y actualiza la tabla. Si falla, no mostramos un éxito falso.

### 2. ¿Para qué usamos React Router?

Para mostrar una pantalla según la URL: Inicio, Clientes, Mesas o Reservas, sin recargar todo al usar los enlaces internos.

### 3. ¿Qué hacen MainLayout y Outlet?

MainLayout mantiene menú, cabecera y pie. Outlet es el espacio donde React Router coloca la página elegida. Así no repetimos el marco en cada pantalla.

### 4. ¿Qué diferencia hay entre componente UI y componente de una feature?

Un Button o MetricCard sirve en varios lugares y no conoce el negocio. ClienteForm sí conoce campos y acciones de clientes. Eso separa presentación reutilizable de reglas de una pantalla.

### 5. ¿Por qué Cliente y Reserva tienen modelos TypeScript?

Porque representan datos diferentes y TypeScript ayuda a detectar usos incorrectos al programar. La relación se expresa con clienteId. Además validamos el JSON recibido: los tipos solos no controlan datos externos en ejecución.

### 6. ¿Por qué los formularios usan strings y la API pide números?

Los inputs/select entregan texto, incluso al escribir un número. Conservamos ese texto mientras se edita y, después de validar, convertimos ids y cantidad a números para enviar el DTO correcto. Vacío no debe convertirse por accidente en cero válido.

### 7. ¿Qué ocurre al elegir un cliente del select?

El select guarda el id elegido como texto del formulario; al guardar se valida y convierte a número. Se envía clienteId, no el nombre. Spring verifica que ese cliente exista y esté habilitado.

### 8. ¿Para qué sirve clienteService?

Concentra las operaciones HTTP del módulo: listar, consultar, crear, actualizar y dar de baja. La página llama a esas funciones sin repetir rutas ni interpretar respuestas en cada botón.

### 9. ¿Por qué no poner muchos fetch en la página?

Mezclaríamos interfaz, URLs, tokens y errores. Con clienteService y apiClient podemos reutilizar, probar y cambiar la comunicación en un lugar. Tampoco hacemos una consulta por cada fila para obtener nombres.

### 10. ¿Qué debe hacer apiClient cuando response.ok es falso?

Interpretar de forma segura la respuesta de error y lanzar un error controlado con status/mensaje/campos cuando existan. La pantalla lo muestra; no trata ese cuerpo como un Cliente válido.

### 11. ¿Fetch lanza automáticamente un error por 404?

No. 404 es una respuesta HTTP recibida. Fetch suele rechazar por problemas de red o cancelación; por eso revisamos response.ok y status.

### 12. ¿Cómo distingo error HTTP, de negocio y de render?

HTTP describe la comunicación y su resultado; negocio explica una regla rechazada, muchas veces mediante 409/422. Render es que React falla al construir la pantalla. Los primeros se manejan en HTTP/hooks/formularios y el último con ErrorBoundary.

### 13. ¿Qué captura y qué no captura ErrorBoundary?

Captura errores de render y ciclo de vida de componentes descendientes. No es un capturador universal: no resuelve por sí solo promesas rechazadas, errores de handlers, errores del propio boundary ni problemas del servidor.

### 14. ¿Para qué sirve getDerivedStateFromError?

Para cambiar el estado del boundary cuando un hijo falla. Aquí activa hasError y React dibuja la pantalla de recuperación en vez del contenido roto.

### 15. ¿Para qué sirve componentDidCatch?

Para registrar información del fallo. Aquí muestra detalles sólo durante desarrollo y un mensaje genérico en producción, sin enviar información a servicios externos.

### 16. ¿Por qué no guardar las métricas también en useState?

Porque ya tenemos las listas y podemos calcular sus totales. Guardar otra copia obliga a sincronizarla y puede dejar cifras viejas. Conservamos la fuente y derivamos el resumen.

### 17. ¿Cuándo deberían calcularse las métricas en el servidor?

Cuando hay muchos datos, permisos complejos o listas paginadas por servidor: contar una página no es contar el total. También cuando se necesitan reportes oficiales por fechas/turnos, como ocupación y no-show.

### 18. ¿Para qué usamos useEffect?

Para sincronizar una pantalla con algo externo, como cargar datos al montarla o cambiar un id. Guardar una reserva por clic va en el evento del botón, no en un efecto de montaje.

### 19. ¿Por qué limpiar una petición en useEffect?

Porque la pantalla puede cerrarse o pedir otro id antes de recibir la respuesta. La limpieza aborta el GET e invalida sus resultados para que no actualicen una vista equivocada.

### 20. ¿Qué evita AbortController?

Permite cancelar una solicitud compatible usando su signal, ahorrando trabajo y evitando continuar una lectura que ya no interesa. Abortar una escritura no garantiza deshacer lo que el servidor ya guardó.

### 21. ¿Qué es una carrera entre respuestas?

Pido A y después B, pero A termina último. Sin protección A podría reemplazar la selección B. Nuestros hooks invalidan resultados anteriores, incluso sus errores y finally.

### 22. ¿Por qué StrictMode puede ejecutar un efecto dos veces en desarrollo?

Hace una comprobación adicional de montaje y limpieza para descubrir problemas. No significa que debamos quitarlo. Los efectos deben limpiarse bien y las escrituras no deben depender del montaje.

### 23. ¿Qué aporta un custom hook?

Reúne una lógica de estado y efectos reutilizable: datos, carga, error, reintento y cancelación. useClientes/useReservas usan los services; el hook no sustituye las reglas del backend.

### 24. ¿Por qué SearchInput no llama al service?

Porque es un control genérico: recibe value y avisa onChange. La pantalla decide qué buscar. En estas listas la búsqueda es local, no una petición por tecla.

### 25. ¿Por qué se filtra antes de paginar?

Para buscar en toda la lista cargada y contar los resultados correctos. Si filtramos sólo diez filas, ocultaríamos coincidencias de otras páginas.

### 26. ¿Qué hacemos si la página queda fuera del rango?

Ajustamos la página al nuevo límite; al cambiar búsqueda, filtro o tamaño volvemos a la primera. Sin resultados mostramos vacío y 0–0 de 0, no una tabla en una página inexistente.

### 27. ¿Por qué no usar Math.random() como key?

Porque cambia en cada render. React pensaría que todas las filas son nuevas, perdería estado/foco y haría trabajo innecesario. Usamos el id estable del servidor.

### 28. ¿Para qué sirve ConfirmDialog?

Para explicar y confirmar una acción antes de enviarla, como dar de baja un cliente. Permite cancelar sin HTTP, maneja foco y Escape, y bloquea confirmaciones repetidas mientras guarda.

### 29. ¿Cómo representa React la relación 1:N?

Un cliente puede aparecer como titular de varias reservas. Cada reserva tiene clienteId; el select elige ese id y un Map permite encontrar el nombre sin pedirlo por cada fila.

### 30. ¿Cómo representa PostgreSQL la relación 1:N?

clientes.id es clave primaria y reservas.cliente_id es clave foránea que la referencia. Varias reservas pueden usar el mismo cliente_id, pero no uno inexistente.

### 31. ¿Qué debe hacer la UI si un DELETE devuelve 409?

Mostrar el conflicto, conservar la fila y no simular que se eliminó. En nuestro contrato DELETE cliente normalmente devuelve 204 y hace baja lógica incluso con reservas; no inventamos una prohibición de ParkFlow. El 409 real de esta prueba fue un email duplicado al actualizar.

### 32. ¿Qué significa que PUT sea idempotente?

Repetir la misma solicitud debe dejar el mismo estado pretendido que enviarla una vez, sin crear copias ni efectos duplicados. No implica que cualquier cambio viejo sea válido: la corrección de reserva usa versión y puede rechazar un cambio distinto obsoleto con 409.

### 33. ¿Por qué DELETE puede devolver 204 sin JSON?

Porque 204 significa operación exitosa sin contenido de respuesta. apiClient debe aceptarlo sin intentar response.json(). En clientes la ausencia de cuerpo no significa borrado físico: el contrato dice baja lógica.

### 34. ¿Para qué sirve Vitest?

Para ejecutar pruebas automáticamente y comparar lo que hace el código con lo esperado. Aquí revisa componentes React, interacción, validación, errores y métricas.

### 35. ¿Por qué usamos jsdom?

Para disponer de document y elementos DOM en pruebas que corren con Node. No es un navegador completo: no reemplaza pruebas visuales, CSS, CORS ni integración real.

### 36. ¿Qué aporta Testing Library?

Permite renderizar componentes y encontrarlos por lo que percibe el usuario, como rol, etiqueta o texto. Probamos que la pantalla responde correctamente en vez de inspeccionar cada variable interna.

### 37. ¿Para qué sirve user-event?

Para simular interacciones como escribir, pulsar un botón o elegir una opción de forma cercana al uso real. Sus acciones se esperan con await cuando corresponda.

### 38. ¿Por qué probar comportamiento y no sólo detalles internos?

Porque podemos cambiar la implementación sin cambiar lo que necesita el usuario. Es más útil comprobar que un error conserva el formulario que exigir un nombre específico de variable.

### 39. ¿Cómo probarías Pagination?

Con suficientes filas compruebo que Siguiente avanza, Anterior se deshabilita al inicio y Siguiente al final. Cambio tamaño, reduzco resultados y pruebo cero filas para verificar reinicio y límites.

### 40. ¿Qué comando ejecuta todas las pruebas automáticas una vez?

`npm.cmd run test:run`. Ejecuta las 79 Node y las 19 Vitest. `npm.cmd test` deja Vitest en vigilancia; no es el comando de finalización para una verificación única.

### 41. ¿Qué hace npm run build?

Ejecuta `tsc -b` para comprobar TypeScript y después Vite genera los archivos optimizados. Puede fallar si hay errores de tipos. No inicia Spring ni pone la página en Internet.

### 42. ¿Qué contiene dist?

El HTML, CSS, JavaScript y recursos generados para servir la web. No es la base de datos ni contiene el código Java. No conviene editarlo a mano: se regenera desde src.

### 43. ¿Para qué sirve npm run preview?

Para servir localmente el build y revisarlo antes de publicar. No compila por sí solo ni sustituye un servidor de producción. El puerto suele ser 4173 y su origen debe estar permitido por CORS.

### 44. ¿Para qué sirve .env.production?

Vite lo carga al compilar en modo producción. Aquí tiene una URL de API local de ejemplo, no un servidor público. Las variables ya definidas en el proceso tienen prioridad; para otro destino hay que configurar su URL y volver a compilar.

### 45. ¿Por qué no guardar secretos en VITE_?

Porque se incorporan al JavaScript que recibe el navegador y cualquiera puede leerlas. Sólo ponemos configuración pública como la URL de API, nunca DB_PASSWORD, JWT_SECRET ni claves privadas.

### 46. ¿Por qué puede funcionar navegar a /clientes pero fallar al recargar?

Al navegar React Router cambia la vista; al recargar el navegador pide /clientes directamente al hosting. Si el servidor busca un archivo con ese nombre, devuelve 404. Hay que configurar fallback SPA.

### 47. ¿Qué es fallback SPA?

Servir index.html para las rutas del frontend que no son archivos reales, dejando a React Router elegir la vista. No debe transformar errores de API ni archivos faltantes en HTML exitoso.

### 48. ¿Por qué funciona en Postman y falla en el navegador?

El navegador aplica CORS; Postman no lo aplica de la misma manera. También pueden variar URL, token o contenido. Hay que mirar Network y permitir en Spring sólo los orígenes necesarios, no desactivar seguridad.

### 49. ¿Qué revisar antes de publicar frontend y backend separados?

URL real de API incorporada al build, HTTPS en ambos, origen exacto permitido en CORS, rutas/contratos, JWT, y resultados Network. Una web pública con localhost como API intentaría conectarse a la computadora del visitante.

### 50. ¿Cómo demostrás que la integración fullstack realmente funciona?

Creo un dato desde la interfaz, muestro su request y respuesta, explico Controller → Service → Repository, y consulto PostgreSQL. Después vuelvo a cargar la lista desde GET. También pruebo un error y la relación con una reserva; no alcanza con que la pantalla cambie usando un array local.

## Referencias primarias consultadas

- [React Component y Error Boundaries](https://react.dev/reference/react/Component).
- [Vitest: configuración](https://vitest.dev/config/) y [entornos](https://vitest.dev/guide/environment.html).
- [Vite: variables y modos](https://vite.dev/guide/env-and-mode), [build](https://vite.dev/guide/build) y [despliegue estático](https://vite.dev/guide/static-deploy.html).
