# Guía 08 — Estado, efectos, asincronía y hooks

Adaptación a GastroReserva de la guía docente ParkFlow360, recibida el 2026-10-08. Vehículo se adapta a **Reserva**, manteniendo la relación Cliente 1:N Reserva de la guía 07. No se agregan vehículos ni se cambian las reglas del restaurante.

## Qué cambió y para qué sirve

La interfaz sigue permitiendo lo mismo: CRUD de perfiles de clientes y gestión limitada de reservas. Esta guía organiza cómo React carga, conserva y actualiza sus datos para no mezclar respuestas viejas con nuevas.

- Clientes y Reservas usan hooks de dominio, con lógica compartida para GET de listas y detalles.
- Se puede **Cancelar consulta** mientras carga un detalle o después de un error. No guarda ni cancela una reserva: solamente cierra esa consulta.
- Al cambiar id, reintentar o salir, se cancela el GET anterior; además se ignoran sus resultados tardíos, incluidos catch/finally.
- Cargando lista, cargando detalle, guardando y dando de baja son operaciones distintas. No se reemplaza toda la tabla por un spinner al eliminar una fila.
- Los avisos de éxito desaparecen a los 5 segundos. Cada nuevo aviso reinicia el tiempo; los errores permanecen hasta su tratamiento.
- StrictMode sigue activado. No se usa una bandera para impedir su segunda comprobación.

No hay cambios de API, tablas, migraciones, permisos ni reglas Java en esta guía. El frontend requiere el contrato de la guía 07 (V9). Cancelar una reserva sigue siendo un cambio de estado definitivo y auditado, no un DELETE; editar sigue limitado a cliente/observaciones de SOLICITADA.

## Estado fuente y derivado

| Estado fuente (sí se conserva) | Derivado (se calcula) |
|---|---|
| Respuesta de la API: clientes y agenda | Cantidad total, activos/inactivos y canceladas |
| Filtro seleccionado por el usuario | Filas visibles según ese filtro |
| Modo de panel e id seleccionado | Panel abierto, posibilidad de editar y controles bloqueados |
| Carga/error de consulta; operación de guardado | saving/cancellingId según la operación; deletingId del cliente pendiente |
| Campos escritos en un formulario | Validez y payload construido antes de enviar |
| Aviso de éxito y su revisión temporal | Visibilidad del aviso según su contenido |

Los filtros y contadores **ya eran derivados** antes de esta guía; se conservaron así. No se inventó un useState redundante sólo para eliminarlo. Sí se retiró la coordinación repetida de controladores, loadingDetail, detailError y carga imperativa del detalle de ambas páginas. El panel conserva el id, no otra copia del objeto obtenido por GET.

## Separación de responsabilidades

```text
ClientesPage / ReservasPage
    → useClientes / useReservas / hooks de detalle
        → useAsyncList / useAsyncDetail → useApiQuery
            → service del módulo → apiClient → fetch → Spring Boot
```

La flecha del hook genérico al service representa el loader que recibe como argumento; el hook genérico no importa endpoints de clientes o reservas.

| Archivo | Responsabilidad |
|---|---|
| src/hooks/asyncQuery.ts | Una ejecución, AbortController y bandera active; sólo publica callbacks de la ejecución vigente. |
| src/hooks/useApiQuery.ts | data/loading/error, limpieza del efecto, reload estable y actualización funcional de datos. |
| src/hooks/useAsyncList.ts | Especialización reutilizable para listas T[]. |
| src/hooks/useAsyncDetail.ts | Consulta por id; null deshabilita/cierra el detalle, cambiar id cambia el loader. |
| src/features/clientes/hooks/useClientes.ts | Conecta los hooks a listar/obtener cliente del service. |
| src/features/reservas/hooks/useReservas.ts | Agenda y catálogo autorizado en paralelo; detalle e historial cuando corresponde. MESERO no solicita el catálogo de clientes. |
| src/hooks/useSuccessMessage.ts | Aviso temporal, reinicio del timer y clearTimeout en limpieza. |
| src/hooks/useSubmission.ts | Hook anterior conservado: mutación disparada por evento, busy/error y bloqueo inmediato de doble envío. |
| src/features/*/pages | Selección, filtros, paneles y sincronización de la tabla con la respuesta del servidor. |
| src/features/*/services y src/api | Contratos, rutas, DTO, HTTP/JSON, JWT, timeout y errores. No se trasladan rutas a los hooks genéricos. |

