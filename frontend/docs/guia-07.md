# Guía 07 — GastroReserva: Cliente 1:N Reserva

Adaptación de la guía docente «CRUD Vehículo + relación Cliente 1:N», recibida el 2026-10-08. El objetivo es recorrer selector → identificador → HTTP → validación → relación JPA → FK, no copiar vehículos al restaurante.

## Qué quedó implementado

| Ejemplo ParkFlow360 | GastroReserva |
|---|---|
| Cliente padre / Vehículo hijo | Cliente padre / Reserva hija. Un cliente tiene muchas reservas; cada reserva tiene un cliente actual. |
| Alta con propietario | POST de reserva operativa con clienteId numérico, fecha, turno, mesa, personas y observaciones. |
| Editar propietario | GET por id + PUT de cliente/observaciones, sólo SOLICITADA, con motivo y versión. |
| Eliminar vehículo | Cancelar SOLICITADA/CONFIRMADA mediante PATCH, conservando reserva e historial. No hay DELETE físico ni reactivación. |
| Nombre del propietario | Map de clientes por id, sin request por fila; proyección del DTO como alternativa autorizada para MESERO. |
| Filtrar por padre | Todos los clientes / un cliente; comparación numérica y filtro local. |

La edición se limita deliberadamente: no cambia fecha, turno, mesa, intervalo ni cantidad de personas. No es un CRUD irrestricto. El backend ya tiene check-in y reasignación de mesa por contratos separados; esta guía no expone esas pantallas. Cancelar conserva trazabilidad y cumple los estados del proyecto. Confirmación y motivo se solicitan en la web.

Crear y editar usan formularios separados porque sus contratos y reglas no son intercambiables. El componente de edición se monta con la reserva consultada por GET, convierte el id a string y trabaja con una copia; Cancelar edición descarta esa copia sin HTTP. No fue necesario copiar el useEffect del ejemplo para lograr la precarga.

## Archivos para explicar en clase

- `features/reservas/models/Reserva.ts`: modelo de vista, clienteId y versión de la respuesta.
- `types/ReservaClienteRequest.ts`: datos del formulario y payload de corrección.
- `utils/reservaCrud.ts`: validación, conversión, filtro, Map, estados y reemplazo inmutable.
- `services/reservaService.ts`: listar, obtenerPorId, crear, actualizarCliente, cancelar e historial; no contiene JSX.
- `components/ReservaForm.tsx`: alta con selector controlado y DTO operativo.
- `components/ReservaClienteForm.tsx`: cliente activo, observaciones, motivo obligatorio, versión y campos de planificación sólo informativos.
- `components/ReservaCancelPanel.tsx`: confirmación, motivo y PATCH de estado.
- `components/ReservaDetail.tsx`: detalle/historial, actores y datos anteriores/nuevos.
- `components/ReservaTable.tsx`: props, callbacks y nombres por Map; sin fetch en filas.
- `pages/ReservasPage.tsx`: Promise.all, filtro, paneles y sincronización tras éxito.
- Backend: ReservaController → ReservaClienteService → Reserva/Cliente/HistorialReserva → repositorios; migración V9.

Los nombres anteriores son relativos a `src/`, excepto el backend. No se agregaron dependencias ni secretos. Los mocks históricos no se usan como respaldo cuando falla la API.

## Contrato real

Todos los endpoints siguientes llevan prefijo `/api` y JWT. Se contrastaron Controller, DTO, servicio y respuestas reales; Swagger UI no está instalado.

| Método y ruta | Resultado / permiso |
|---|---|
| GET /reservas | 200 lista. ADMINISTRADOR, HOST, MESERO. Admite filtros backend existentes, aunque esta pantalla filtra localmente. |
| GET /clientes | 200 catálogo. Sólo ADMINISTRADOR/HOST; no se llama para MESERO. |
| GET /reservas/{id} | 200 DTO vigente con version; personal autorizado. |
| GET /reservas/{id}/historial | 200 eventos; personal autorizado. |
| POST /reservas/operativas | 201 DTO creado; ADMINISTRADOR/HOST. |
| PUT /reservas/{id}/datos-cliente | 200 DTO actualizado; ADMINISTRADOR/HOST. |
| PATCH /reservas/{id}/estado | 200 DTO, aquí usado con CANCELADA; la web ofrece cancelación a ADMINISTRADOR/HOST. |

Ejemplo de corrección; usar ids y versión devueltos por tu API:

```json
{
  "clienteId": 2,
  "observaciones": "Corrección de la solicitud",
  "motivo": "Recepción registró otro titular",
  "version": 0
}
```

