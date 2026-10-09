# Guía 05 — React conectado a Spring Boot

Adaptación de ParkFlow360 a **GastroReserva: Cliente 1:N Reserva**, 2026-10-08.

## Resultado y límites

Los formularios ahora realizan POST reales y las tablas usan GET. No hay vehículos, placas ni campos ajenos al contrato. Se agregó login mínimo porque nuestra API ya exige JWT; no se deshabilitó seguridad para seguir un ejemplo público.

Las capturas muestran una **base temporal de pruebas**, no los registros habituales del usuario. Las pruebas terminaron sin publicar código ni usar Docker/IA.

## Checklist

- [x] Contratos de Controller/DTO contrastados con solicitudes HTTP reales.
- [x] VITE_API_URL pública y centralizada, tipada en vite-env.d.ts.
- [x] CORS existente verificado con origen permitido y rechazado.
- [x] apiClient reutilizable con fetch, response.ok, JSON, errores y 204.
- [x] clienteService y reservaService, peticiones tipadas y mapeo de respuestas.
- [x] GET real en ambos listados; los componentes no importan mocks.
- [x] POST padre (cliente) y POST hija (reserva operativa), ids asignados por servidor.
- [x] Relación real por clienteId y catálogos activos obtenidos desde la API.
- [x] Carga, vacío, errores, Guardando, bloqueo y éxito visibles.
- [x] Fallos intencionales: URL incorrecta, body inválido y CORS; diagnóstico documentado.
- [x] Evidencia de UI, respuestas/estados HTTP y persistencia PostgreSQL.
- [ ] Verificación literal en Swagger UI: este backend no incluye Swagger/OpenAPI; se usó el contrato Java + HTTP observable, sin inventar rutas.
- [ ] Capturas de **DevTools → Network** de GET/POST: pendientes de tomarlas con el estudiante. Las capturas de UI no las sustituyen.
- [ ] Defensa oral sin mirar código: debe practicarla el estudiante.

La implementación funcional de la guía está disponible. Estos tres puntos académicos no se presentan como completados automáticamente.

## Contratos usados

| Acción | Método y ruta relativa a VITE_API_URL | Datos | Permiso |
|---|---|---|---|
| Login | POST /auth/login | email, password → accessToken, tokenType, expiresAt, usuario | Público |
| Listar clientes | GET /clientes | Lista ClienteResponse | ADMINISTRADOR/HOST |
| Crear cliente | POST /clientes | nombre, email, telefono → 201 ClienteResponse | ADMINISTRADOR/HOST |
| Consultar agenda | GET /reservas | Lista ReservaResponse anidada | ADMINISTRADOR/HOST/MESERO |
| Crear reserva operativa | POST /reservas/operativas | clienteId, fecha, turnoId, mesaId, cantidadPersonas, observaciones → 201 ReservaResponse | ADMINISTRADOR/HOST |
| Opciones mesa | GET /mesas?activa=true | MesaResponse con zona anidada | Personal |
| Opciones turno | GET /turnos?activo=true | TurnoResponse | Personal |

VITE_API_URL incluye /api. El service agrega /clientes, no /api/clientes otra vez.

Ejemplos de cuerpos **ficticios** (los ids deben existir; no ejecutarlos ciegamente):

```json
{
  "nombre": "Ana Prueba",
  "email": "ana.prueba@example.test",
  "telefono": "70010009"
}
```

```json
{
  "clienteId": 1,
  "fecha": "2030-01-15",
  "turnoId": 1,
  "mesaId": 1,
  "cantidadPersonas": 4,
  "observaciones": "Ejemplo de reserva"
}
```

El DTO de respuesta de reserva incluye cliente, mesa y turno anidados; el service toma cliente.id, cliente.nombre y mesa.numero para la tabla. No confundir mesa.id con mesa.numero. En la prueba: mesaId=1, número visible=12.

