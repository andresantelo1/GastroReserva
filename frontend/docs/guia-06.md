# Guía 06 — CRUD de clientes en GastroReserva

La entidad padre es Cliente y la hija es Reserva. La implementación añade GET por id, edición mediante PUT y baja lógica mediante DELETE a las altas/listados de la guía 05.

## Qué se puede hacer

- Crear y listar clientes reales.
- Pulsar Editar: se obtiene una copia actual con GET /api/clientes/{id}, no se utiliza ciegamente la fila.
- Modificar nombre, email, teléfono y estado activo mediante el mismo formulario.
- Cancelar edición: vuelve a Nuevo cliente vacío sin POST/PUT.
- Eliminar con confirmación: DELETE da de baja, no borra físicamente.
- Consultar Activos/Inactivos/Todos y reactivar un perfil mediante Editar.
- Ver errores sin inventar cambios en la tabla ni perder los campos del formulario.

Sólo ADMINISTRADOR/HOST. Los permisos de Spring existentes cubren también DELETE; MESERO/CLIENTE reciben 403 y un anónimo 401. No se debilitaron seguridad ni CORS.

## Adaptación de la eliminación y la relación 1 N

GastroReserva necesita conservar reservas, auditoría y referencias. Por eso **Eliminar significa activo=false**. La confirmación lo informa antes de ejecutar el DELETE. El registro sigue en PostgreSQL, conserva su id/email y no desaparece de los listados administrativos que incluyen inactivos.

En Activos, la fila desaparece de esa vista al confirmarse 204. En Todos/Inactivos se ve con estado Inactivo. El array completo conserva el perfil y el filtro calcula qué mostrar. No se aplica filter para borrarlo de la copia completa: eso falsearía la baja lógica.

Una reserva existente no se cancela, no cambia de estado ni desaparece por dar de baja al cliente. El backend rechaza nuevas reservas operativas para un cliente inactivo con 422. No se borra ni se desactiva la cuenta de acceso vinculada; sigue siendo un concepto separado. Los endpoints de autoservicio que ya requieren perfil activo mantienen esa restricción (por ejemplo, /reservas/mias devuelve 404 si no hay perfil activo). No se cambiaron sus políticas en esta guía.

El DELETE funciona incluso si hay reservas hijas porque no elimina la fila padre ni rompe su FK. Repetirlo sobre un id existente ya inactivo devuelve 204; un id realmente inexistente devuelve 404. GET administrativo por id sigue devolviendo 200 para un inactivo. PUT con activo=true permite reactivarlo. No libera el correo único.

## Contratos exactos

| Acción | HTTP | Respuesta |
|---|---|---|
| Listar | GET /api/clientes | 200 y array, incluidos inactivos sin filtro |
| Buscar por id | GET /api/clientes/{id} | 200 y ClienteResponse, o 404 |
| Crear | POST /api/clientes | 201 y objeto creado |
| Editar/reactivar | PUT /api/clientes/{id} | 200 y objeto actualizado; 400/404/409 si falla |
| Dar de baja | DELETE /api/clientes/{id} | 204 sin body; 400 si id no positivo, 404 si no existe |

POST envía nombre, email y telefono. PUT agrega **activo obligatorio booleano**; no son el mismo DTO. En ambos casos telefono puede ser null. El id está en la URL de PUT/DELETE, no se genera en React ni se incluye en esos cuerpos. DELETE no lleva body.

Si el perfil tiene usuario vinculado, la actualización administrativa sincroniza nombre/email con esa cuenta, como ya hacía el backend. El formulario advierte ese efecto; la contraseña no cambia.

El backend no tenía DELETE de clientes: se agregó ClienteController.eliminarCliente → ClienteService.desactivar, transaccional. No hizo falta migración porque activo ya existe. Swagger UI continúa sin instalarse; los contratos se contrastaron con Controller/DTO y HTTP real.

## Código y responsabilidades

- ClienteRequests.ts distingue ClienteCreateRequest y ClienteUpdateRequest.
- clienteService tiene listar, obtenerPorId, crear, actualizar y eliminar. No hay fetch en la tabla.
- ClienteTable recibe onEdit/onDelete, disabled y deletingId. Expresa una intención mediante callback.
- ClientesPage coordina listado, detalle, modo de formulario, guardado, confirmación, errores y filtros.
- ClienteForm es único para crear/editar. POST o PUT se decide por la existencia de cliente con id, no por el texto del botón.
- useSubmission bloquea reentradas con ref, deshabilita controles y no anuncia éxito hasta recibir la respuesta.
- clienteCrud.ts contiene transformaciones puras de formulario, lista y filtro, con pruebas.
- El detalle tiene AbortController y se ignoran respuestas al desmontar. No se ejecutan mutaciones desde Effects.