El id de la reserva va en la URL; no se crea otra reserva ni otro cliente. El usuario elige un nombre, pero manda el id numérico. `version=0` es válido; omitirla no lo es. Sólo se ofrecen clientes activos; Spring vuelve a comprobar existencia/actividad bajo transacción. Motivo: 1–500 caracteres no blancos; observaciones: opcionales, hasta 500. Texto vacío se normaliza a null.

Una modificación obsoleta distinta se rechaza con 409. Repetir exactamente el estado ya guardado devuelve el DTO vigente sin duplicar eventos (incluso si la versión enviada es anterior). El historial CORRECCION_CLIENTE guarda cliente/observaciones anteriores y nuevos, actor, motivo, fecha y estado SOLICITADA. La reserva pasa al perfil del nuevo cliente y sale de las reservas propias del anterior.

Cancelación:

```json
{"estado":"CANCELADA","motivo":"Cancelación solicitada por el cliente"}
```

No devuelve 204: devuelve 200 y una reserva con estado CANCELADA. La tabla reemplaza esa fila; no usa filter para borrarla. CANCELADA es terminal: no ofrece reactivar. No se eliminan registros para demostrar la guía.

## Estado y carga sin N+1

La página gestora carga reservas y clientes en paralelo, una consulta de cada tipo por carga. Abrir el alta consulta mesas y turnos; reutiliza clientes ya cargados. Abrir detalle/edición consulta una reserva concreta intencionalmente, no todas las filas. React StrictMode en desarrollo puede iniciar/abortar una carga adicional; eso no es un GET por fila.

El filtro no hace peticiones. Tras guardar se usa el DTO del servidor para agregar/reemplazar por id, se cierra el panel y se selecciona el cliente guardado para que el resultado sea visible. La lista no se modifica si el servidor falla. Los controles incompatibles y el doble envío se bloquean mientras hay una operación pendiente. AbortController/timeout ya existían; se conserva la protección de respuestas obsoletas al cambiar/cerrar paneles.

MESERO no tiene acceso al catálogo administrativo de clientes: obtiene su nombre del cliente anidado en ReservaResponse. Es una proyección de lectura, no otra columna duplicada ni otro dueño en PostgreSQL. El fallback último es `Cliente #id`, no un nombre inventado.

## Probar paso a paso

1. Reiniciá Spring Boot en IntelliJ. V9 se aplica mediante Flyway; respaldá primero datos importantes. No borres ni recrees tablas a mano. Un backend viejo sin version no cumple el contrato nuevo.
2. En WebStorm, carpeta `gastroreserva1`, `npm.cmd run dev`. Abrí la URL de Vite (normalmente 5173), ingresá como ADMINISTRADOR/HOST de GastroReserva.
3. Creá dos clientes activos. Asegurá zona, mesa y turno activos mediante los contratos existentes.
4. En Reservas, creá reservas de ambos clientes para días/mesas sin solaparse. Comprobá sus nombres y ids.
5. Filtrá cada cliente y Todos; la lista cambia sin petición adicional.
6. Editá una SOLICITADA. Primero cambiá algo y Cancelar edición: no se guarda. Después seleccioná otro cliente, escribí motivo y guardá: mismo id, nuevo cliente.
7. Abrí Detalle: aparece la corrección y sus valores anteriores/nuevos. Actualizar agenda vuelve a consultar la persistencia.
8. Cancelá una solicitud/confirmada con motivo. Primero probá Volver sin cancelar; luego confirmá con un dato ficticio. La fila permanece CANCELADA, sin editar/reactivar.
9. Si se recibe 409 de versión, copiá el texto que quieras conservar, cerrá y reabrí la edición para consultar el estado vigente. No fuerces ni cambies la versión a mano para sobrescribir.

**Guardar/cancelar escriben en tu base configurada.** Las evidencias de este incremento usaron exclusivamente servicios y datos ficticios separados.

## Diagnóstico y evidencia para el profesor

| Resultado | Interpretación |
|---|---|
| 400 | JSON/tipos/id/motivo/versión inválidos. Mirar fieldErrors. |
| 401 / 403 | Sesión ausente/caducada o rol no autorizado. |
| 404 | Reserva o cliente inexistente; distinguir mensaje y ruta. |
| 409 | En corrección, versión vieja que intenta otro cambio; no es una placa duplicada. |
| 422 | Estado no editable, cliente inactivo, capacidad/solapamiento/otras reglas de reserva. |
| No hay clientes en alta | No hay activos, fallo de carga o permisos; no inventar opciones. |
| No cambia el titular | Revisar URL/PUT, payload clienteId/version, status, cliente.id y version de Response; luego detalle/historial. |
| Respuesta 200 inválida | Reiniciar backend actualizado y contrastar DTO; no ocultar con mocks. |