El request de alta no es simplemente Omit<Cliente,'id'>: el modelo contiene activo, pero el alta no permite elegirlo. En el código se llama ClienteCreatePayload y está definido explícitamente en ClienteFormData.ts; ReservaOperativaPayload está en ReservaFormData.ts. Equivalen a los tipos CreateRequest del ejemplo docente. Los campos extra de práctica local de guía 04 se retiraron.

## Capas y recorrido

1. El formulario controla inputs, cancela el submit HTML y valida.
2. La función de preparación normaliza texto y convierte ids/cantidad a números.
3. El service conoce endpoint y mapea el contrato.
4. apiClient agrega URL y cabeceras, envía fetch y comprueba response.ok.
5. Spring Security valida JWT/rol; Controller valida DTO; Service aplica reglas y transacción; Repository/JPA persiste.
6. PostgreSQL devuelve los datos; Spring responde con DTO JSON, no entidad JPA.
7. El service verifica/proyecta JSON; el formulario recibe el objeto creado.
8. La página actualiza estado; React dibuja la fila y el formulario se limpia.

React nunca se conecta directamente a PostgreSQL. CORS controla el acceso del navegador a respuestas desde otro origen, pero no autentica al usuario.

## Qué hacen las piezas

- apiClient.ts configura URL pública, token en memoria e invalidación 401.
- httpClient.ts contiene el transporte reusable, ApiError, status/code/fieldErrors, timeout de 15 s y AbortController.
- useApiQuery ejecuta GET desde Effect y cancela/ignora respuestas al desmontar.
- useSubmission ejecuta POST sólo desde el evento del formulario y evita reentradas con un ref, además de deshabilitar controles.
- Los services validan la forma del JSON en ejecución: una anotación TypeScript no transforma ni valida una respuesta externa.
- AccessBoundary limita vistas según rol; Spring sigue siendo la autoridad incluso si el rol cambia después del login.
- El JWT sólo está en memoria. Al recargar se inicia sesión otra vez. La contraseña no se persiste ni se registra en logs.

Una cancelación del navegador no deshace una transacción que el servidor ya recibió. Tampoco hay reintentos automáticos de POST; ante pérdida de conexión, actualizar el listado antes de intentar otra alta.

## Diagnóstico de los fallos intencionales

| Prueba aislada | Evidencia | Diagnóstico |
|---|---|---|
| GET /api/clientes-inexistentes autenticado | Inicialmente 500; luego 404 ROUTE_NOT_FOUND tras corrección y prueba de regresión | Ruta que no existe. No era un problema del JSON de cliente. |
| POST /api/clientes con nombre vacío/email inválido | 400 VALIDATION_ERROR y fieldErrors | DTO rechazado. No se llegó a crear un cliente. |
| Origen localhost:5174 no autorizado en API temporal, que sólo permitía 127.0.0.1:5174 | Preflight OPTIONS 403; formulario muestra error de conexión | CORS: cambia el host y por tanto cambia el origen. |
| API temporal detenida | Error visible y botón Reintentar carga; ningún mock | Fallo de conexión. Al volver, token antiguo rechazado con 401 y nueva autenticación recupera listas persistidas. |
| Email repetido | 409, inputs conservados y una sola fila | Restricción de unicidad del backend. |
| Misma mesa/turno/fecha ya reservada | 422, inputs conservados y una sola reserva | Regla RN-02 del backend, no un problema de fetch. |

La API temporal se configuró deliberadamente para estas pruebas. La configuración normal mantiene los orígenes 5173 existentes. No se dejó una URL equivocada ni CORS roto en el proyecto.

## Evidencia Network que debes tomar para clase

1. Encender backend y Vite; abrir la web en el navegador habitual.
2. Abrir herramientas de desarrollador y la pestaña **Network/Red**. Filtrar Fetch/XHR.
3. Iniciar sesión y entrar a Clientes. Seleccionar GET /api/clientes.
4. Capturar método, URL, estado 200 y Response/Preview (sin mostrar Authorization ni datos personales reales).
5. Abrir Nuevo cliente y usar datos ficticios autorizados para tu base. Enviar una sola vez.
6. Seleccionar POST /api/clientes. Capturar Payload con los tres campos y Response con 201/id.
7. En Reservas repetir con POST /api/reservas/operativas y comprobar que clienteId coincide.
8. No exportar ni compartir un HAR completo sin sanearlo: puede contener contraseña de login, JWT y datos privados.