**Diferencia intencional respecto del ejemplo:** el formulario recibe key según modo/id y utiliza un inicializador de useState. Cambiar selección o cancelar desmonta el formulario anterior y crea una copia limpia; cumple la sincronización sin un Effect que copie props a estado. No se pierde un formulario por un GET que llegue tarde: se bloquean otras acciones mientras se carga/edita.

Al crear/editar se usa la respuesta real: alta agrega sin duplicar id; edición reemplaza la fila con map. Al dar de baja, sólo después de 204 se marca inactivo en la copia local. Un error no ejecuta esos cambios. Guardar cambia el filtro a la vista del estado resultante para que el cliente siga visible. Actualizar lista hace un nuevo GET, sin recargar toda la página.

## Cómo probarlo

1. Reiniciar Spring Boot en IntelliJ: este incremento agregó un endpoint al backend.
2. Ejecutar npm.cmd run dev en WebStorm si el frontend no está encendido.
3. Iniciar sesión en http://localhost:5173/login con una cuenta ADMINISTRADOR/HOST de GastroReserva.
4. Clientes → Nuevo cliente → guardar datos ficticios permitidos para tu base.
5. Editar → modificar → Actualizar cliente. Comprobar que no apareció una fila duplicada.
6. Editar → escribir sin guardar → Cancelar edición. El formulario vuelve vacío y la base no cambia.
7. Eliminar → Cancelar baja. No se envía DELETE.
8. Eliminar → Confirmar baja. En Activos deja de verse, pero el contador de inactivos aumenta.
9. Mostrar → Inactivos → Editar → marcar Cliente activo → Actualizar. Conserva el id.
10. Si tenía una reserva, consultar Reservas: sigue allí. No eliminar ni alterar datos reales sólo para generar errores de prueba.

La sesión sigue siendo en memoria; una recarga requiere ingresar de nuevo. No usar credenciales de PostgreSQL en la web.

## Verificación realizada

- **44 pruebas frontend aprobadas**, lint correcto y build TypeScript/Vite correcto.
- **118 pruebas backend aprobadas**, sin fallos/errores/omitidas y Maven verify BUILD SUCCESS con JDK 21/H2.
- Recorrido navegador → Spring Boot → PostgreSQL 17 temporal: GET detalle, POST 201, PUT 200, conflicto 409, cancelación, DELETE 204, conservación de reserva, exclusión de inactivos del formulario de nuevas reservas y reactivación.
- Para demostrar un 404 real, se retiró exclusivamente un registro ficticio sin reservas de la base temporal después de cargar la lista. Editar respondió 404 sin abrir datos obsoletos; DELETE respondió 404 sin anunciar éxito ni ocultar falsamente la fila. Actualizar lista la sincronizó.
- Las pruebas automáticas del backend cubren ADMINISTRADOR/HOST, 401/403, 400/404/409, repetición idempotente de PUT/DELETE, inactivo, reactivación, cuenta vinculada y conservación de reserva/historial.
- Las pruebas del frontend comprueban rutas/métodos/body, 204, errores propagados, copia del formulario, sustitución por id, inmutabilidad y filtros.
- No se ejecutaron estas pruebas sobre la base habitual. Los servicios temporales se apagan al terminar; los datos/logs se conservan para revisión.
- No se afirma haber ejecutado las 118 pruebas completas sobre PostgreSQL: allí se hizo el recorrido HTTP real; la suite completa usó H2.

[Evidencias y capturas](evidencias/guia-06/recorrido-http.md).

## Evidencia para el profesor

Las capturas entregadas muestran la interfaz y hay un registro método/ruta/status. **No son capturas de DevTools Network.** Para completar ese punto académico:

1. Abrir Network/Red → Fetch/XHR en el navegador.
2. Capturar GET /clientes y GET /clientes/{id}, POST 201, PUT 200 y DELETE 204.
3. En PUT mostrar Request Payload con activo booleano y Response con el mismo id.
4. En DELETE comprobar que no hay body de respuesta y explicar la baja lógica.
5. Mostrar después GET por id o vista Inactivos para demostrar la conservación.
6. No incluir contraseñas, Authorization/JWT ni un HAR sin sanear en la entrega.