DevTools (F12) → Network → Fetch/XHR: al crear capturá POST /reservas/operativas con clienteId numérico, 201 y el id devuelto; al corregir, PUT /datos-cliente con clienteId, motivo, version y 200. Compará el id de reserva antes/después. Al filtrar, no debe aparecer otra consulta. Para 404 de padre, en una base de pruebas usá un cliente inexistente con un formulario/request válido; no borres un cliente real. Los tests automáticos cubren ese caso. Ocultá Authorization, tokens y datos personales antes de compartir capturas.

Las [evidencias entregadas](evidencias/guia-07/recorrido-http.md) son capturas de UI y access log del servidor, no capturas de DevTools Network. Esa presentación y la defensa oral siguen siendo tareas del estudiante. No se marca el uso literal de Swagger como hecho.

## Defensa oral — 40 preguntas adaptadas

1. **¿Por qué Reserva es la hija en Cliente 1:N?** Porque muchas reservas pueden referirse a un mismo cliente, y cada reserva guarda la referencia a su cliente.

2. **¿Qué significa clienteId en el DTO del frontend?** Es el identificador del cliente elegido, no su nombre ni todos sus datos.

3. **¿Dónde existe realmente la clave foránea?** En PostgreSQL: `reservas.cliente_id` referencia `clientes.id` y la base impide referencias inexistentes.

4. **¿Por qué clienteId no es lo mismo que @ManyToOne?** clienteId es un dato del contrato HTTP; @ManyToOne describe cómo Java/JPA relaciona las entidades. La FK es la restricción de PostgreSQL. Son tres capas distintas de la misma relación.

5. **¿Qué endpoints usamos para gestionar Reserva?** GET lista/detalle, POST operativas, PUT datos-cliente y PATCH estado para cancelar. También GET historial. No existe DELETE físico de reservas.

6. **¿Por qué necesitamos GET /clientes?** Para ofrecer nombres válidos en el selector y resolverlos en la tabla. Sólo lo llaman los roles autorizados; MESERO usa el cliente incluido en la agenda.

7. **¿Por qué el selector debe ser controlado?** Para que lo elegido y el estado de React coincidan, se puedan validar y precargar al editar.

8. **¿Qué necesita un select controlado?** `value` con el estado y `onChange` que lo actualice; las option tienen value con el id.

9. **¿Por qué no ponemos selected en cada option?** React determina la seleccionada con el value del select. Evitamos dos fuentes de verdad.

10. **¿Por qué clienteId comienza como string en el formulario?** Porque los controles HTML entregan texto; una selección vacía se representa con `''`.

11. **¿Cuándo lo convertimos a number?** Para validar su identidad y al construir el payload, después de comprobar que la selección no está vacía y es válida.

12. **¿Qué pasa si Number(clienteId) da NaN?** Se rechaza. Debe ser un entero seguro positivo y corresponder a un cliente activo; no se envía un dato inválido.

13. **¿Por qué valida React si también valida Spring?** React avisa rápido y evita envíos inútiles. Spring protege los datos de verdad.

14. **¿Por qué no alcanza con validar en frontend?** Se puede llamar directamente a la API o usar datos que cambiaron mientras el formulario estaba abierto. El servicio y la base deben comprobarlos.

15. **¿Qué significa que un cliente tenga muchas reservas?** Varias filas de reservas pueden tener el mismo cliente_id; no significa guardar un arreglo dentro de una columna del cliente.

16. **¿Una reserva puede tener dos clientes simultáneos aquí?** No. Tiene un solo cliente actual. El historial conserva los anteriores, pero no los convierte en propietarios actuales.

17. **¿Qué hace Promise.all en la carga inicial?** Espera las promesas de reservas y clientes juntas y permite usar ambos resultados cuando terminan. Un fallo se trata como error de carga.

18. **¿Qué ventaja tiene cargar en paralelo?** Una consulta no espera a que finalice la otra; se reduce el tiempo total de espera.

19. **¿Qué riesgo tiene hacer GET cliente por fila?** Con muchas reservas se multiplican las solicitudes, la espera y los puntos de fallo.

20. **¿Qué es N+1 desde la UI?** Una consulta para la lista y N consultas adicionales para los padres de sus N filas.

21. **¿Cómo mostramos el nombre sin una petición por fila?** Cargamos los clientes una vez y buscamos cada clienteId en un Map local.