Dos extracciones concretas de repetición: ciclo de GET por id en ambas páginas y ciclo de avisos de éxito. Los listados comparten useApiQuery; la agenda usa un objeto con reservas/clientes para mantener la carga conjunta, no dos copias de la misma lista.

### Contrato del hook de consulta

`useApiQuery(loader, initial)` recibe un loader **estable** o null y devuelve data, loading, error, setData, setError y reload. initial es el valor vacío inicial, no una dependencia reactiva ni caché. `useAsyncList<T>(loader)` lo especializa a listas y `useAsyncDetail<T>(id, loaderPorId)` a detalles opcionales.

Las funciones de service sin dependencias ya son estables. Si el loader depende de un id o permiso, se usa useCallback con esas dependencias reales. No pasar una función nueva en cada render al hook genérico.

El efecto depende de loader y revision. reload cancela de inmediato la consulta actual e incrementa revision. Al cambiar la identidad de consulta, un ajuste de estado condicionado limpia datos/error/carga antes de mostrar el nuevo recurso; no es un efecto que filtre o copie arreglos. No se hace setState incondicional durante el render.

La limpieza pone active=false y aborta. Aunque un loader no respete signal o termine después de una transformación asíncrona, no puede aplicar un success, error o finally obsoleto. Cerrar y reabrir el mismo id vuelve a obtener datos; no reutiliza un detalle viejo. Dos llamadas al mismo hook tienen estados independientes.

### Efecto versus evento

El GET sincroniza la pantalla con una selección o recarga: va en un efecto con limpieza. POST/PUT/PATCH/DELETE ocurren al pulsar y confirmar, nunca por montar un componente. Los filtros y contadores se calculan directamente: no necesitan efectos. El temporizador del aviso sí es un recurso externo y se limpia.

AbortError voluntario no se muestra como fallo de negocio. Un 404, 409, 422, 500, fallo de contrato o de conexión sí se trata como error real. El timeout mantiene su mensaje específico; no se oculta como cancelación voluntaria.

**Abortar el navegador no revierte una escritura ya recibida por Spring.** Por eso no prometemos rollback ni reintentamos POST automáticamente. Si se pierde la conexión al guardar, consultar el listado antes de repetir.

## Cómo comprobarlo

1. Encender PostgreSQL/backend habitual y ejecutar `npm.cmd run dev` en WebStorm. Ingresar con una cuenta ADMINISTRADOR/HOST de GastroReserva.
2. Abrir Clientes: verificar carga, tabla y filtro. Crear/editar un perfil de prueba; la respuesta del servidor actualiza la fila y el aviso dura 5 segundos.
3. En DevTools → Network, activar una limitación de red. Abrir Editar y, mientras aparece Cargando, pulsar Cancelar consulta o navegar a otra ruta. El GET pendiente debería aparecer cancelado; no debe mostrarse un error de negocio ni abrirse después el formulario viejo. Si la respuesta ya terminó, no habrá nada pendiente para abortar: repetir con más latencia.
4. Reabrir el detalle. Debe cargar el id seleccionado, sin mezclar nombre/error de otra selección. Hacer lo mismo en Reservas → Detalle/Editar.
5. Conservar StrictMode y observar el montaje en desarrollo: puede verse un primer GET cancelado y otro vigente. No deben ocurrir escrituras por montar ni peticiones ilimitadas. El conteo exacto visible depende del momento de la cancelación y del navegador.
6. Para error real, detener **tu proceso de desarrollo** de Spring durante una consulta, verificar mensaje y reintentar después de encenderlo. No apagar servicios compartidos. No modificar la ruta real de producción para provocar errores.
7. Con Vite encendido, abrir `/tests/hooks.html` y pulsar **Ejecutar pruebas de hooks**. Son pruebas React aisladas; no necesitan login/API ni escriben en PostgreSQL. Esperar 12/12. Esta página no forma parte del build de la aplicación.

En la terminal del frontend:

```powershell
npm.cmd test
npm.cmd run lint
$env:VITE_API_URL = 'http://localhost:8080/api'
npm.cmd run build
Remove-Item Env:VITE_API_URL
```

67 pruebas Node + 12 pruebas de hooks en navegador; lint/build correctos. Los dos grupos son distintos: npm test no ejecuta la página de hooks. Ver [evidencia del recorrido real](evidencias/guia-08/recorrido-http.md). Las capturas de UI no sustituyen capturas de DevTools Network ni la explicación oral del estudiante.