No se marca completada la defensa oral personal ni la evidencia Network pendiente. Tampoco se afirma haber usado Swagger UI, que no está instalado.

## Checklist del incremento

- [x] Service con las cinco operaciones.
- [x] Tabla con callbacks, sin lógica HTTP.
- [x] GET por id antes de editar, con carga/error.
- [x] Un formulario para crear/editar y Cancelar edición sin mutación.
- [x] POST incorpora respuesta; PUT reemplaza sólo el cliente correcto.
- [x] DELETE confirmado y 204 manejado sin parsear JSON.
- [x] Bloqueo de acciones incompatibles y errores sin cambios falsos.
- [x] Baja lógica coherente con reservas hijas, vista de inactivos y reactivación.
- [x] 404 real demostrado; 400/409/permisos cubiertos.
- [ ] Capturas de la pestaña Network para la entrega del estudiante.
- [ ] Defensa oral del estudiante sin mirar código.
- [ ] Swagger UI: pendiente si el docente exige esa herramienta concreta.

## Las 35 preguntas con respuestas sencillas

**1. ¿Qué operaciones incluye CRUD y qué métodos usamos?**  
Crear con POST, leer con GET, actualizar con PUT y eliminar con DELETE. En GastroReserva ese DELETE de clientes da de baja lógicamente.

**2. ¿GET /clientes y GET /clientes/{id}?**  
El primero devuelve una lista. El segundo busca un cliente concreto y devuelve un objeto o 404.

**3. ¿Por qué GET por id antes de editar?**  
Para obtener la versión actual del servidor. La tabla podría estar desactualizada o el registro podría ya no existir.

**4. ¿Qué representa el id de la URL?**  
La identidad del recurso que queremos leer, modificar o dar de baja. Spring lo recibe con PathVariable y busca su clave primaria.

**5. ¿POST /clientes y PUT /clientes/7?**  
POST crea un perfil nuevo con un nuevo id. PUT modifica el cliente que ya está identificado como 7.

**6. ¿Por qué POST no siempre es idempotente?**  
Porque repetir la misma petición podría crear otro registro. Una restricción única puede rechazarlo, pero no convierte todos los POST en idempotentes.

**7. ¿Por qué PUT es idempotente?**  
Porque enviar otra vez los mismos datos al mismo recurso deja el mismo estado de negocio. No crea otro cliente por cada envío.

**8. ¿Por qué DELETE es idempotente respecto del estado final?**  
Porque repetirlo no debe multiplicar el efecto. En nuestro contrato el perfil permanece inactivo; si el id no existe devuelve 404.

**9. ¿Responsabilidad de ClienteTable?**  
Mostrar filas y botones y avisar al padre cuando el usuario quiere editar o eliminar.

**10. ¿Por qué la tabla no llama directamente al service?**  
Para separar presentación de coordinación. La página maneja cargas, errores y actualización de estado; el service conoce HTTP.

**11. ¿Qué es editingCliente?**  
El cliente seleccionado para editar. Aquí se guarda en mode cuando kind es edit; incluye el objeto fresco obtenido por GET.

**12. ¿Cómo distingue el formulario crear y editar?**  
Si recibe un cliente persistido con id, utiliza PUT. Si no lo recibe, utiliza POST.

**13. ¿Por qué el ejemplo usa useEffect al cambiar cliente?**  
Para copiar los datos seleccionados al estado editable. Nosotros usamos key por modo/id y un inicializador: se crea un formulario limpio cuando cambia la selección, sin duplicar esa sincronización en un Effect.

**14. ¿Por qué no hacer PUT desde ese Effect?**  
Porque seleccionar o montar una pantalla no es una orden de guardar. Los Effects pueden repetirse; sólo el submit del usuario inicia la modificación.

**15. ¿CreateRequest y UpdateRequest?**  
Representan intenciones distintas y pueden tener campos diferentes. En nuestro POST no se envía activo; en PUT sí es obligatorio.

**16. ¿Qué pasa al presionar Editar?**  
La página bloquea acciones, llama obtenerPorId, recibe 200 y abre el formulario con una copia. Si recibe 404, muestra el error y no abre datos viejos.