El registro y capturas de UI de esta entrega están en [recorrido-http.md](evidencias/guia-05/recorrido-http.md). No se fabricaron capturas de Network ni se afirmó que Swagger estaba instalado.

## Pruebas ejecutadas

- Frontend: 32 pruebas automáticas de formularios, HTTP, contrato y sesión; lint y build correctos.
- Backend: verify con JDK 21, 112 pruebas H2, cero fallos/errores/omitidas, JAR generado.
- Navegador con API Spring y PostgreSQL 17 temporales: GET, POST 201 de ambas entidades, unicidad 409, solapamiento 422, navegación, persistencia tras reinicio, mesero sin botón de alta, acceso denegado a clientes, CORS rechazado y API apagada.
- Los casos de 204, timeout, cancelación, respuesta malformada y algunos estados HTTP adicionales se verificaron automáticamente con fetch simulado; no se presentan como endpoints reales nuevos.
- Diseño comprobado en escritorio y pantalla angosta; tablas con desplazamiento horizontal. Capturas reales adjuntas.
- No se ejecutaron las altas de prueba sobre la base de datos habitual.

## Las 30 preguntas, en sencillo

**1. ¿Qué problema resuelve VITE_API_URL?**  
Guarda la dirección del backend en un solo lugar. Si cambia, no debemos editar todos los componentes. El service sólo indica el recurso, por ejemplo /clientes.

**2. ¿Por qué VITE_ no debe contener contraseñas?**  
Porque Vite copia su valor al JavaScript que descarga el navegador. No es secreto aunque venga de un archivo .env.

**3. ¿Qué es un origen?**  
La combinación de protocolo, host y puerto. http://localhost:5173 y http://localhost:8080 son orígenes distintos; localhost y 127.0.0.1 también son hosts distintos.

**4. ¿Por qué Postman puede funcionar y React fallar por CORS?**  
Porque el navegador aplica la política de CORS. Postman no está sujeto a esa misma restricción; que funcione allí no prueba que el origen web esté autorizado.

**5. ¿CORS autentica o protege directamente la base?**  
No. Autoriza orígenes del navegador. JWT y permisos identifican/autorizan usuarios; sólo el backend accede a PostgreSQL.

**6. ¿Qué devuelve fetch inmediatamente?**  
Una Promise. Al resolverse obtenemos un Response con status, headers y cuerpo; todavía no es nuestro objeto Cliente.

**7. ¿Qué hace await?**  
Espera el resultado de una Promise dentro de una función async y luego continúa. No congela toda la página mientras espera la red.

**8. ¿Por qué un 404 no entra siempre al catch?**  
Porque fetch considera que recibió una respuesta HTTP. Nosotros comprobamos response.ok y lanzamos ApiError si no fue exitosa.

**9. ¿Qué significa response.ok?**  
Es true para códigos HTTP del 200 al 299. No garantiza por sí solo que el JSON tenga los campos que espera nuestra aplicación.

**10. ¿Por qué response.json() es asíncrono?**  
Porque el cuerpo de la respuesta puede seguir llegando como un flujo y debe leerse antes de convertirlo. Devuelve otra Promise. Nuestro cliente lee text() y luego JSON.parse para tolerar también errores sin JSON.

**11. ¿Para qué apiClient?**  
Para no repetir URL, headers, token, control de status, JSON y errores en cada componente. Centraliza la mecánica HTTP.

**12. ¿Qué hace clienteService?**  
Conoce /clientes, ejecuta listar por GET y crear por POST, y adapta la respuesta al modelo de vista. No dibuja botones ni guarda directamente en la base.

**13. ¿Modelo Cliente y ClienteCreateRequest son iguales?**  
No. Cliente representa lo que mostramos, con id y activo. El request de alta sólo permite nombre, email y teléfono; no debemos enviar todo el modelo mediante spread.