## Las 40 preguntas, respondidas para GastroReserva

1. **¿Qué problema resuelve la guía 08 después del CRUD?** Que el CRUD no sólo funcione una vez: organiza cargas, estados y errores, evita respuestas fuera de orden y permite reutilizar esa lógica.

2. **¿Qué diferencia hay entre estado fuente y derivado?** Fuente es lo que recibimos o el usuario decide, como clientes y filtro. Derivado es lo que podemos calcular con eso, como los clientes visibles.

3. **¿Por qué no guardar clientesVisibles en useState?** Porque duplicaría información y podría quedar desactualizado. Lo calculamos con filter usando clientes y filtro.

4. **¿Qué es useEffect?** Es un hook para sincronizar el componente con algo externo, como una consulta HTTP o un temporizador, después del render confirmado.

5. **¿Cuándo usar un efecto y cuándo un evento?** Efecto: mantener datos acordes a la pantalla o id seleccionado. Evento: responder a una acción concreta, como Guardar o Confirmar baja.

6. **¿Por qué un POST no debe depender del montaje?** Porque montar o volver a montar no significa que el usuario quiera crear otro registro. Guardar se ejecuta desde el envío del formulario.

7. **¿Para qué sirve el arreglo de dependencias?** Declara los valores reactivos usados por el efecto. Si alguno cambia, React limpia la ejecución anterior y sincroniza otra vez.

8. **¿Qué ocurre al cambiar una dependencia?** React compara su identidad/valor con Object.is; si cambió, ejecuta la limpieza anterior y luego el nuevo efecto.

9. **¿Qué hace el cleanup?** Deshace o detiene la sincronización anterior: aquí aborta el GET y evita aplicar respuestas tardías; en el aviso cancela el timer.

10. **¿Cuándo se ejecuta el cleanup?** Antes de repetir el efecto por dependencias nuevas y al desmontar. StrictMode también prueba un ciclo adicional en desarrollo.

11. **¿Qué es AbortController?** Un objeto que permite enviar una señal de cancelación a operaciones que la aceptan, como fetch.

12. **¿Qué es AbortSignal?** La señal del controlador que pasamos del hook al service y al cliente HTTP; permite que fetch se entere de la cancelación.

13. **¿Qué pasa al abortar fetch?** Deja de esperar normalmente y rechaza con un error de cancelación. Eso no garantiza detener lo que el servidor ya procesó.

14. **¿Por qué AbortError no es un error funcional?** Porque suele significar que salimos o cambiamos de consulta a propósito, no que el cliente, la reserva o sus datos sean inválidos.

15. **¿Qué es una condición de carrera en una carga por id?** Dos consultas terminan en un orden distinto del esperado; una respuesta vieja podría pisar la selección actual.

16. **Dá un ejemplo.** Abrimos cliente 1 y luego cliente 2. Si la respuesta del 1 tarda más y llega al final, podría mostrarse Ana cuando seleccionamos Bruno.

17. **¿Cómo ayuda AbortController?** La limpieza cancela la consulta que ya no necesitamos, en lugar de seguir esperándola como vigente.

18. **¿Para qué sirve una bandera ignore o active?** Es una protección adicional: aunque una promesa no se pueda abortar, su resultado viejo no actualiza datos, error ni loading.

19. **¿Por qué StrictMode puede ejecutar efectos dos veces en desarrollo?** React hace una comprobación adicional de setup y limpieza para detectar efectos que no se limpian correctamente.

20. **¿Qué comprueba setup → cleanup → setup?** Que podamos iniciar, detener y volver a iniciar la sincronización sin dejar recursos pendientes ni mezclar resultados.

21. **¿Conviene quitar StrictMode para ocultar duplicados?** No. Corregimos la limpieza y las dependencias; quitarlo ocultaría el problema sin resolverlo.

22. **¿Conviene un useRef para impedir la segunda ejecución?** No como parche de “ejecutar una sola vez”. Los refs sí sirven para el controlador actual o bloqueo de doble clic, no para saltarse la comprobación.

23. **¿En qué se diferencian loading y saving?** loading indica que estamos leyendo datos. saving indica una escritura enviada por el usuario. Sus mensajes y controles no tienen por qué ser iguales.

24. **¿Para qué usar deletingId en lugar de loading global?** Identifica el cliente que se está dando de baja y permite mostrar el progreso en su acción sin hacer desaparecer toda la lista. Podemos bloquear acciones incompatibles igualmente.