22. **¿Por qué un Map?** Permite consultar directamente por id, sin recorrer todo el arreglo para cada reserva.

23. **¿Qué pasa si el cliente no está en el arreglo?** Usamos el nombre de la proyección autorizada del DTO si existe; como último recurso mostramos Cliente #id. No inventamos un cliente ni ocultamos la fila.

24. **¿Cómo precargamos al editar?** GET de la reserva vigente, copia de sus campos y `String(reserva.clienteId)` para el select. La versión también se conserva para el PUT.

25. **¿Qué ocurre al cambiar cliente y actualizar?** React manda el nuevo id con motivo y versión; Spring verifica las reglas, cambia la referencia y audita. La UI usa el resultado real.

26. **¿Qué valida Spring antes de persistir?** Rol ADMINISTRADOR/HOST, reserva existente y SOLICITADA, cliente existente/activo, DTO válido y versión vigente si hay cambios.

27. **¿Qué columna cambia al reasignar el titular?** `reservas.cliente_id`. Además se actualizan versión/fecha de modificación y se inserta el evento de historial; la reserva mantiene su id.

28. **¿Qué diferencia hay entre crear y cambiar el cliente?** Crear inserta una reserva nueva con programación validada. Corregir actualiza una solicitud existente sin cambiar su fecha, mesa, turno ni cantidad.

29. **¿Cómo sincronizamos la fila después del PUT?** Con map reemplazamos la que tiene el id devuelto, sin mutar el arreglo original ni agregar otra reserva.

30. **¿Por qué usamos la respuesta y no sólo el payload?** Porque el servidor es la autoridad: devuelve normalizaciones, cliente real, estado, fechas y versión actualizada.

31. **¿Cómo funciona el filtro local?** Vacío muestra todas; un id seleccionado usa filter para conservar sólo las reservas de ese cliente.

32. **¿Por qué Number(clienteFiltro) importa?** El select da string y clienteId es number. Convertir permite comparar correctamente con igualdad estricta.

33. **¿Cuándo filtrar en backend?** Cuando hay muchos registros, paginación o criterios que no conviene resolver con toda la lista descargada.

34. **¿Podemos inventar ?clienteId= si el contrato no lo define?** No. Hay que revisar el contrato real. Nuestro GET /reservas sí admite ese filtro, aunque esta práctica usa el filtro local.

35. **¿Qué puede significar 409 en nuestro caso?** En la corrección, otra sesión cambió la reserva desde que abrimos el formulario. No debemos sobrescribirla con una versión vieja. Los solapamientos de reserva se informan como 422, no como placa duplicada.

36. **¿Qué significa 404 relacionado con clienteId?** Que el cliente referenciado no existe. También puede faltar la reserva objetivo: el mensaje permite distinguirlos.

37. **¿Qué miro en Network si un PUT no cambia el cliente?** Método/URL, clienteId numérico y version del Payload, status y cuerpo Response. Un error no se debe presentar como éxito; después comparar GET e historial.

38. **Explicá el POST hasta la FK.** Elegimos nombre, React valida y envía clienteId con los datos de reserva. Security/Controller reciben; Service busca cliente y valida fecha/capacidad/solapamiento; JPA inserta con cliente_id; PostgreSQL comprueba FK; el DTO 201 vuelve y actualiza la lista.

39. **Explicá el PUT que cambia de cliente.** GET precarga, elegimos otro cliente y motivo, enviamos PUT con versión. Spring bloquea/valida, cambia cliente_id y agrega historial en la misma transacción. Devuelve 200 con nueva versión; React reemplaza esa fila y muestra el cliente nuevo.

40. **¿Cómo mostramos el nombre sin duplicarlo en la entidad hija?** La relación persistida es cliente_id. En la vista usamos Map del catálogo. Para MESERO se aprovecha la proyección anidada que ya entrega el DTO; eso no añade una columna nombre_cliente a reservas ni cambia la FK.

## Cobertura y siguiente paso

Implementados selector controlado, referencias numéricas validadas, carga paralela autorizada, tabla sin N+1, filtro, alta, cambio de padre limitado con PUT, sincronización y errores. La eliminación del ejemplo se adapta a cancelación trazable; creación/edición usan formularios acordes a sus contratos. No se promete CRUD irrestricto ni 100% del frontend.

Verificación: 58 pruebas frontend, lint/build correctos; 129 pruebas backend sobre PostgreSQL 17; recorrido real de navegador descrito en las evidencias. Guía 08 pendiente de recibir: estado, efectos y hooks reutilizables. Docker, GitHub, IA y móvil no se trabajaron.