**14. ¿Por qué no enviamos id en el POST?**  
Porque lo genera el backend/base al crear el registro. React no decide su clave primaria.

**15. ¿Para qué JSON.stringify?**  
Convierte el objeto JavaScript del formulario en texto JSON, que es lo que enviamos como cuerpo HTTP.

**16. ¿Cuándo agregamos Content-Type: application/json?**  
Cuando enviamos un body JSON. Un GET sin body no lo necesita. Accept: application/json indica qué respuesta esperamos.

**17. ¿loading y submitting son distintos?**  
loading indica que se están consultando datos con GET. submitting indica que se está enviando un formulario con POST.

**18. ¿Por qué deshabilitamos Guardar?**  
Para impedir que el usuario envíe otra vez mientras esperamos. También hay un bloqueo con ref. Esto ayuda en la UI, pero las reglas y restricciones finales siguen en el backend.

**19. ¿Qué ocurre desde Guardar Cliente hasta PostgreSQL?**  
El handler valida y prepara el request; clienteService hace POST mediante apiClient; Spring Security verifica permiso; Controller recibe DTO; Service valida y usa Repository/JPA para guardar en una transacción.

**20. ¿Qué ocurre de PostgreSQL hacia React?**  
Spring construye el DTO del cliente creado y responde 201 con JSON. El service lo proyecta, onCreated actualiza el estado del listado y React muestra la nueva fila.

**21. ¿Por qué usar el objeto devuelto?**  
Contiene el id y valores definitivos del servidor. Inventar un id podría mostrar una fila que no corresponde con la base.

**22. ¿Qué revisar en Network ante 400?**  
Request Payload, Content-Type y Response/fieldErrors. Comparar los nombres, tipos y valores con el DTO que recibe el Controller.

**23. ¿Qué revisar ante 404?**  
Request URL y método; comparar ruta exacta y, si hay un id, comprobar que exista. En nuestra API protegida primero debe haber una sesión válida.

**24. ¿Qué revisar ante 500?**  
La petición y respuesta en Network y los logs de Spring Boot. El backend tuvo un fallo interno; cambiar el formulario a ciegas no identifica su causa.

**25. ¿Cómo distinguir CORS de JPA?**  
CORS se investiga con el origen y la petición OPTIONS/errores del navegador. JPA se investiga en los logs del servidor y sus operaciones de persistencia. Un mensaje Failed to fetch por sí solo no alcanza para distinguir CORS de red.

**26. ¿Para qué AbortController en GET desde Effect?**  
Cancela la solicitud cuando la pantalla se desmonta o una carga deja de ser válida. Además ignoramos respuestas canceladas para no mostrar datos viejos.

**27. ¿Qué riesgo tiene POST dentro de Effect?**  
Un Effect puede ejecutarse nuevamente por montaje, cambios o comprobaciones de desarrollo. Podría crear registros sin una nueva intención del usuario. El POST va en el evento de envío.

**28. ¿Cómo representamos Cliente 1:N en nuestro proyecto?**  
Un cliente puede tener muchas reservas. El formulario selecciona un cliente real y manda clienteId numérico en POST /reservas/operativas. Spring valida la referencia y guarda la FK de la reserva.

**29. ¿Y si la API espera cliente: {id:5}, no clienteId:5?**  
Se adapta el request al DTO real, no se inventa otro contrato. GastroReserva actualmente recibe clienteId en el alta operativa y devuelve cliente anidado en la respuesta: entrada y salida no tienen que ser idénticas.

**30. Explica el viaje completo.**  
Formulario → service → apiClient/fetch → seguridad y Controller → Service → Repository/JPA → PostgreSQL → DTO JSON con 201 → service → estado de React → tabla. Si falla, se muestra el error y no se inventa una fila.

## Siguiente guía

Esperar la guía 06 y adaptar su CRUD al backend de GastroReserva. No asumir que debemos borrar físicamente registros: nuestro modelo conserva referencias y utiliza estados/bajas lógicas donde corresponde. Esta guía no añadió edición, eliminación, pagos ni facturación.