**17. ¿Qué pasa con Cancelar edición?**  
Descarta los cambios locales y vuelve a Nuevo cliente vacío. No ejecuta POST ni PUT.

**18. ¿Qué envía el PUT?**  
Nombre, email, teléfono o null y activo booleano. El id viaja en /clientes/{id}; no enviamos contraseña ni campos de otro proyecto.

**19. ¿Por qué esperar antes de modificar la fila?**  
Porque el servidor puede rechazar los datos. Si cambiáramos la tabla antes, mostraríamos algo que no se guardó.

**20. ¿Cómo reemplazamos sólo la fila editada?**  
Con map: cuando id coincide, devolvemos el objeto actualizado del backend; en los demás casos conservamos el cliente anterior.

**21. ¿map para actualizar y filter para eliminar?**  
map transforma elementos y filter selecciona cuáles permanecen. En nuestro borrado lógico usamos map para marcar inactivo y filter para la vista Activos; no quitamos el perfil de la copia completa.

**22. ¿Qué devuelve DELETE con 204?**  
Éxito sin cuerpo de respuesta. No devuelve un cliente JSON.

**23. ¿Por qué no response.json() en 204?**  
Porque no hay cuerpo y parsearlo como JSON produciría un error aunque la baja haya salido bien.

**24. ¿Por qué pedir confirmación?**  
Para mostrar el cliente exacto y el efecto antes de cambiarlo. El usuario puede cancelar sin enviar DELETE.

**25. ¿Qué significa 404 durante GET por id?**  
Que el recurso no existe o la ruta está mal. Revisamos URL e id; no debemos abrir el objeto viejo de la tabla.

**26. ¿Qué significa 400 durante PUT?**  
Que la entrada no cumple el DTO/validaciones, por ejemplo email inválido o activo ausente. Revisamos Payload y fieldErrors.

**27. ¿Qué podría significar 409 al eliminar?**  
En un diseño con borrado físico podría ser un conflicto por relaciones o una regla de negocio. No debemos prometer ese código si nuestro contrato usa otro comportamiento.

**28. ¿Cómo se relaciona con Cliente 1:N?**  
Las reservas tienen una FK hacia cliente. Un DELETE físico podría romperla y ser rechazado. Nuestra baja lógica conserva la fila y por eso conserva también la relación.

**29. ¿Borrado físico y baja lógica?**  
Físico elimina la fila. Lógico conserva sus datos/id y cambia activo a false. Usamos el segundo para no perder historial y permitir reactivación.

**30. ¿Ventaja de actualizar estado local después del PUT?**  
La interfaz refleja la respuesta inmediatamente sin hacer un GET adicional ni recargar la página.

**31. ¿Ventaja de repetir GET después de una mutación?**  
Vuelve a sincronizar con el servidor, incluyendo cambios hechos por otros usuarios. Es útil cuando hay información que no podemos actualizar con seguridad localmente.

**32. ¿Cómo evitar doble PUT/DELETE?**  
Deshabilitamos botones mientras se espera y useSubmission tiene un ref que impide iniciar otra operación antes de terminar. El backend mantiene sus reglas aunque se llame fuera de la UI.

**33. ¿Qué revisar en Network?**  
URL, método, estado HTTP, Payload y Response. GET sin body; POST/PUT JSON; DELETE sin body y, en nuestro caso, 204 sin respuesta JSON.

**34. Explica el PUT completo.**  
Actualizar → validación/formulario → clienteService → apiClient/fetch → seguridad → Controller/DTO → Service transaccional → Repository/JPA/PostgreSQL → DTO JSON 200 → map actualiza estado → React vuelve a dibujar.

**35. Explica DELETE y qué pasa con las entidades hijas.**  
Eliminar → confirmar → service → DELETE /clientes/id → permisos → Controller → Service cambia activo=false dentro de la transacción → PostgreSQL guarda → 204 → React marca inactivo y lo oculta de Activos. Reservas e historial no se borran. Si falla, se muestra error y no se cambia falsamente la lista.

## Siguiente etapa

Esperar la guía 07 para adaptar el CRUD de la entidad hija a Reserva y sus reglas propias. No copiar cambios de propietario, DELETE o PUT de vehículos sin comprobar antes el contrato de reservas, sus estados y auditoría. Docker, GitHub e IA siguen aplazados.