25. **¿Qué es un custom hook?** Una función que combina hooks de React para reutilizar comportamiento con estado, como consultar una lista con loading/error/cleanup.

26. **¿Por qué su nombre comienza por use?** Es la convención que identifica un hook y permite aplicar sus reglas: llamarlo en el nivel superior de un componente u otro hook, no condicionalmente.

27. **¿Dos componentes que usan el mismo hook comparten estado?** No automáticamente. Comparten la lógica, pero cada llamada tiene su propio estado. Compartir datos requeriría otro mecanismo explícito.

28. **¿Qué responsabilidad tiene useAsyncList?** Exponer una lista y su ciclo de consulta reutilizando data/loading/error, recarga y cancelación. No decide permisos del negocio ni conoce URLs concretas.

29. **¿Qué sigue estando en el service?** La operación del módulo: endpoint, método, DTO de envío, validación/mapeo de la respuesta y propagación de signal.

30. **¿Por qué el loader debe ser estable?** Una función nueva en cada render parece una dependencia distinta y puede provocar GET repetidos. Usamos funciones del service o useCallback con dependencias correctas.

31. **¿Para qué sirve reloadToken?** Es un contador que cambia cuando pedimos recargar, aunque el id sea el mismo. Aquí se llama revision y vuelve a disparar el efecto.

32. **¿Por qué reload usa useCallback?** Mantiene la misma referencia entre renders y evita que quienes la reciben se resuscriban sólo porque se creó otra función equivalente.

33. **¿Por qué no usar un efecto para filtrar una lista?** Porque es un cálculo interno y sin recursos externos. Hacer filter durante el render es suficiente y evita sincronizar dos estados.

34. **¿Qué puede provocar GET infinitos?** Dependencias nuevas en cada render, como un loader inline, o actualizar desde el efecto un estado que vuelve a dispararlo continuamente.

35. **¿Cómo diagnosticar solicitudes inesperadas?** Revisar Network, dependencias, estabilidad del loader, navegación y StrictMode. Un ciclo adicional de desarrollo no es un bucle sin fin.

36. **Explicá Page → Hook → Service → apiClient.** La página pide los datos y los muestra; el hook administra cuándo/cómo cargar; el service define la operación del dominio; apiClient ejecuta HTTP y trata JSON, sesión y errores.

37. **¿Dónde filtrar pocos registros ya cargados?** En React, con un cálculo derivado. Nuestro filtro por cliente no vuelve a consultar cada fila.

38. **¿Y si hay muchos registros o paginación?** Se consulta al backend con filtros/paginación definidos en su contrato. No se descarga todo para filtrar una sola página ni se inventan parámetros.

39. **¿Qué pasa si salimos durante un GET?** El componente se desmonta, React ejecuta cleanup, el controlador aborta y active=false invalida callbacks tardíos. No aparece un error falso ni se actualiza la pantalla que dejamos.

40. **¿Cómo refactorizamos Clientes sin cambiar su comportamiento?** Movimos la consulta y el detalle a hooks, mantuvimos formularios y contratos, dejamos filtros derivados y escrituras por eventos. Comprobamos alta, edición, baja lógica, errores y tabla actualizada; no cambiamos la regla de negocio.

## Trazabilidad y límites

RF-02/RNF-09: carga, vacío, error, reintento y filtros coherentes. RF-03/RF-05/HU-06: se conserva el CRUD validado de clientes. RF-04/RF-08/HU-15 y RN-01/RN-02/RN-03: se conservan reservas, corrección limitada y trazabilidad decididas por Spring; la refactorización no permite eludirlas. RN-08 de reasignación de mesa sigue siendo un contrato distinto, sin nueva pantalla aquí.

Guías 01–08 implementadas en su adaptación al dominio. No significa 80% del esfuerzo del proyecto. Guías 09–10 y experiencias web pendientes; Docker/GitHub/IA/móvil no se trabajaron. No se cambió la base habitual del usuario. No se ejecutó nuevamente Maven porque este incremento no modifica backend; las 129 pruebas PostgreSQL corresponden a la guía 07, no a una nueva corrida.

Referencias primarias: [useEffect](https://react.dev/reference/react/useEffect), [StrictMode](https://react.dev/reference/react/StrictMode) y [reutilizar lógica con hooks](https://react.dev/learn/reusing-logic-with-custom-hooks).
