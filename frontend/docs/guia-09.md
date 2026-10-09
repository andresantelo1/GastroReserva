# Guía 09 — componentes, búsqueda, filtros, paginación y modales

Fecha: 2026-10-09. Adaptación de la guía de ParkFlow360 a GastroReserva. Implementación en React + TypeScript, sobre los servicios reales y hooks de las guías 05–08. No modifica API, base de datos, permisos ni dependencias.

## Qué cambió y cómo verlo

Encender el backend habitual y ejecutar `npm.cmd run dev` en la carpeta del frontend. Iniciar sesión con una cuenta de GastroReserva ADMINISTRADOR/HOST; las credenciales de PostgreSQL no sirven para el login web.

1. En **Clientes**, escribir un nombre, email, teléfono o id; combinarlo con Mostrar: Activos/Inactivos/Todos.
2. Elegir 5, 10 o 20 filas por página. Anterior/Siguiente respetan los límites. Escribir otra búsqueda vuelve a la primera página.
3. Buscar algo inexistente: aparece una explicación del vacío, no una tabla sin contexto. Limpiar filtros permite recuperar la lista.
4. Eliminar abre un modal que identifica al cliente y explica la baja lógica. Escape o Cancelar no envían DELETE. Confirmar sí cambia la base configurada.
5. En **Reservas**, combinar texto, cliente y estado. El texto busca id, nombre del cliente, número de mesa, fecha ISO/visible y observaciones.
6. Cancelar reserva abre el mismo patrón de confirmación, pero exige motivo y conserva la reserva/historial. No permite reactivar CANCELADA.

Para probar sin cambiar datos, usar búsquedas, filtros, páginas y abrir/cerrar confirmaciones; no pulsar su confirmación final. La captura de las pruebas usa datos ficticios en otra instancia, no datos precargados en tu base habitual.

## Adaptaciones al contrato

| Guía ParkFlow360 | GastroReserva |
|---|---|
| Cliente: nombre, apellido, documento, email | Nombre completo, email, teléfono e id. No existen campos separados apellido/documento en el DTO actual; no se inventaron. |
| Vehículo: placa, marca, modelo y propietario | Reserva: id, mesa, fecha, observaciones y cliente relacionado. |
| Propietario 1:N vehículos | Cliente 1:N reservas, usando clienteId numérico. |
| Filtro activo/inactivo del hijo | Estados reales: SOLICITADA, CONFIRMADA, SENTADA, FINALIZADA, CANCELADA y NO_SHOW. Reserva no tiene activo. |
| Borrar vehículo | Cancelar reserva mediante PATCH; no hay eliminación física. Cliente conserva su DELETE de baja lógica. |
| window.confirm | Se reemplazaron las confirmaciones inline previas por ConfirmDialog; no se introdujo window.confirm. |

Los permisos y las reglas RN-01/RN-02/RN-03/RN-08 siguen en Spring. RF-02 y RNF-09 se amplían con búsqueda, filtros, paginación, mensajes y teclado. RF-05/HU-06 y RF-08/HU-15 conservan el CRUD de clientes y la gestión **limitada** de reservas de la guía 07. No se agrega reprogramación, check-in, autoservicio ni otros estados operativos desde esta pantalla.

## Archivos y responsabilidades

Los ocho componentes pedidos están en `src/components/ui`:

| Componente | Responsabilidad |
|---|---|
| Button | Variantes primary/secondary/danger/ghost, atributos nativos tipados, loading y disabled. Por defecto type=button; un envío declara type=submit. |
| SearchInput | Entrada controlada de búsqueda, label visible y callback. No conoce clientes. |
| SelectFilter | Selección controlada de opciones value/label, sin conocimiento de negocio. |
| Pagination | Muestra rango, página y controles 5/10/20; comunica cambios. No hace fetch. |
| Modal | Sincroniza open de React con dialog.showModal/close; nombre accesible, Escape, foco inicial y restauración. |
| ConfirmDialog | Compone Modal, mensaje, botones, error, bloqueo durante envío y contenido opcional. No hace DELETE/PATCH. |
| EmptyState | Explica que no hay resultados y cómo continuar. |
| StatusBadge | Texto y tono visual; el dominio decide etiqueta/tono. |

`hooks/usePagination.ts` guarda solamente página/tamaño y corrige límites. `utils/pagination.ts` calcula el rango y `utils/search.ts` normaliza el texto. `features/clientes/utils/clienteSearch.ts` y `features/reservas/utils/reservaSearch.ts` contienen las condiciones específicas. Las páginas coordinan filtros, selección pendiente y operaciones; los servicios mantienen HTTP. `styles/tokens.css` centraliza colores, espacios, radios, foco y sombras, y `styles/ui.css` aplica estos tokens sin sustituir el diseño previo.

Ningún componente UI importa servicios ni modelos de Cliente/Reserva. Las tablas reciben sólo las filas visibles y usan key=id. MESERO conserva la lectura permitida: resuelve el nombre proyectado en la reserva sin pedir el catálogo protegido de clientes.

## Estado y flujo de datos

Estado fuente: lista recibida de la API, búsqueda, filtros, página, tamaño y selección de formulario/confirmación. Estados asíncronos separados: GET, guardado y baja/cancelación. No se guardan otra vez las listas filtradas o paginadas.

```text
lista de la API → búsqueda + filtros → total de resultados → página válida → slice → tabla
```

Dentro del texto se usa OR: basta coincidir con un campo. Entre texto/cliente/estado se usa AND: deben cumplirse todos. Se normalizan ambos textos con trim y minúsculas en español; no se promete búsqueda sin tildes. Se construye un Map de clientes para resolver la relación, sin consultas HTTP por fila ni persistir un segundo nombre en el modelo.

Para estas listas pequeñas se calcula directamente en cada render. `useMemo` es opcional y no mejora la corrección: se incorporaría después de medir un coste significativo, con dependencias completas.

```ts
totalPages = Math.max(1, Math.ceil(total / pageSize))
page = Math.min(Math.max(requestedPage, 1), totalPages)
start = (page - 1) * pageSize
end = Math.min(start + pageSize, total)
visible = filtered.slice(start, end)
```

Con cero resultados el rango es 0–0, página 1 de 1 y ambos botones deshabilitados. Cambiar texto, filtro o tamaño reinicia la página desde el handler. Si una baja deja la página fuera de rango, usePagination ajusta **condicionalmente** la página solicitada al nuevo límite; no hay un setState incondicional ni un bucle. Esto evita regresar a una página antigua si después crece la lista.

Guardar usa la respuesta del servidor: actualiza por id o incorpora el nuevo registro. Reinicia búsqueda/página y selecciona el estado/cliente guardado cuando corresponde. Con muchos registros el recién creado puede quedar en otra página; se encuentra con su nombre o id, sin duplicarlo artificialmente al principio.

## Confirmación y accesibilidad

La entidad pendiente identifica exactamente qué acción se está confirmando. Abrir el modal no escribe en la base. Confirmar llama al handler del dominio; sólo el éxito actualiza la lista y cierra. Un error conserva el modal y su mensaje. Cancelar/volver/Escape antes del envío descartan la selección sin HTTP.

`showModal()` abre un diálogo modal nativo y deja el fondo inerte. El foco inicial va al botón seguro de cancelar/volver; Tab y Shift+Tab se mantienen en el diálogo. El botón × tiene aria-label=Cerrar. Al cerrar, el foco vuelve al disparador; si desapareció por la baja, va a Nuevo cliente/Nueva reserva. Mientras se confirma, Escape y los cierres quedan bloqueados para no fingir que una escritura en curso se deshizo. Abortar HTTP no garantiza rollback.

Se conserva StrictMode. La limpieza del efecto cierra el diálogo sin disparar una acción de negocio; montarlo o desmontarlo nunca elimina ni cancela una entidad. Labels visibles, foco visible, badges con texto y botones de carga/deshabilitados no dependen solamente del color. En móvil los controles se apilan y las tablas mantienen su desplazamiento horizontal dentro del contenedor.

## Paginación local y futura paginación del servidor

Hoy GET `/api/clientes` y GET `/api/reservas` devuelven listas. Buscar/cambiar página trabaja sobre la lista ya cargada: no envía nuevas peticiones. Es adecuado para el tamaño actual, no una solución de rendimiento para miles de filas.

Una migración futura debe definir filtros/orden/página/tamaño en backend y una respuesta con contenido y totales. Ejemplo **propuesto, no implementado**: `GET /api/clientes?search=ana&page=0&size=10` y `{ content, totalElements, totalPages, number, size }`. También exige adaptar DTO/service, validar el contrato, ordenar de forma estable y abortar respuestas obsoletas. No basta con enviar parámetros que la API actual no entiende. Pagination podría conservarse: recibiría los totales reales y sus callbacks cambiarían la consulta.

## Verificación y checklist

- [x] Ocho componentes genéricos, sin dependencias de servicios del dominio.
- [x] Búsqueda y filtros combinados en Clientes y Reservas, adaptados al modelo real.
- [x] Páginas 5/10/20, límites, vacío, reinicio y corrección tras baja de última fila.
- [x] Resultados derivados, sin duplicarlos en useState.
- [x] Confirmaciones modales; teclado/Escape/foco y envío bloqueado.
- [x] Loading/error/saving/deleting conservados; mutaciones sólo por eventos.
- [x] Baja/reactivación y alta de cliente; corrección/cancelación/detalle de reserva comprobados contra API aislada.
- [x] Estilos con tokens y modal comprobado a 390 px.
- [x] 79 pruebas Node, 8 pruebas de UI React y 12 de hooks React aprobadas; lint y build correctos.
- [ ] Defensa oral: leer/entender estas respuestas y practicar sin el código. No puede certificarse automáticamente.

Las pruebas de UI y hooks son suites separadas: con Vite encendido, abrir `/tests/ui.html` o `/tests/hooks.html` y pulsar Ejecutar. No requieren API, no escriben en PostgreSQL, no se ejecutan con npm test ni se incluyen en el build de la aplicación. Los 79 casos Node sí se ejecutan con `npm.cmd test`.

[Recorrido, límites y capturas](evidencias/guia-09/recorrido-http.md). No se reejecutó Maven en esta guía sin cambios Java/SQL; las 129 pruebas PostgreSQL de guía 07 siguen siendo evidencia histórica, no una ejecución nueva. Guía 10 pendiente de recibir.

## 40 preguntas y respuestas para defender

1. **¿Qué diferencia existe entre un componente UI genérico y uno de dominio?**
   El genérico sirve en distintas pantallas, como Button. El de dominio entiende algo del negocio, como ReservaTable, que muestra mesas, clientes y estados de reserva.

2. **¿Por qué Button no debe importar clienteService?**
   Porque su trabajo es mostrar un botón y comunicar el clic. La página decide si ese clic guarda un cliente, abre un modal o cambia de página.

3. **¿Qué significa que SearchInput sea controlado?**
   Su texto viene de una prop value. Cuando escribo, avisa con onChange y la página actualiza el estado; el componente no mantiene otra copia del texto.

4. **¿Por qué filteredClientes no debería guardarse normalmente en useState?**
   Porque se obtiene de clientes, búsqueda y filtro. Podemos calcularlo y evitar un estado adicional que habría que sincronizar.

5. **¿Qué problema crea mantener clientes y filteredClientes como dos estados independientes?**
   Podría editar o dar de baja un cliente en una lista y olvidarme de actualizar la otra. La pantalla mostraría información vieja o contradictoria.

6. **¿En qué orden debe ejecutarse búsqueda, filtrado y paginación?**
   Primero buscar y aplicar todos los filtros a la lista completa; después contar los resultados y recortar la página que se muestra.

7. **¿Por qué es incorrecto paginar primero y filtrar después?**
   Porque buscaría sólo dentro de esa página y perdería coincidencias de las demás. Podría mostrar vacío aunque el cliente exista.

8. **¿Cómo se construye una búsqueda case-insensitive?**
   Convierto tanto lo escrito como los campos a minúsculas y comparo con includes. Así ANA y ana coinciden.

9. **¿Por qué trim() es útil en una búsqueda?**
   Quita espacios accidentales al principio y al final: escribir « ana » permite encontrar Ana. No elimina espacios internos del nombre.

10. **¿Qué diferencia hay entre OR dentro de la búsqueda y AND entre filtros?**
    OR permite coincidir con nombre o email o teléfono. AND exige además el estado elegido. En reservas deben coincidir texto, cliente y estado cuando están seleccionados.

11. **¿Qué hace useMemo?**
    Conserva el resultado de un cálculo entre renders mientras no cambien sus dependencias. Puede evitar repetir un cálculo costoso.

12. **¿Por qué useMemo no debe ser requisito para que la lógica funcione?**
    Porque es una optimización. Quitar useMemo puede hacer que se recalcule más, pero no debería cambiar los resultados correctos.

13. **¿Cuáles son las dependencias correctas de un useMemo que filtra clientes?**
    La lista clientes, el texto search y el filtro de actividad. Si el cálculo lee otro valor reactivo, también debe incluirlo. Nuestra versión deriva directamente sin useMemo.

14. **¿Cómo se calcula totalPages?**
    Se divide la cantidad de resultados entre el tamaño y se redondea hacia arriba: Math.ceil(total / pageSize). Aquí usamos un mínimo de 1 para representar también la lista vacía.

15. **¿Cómo se calcula el índice inicial de una página?**
    Con (page - 1) * pageSize. Para la página 2 de tamaño 10 el índice inicial es 10, porque los índices empiezan en cero.

16. **¿Qué hace slice(start, end)?**
    Crea un arreglo con los elementos desde start incluido hasta end excluido. No modifica la lista original.

17. **¿Por qué page debe volver a 1 cuando cambia search?**
    Porque la búsqueda nueva puede tener menos páginas. Además, lo esperado es empezar a ver sus primeras coincidencias.

18. **¿Qué debe ocurrir si se elimina el único registro de la última página?**
    Se recalcula el total y se pasa a la última página que todavía existe. Si no queda ningún resultado, se muestra el vacío en página 1.

19. **¿Qué responsabilidad tiene Pagination?**
    Mostrar página, rango y controles; deshabilitar límites y avisar cuando el usuario cambia página o tamaño. No conoce qué entidad se lista.

20. **¿Por qué Pagination no debe hacer fetch directamente?**
    Para poder reutilizarlo en clientes, reservas y listas locales. El acceso a datos pertenece a la página, hooks y servicios.

21. **¿Por qué usamos un id estable como key en filas y opciones?**
    Para que React reconozca cada registro aunque cambie de posición al filtrar. El índice de una fila puede cambiar; su identidad no.

22. **¿Qué ventaja ofrece EmptyState frente a una tabla simplemente vacía?**
    Explica qué pasó y cómo continuar, por ejemplo cambiar los filtros. Una tabla vacía no aclara si está cargando, falló o no tiene coincidencias.

23. **¿Por qué StatusBadge debe incluir texto y no depender solamente del color?**
    Porque no todos perciben igual los colores. Leer «Cancelada» permite entender el estado aunque no se distinga el rojo.

24. **¿Qué problema de UX resuelve un ConfirmDialog?**
    Evita ejecutar una acción importante por un clic accidental. Muestra sobre qué registro actuará y sus consecuencias antes de confirmar.

25. **¿Por qué pendingDelete debe guardar la entidad o su id antes de ejecutar DELETE?**
    Para saber cuál se está confirmando, mostrar su nombre y enviar exactamente ese id al aceptar. Abrir la confirmación todavía no elimina nada.

26. **¿Qué hace showModal() sobre un elemento dialog?**
    Lo abre como modal en la capa superior del navegador, con fondo inerte y manejo nativo de foco/teclado. No es sólo mostrar una caja.

27. **¿Qué diferencia conceptual hay entre open y showModal()?**
    El atributo open muestra un diálogo sin convertirlo por sí solo en modal. showModal() activa el comportamiento modal. Nuestra prop open de React se sincroniza con ese método mediante un efecto.

28. **¿Qué debería ocurrir al presionar Escape en un modal?**
    Antes de enviar, cerrar como Cancelar, sin ejecutar la acción, y restaurar el foco. Durante la escritura bloqueamos el cierre para no dar a entender que se canceló una operación ya enviada.

29. **¿Por qué un botón de sólo icono necesita aria-label?**
    Porque un lector de pantalla necesita un nombre que explique la acción. El símbolo × lleva el nombre «Cerrar».

30. **¿Qué son design tokens?**
    Son valores compartidos del diseño: colores, espacios, radios, sombras y foco. Permiten mantener una apariencia consistente.

31. **¿Qué ventaja tienen las variables CSS frente a copiar colores y espacios?**
    Cambiamos el valor en un solo lugar y todos los componentes que lo usan se actualizan. Evitamos pequeñas diferencias accidentales.

32. **¿Cómo se filtra Vehículo por clienteId si el select devuelve string? En nuestro proyecto, Reserva.**
    La opción vacía significa todos. Si hay un id, lo convierto con Number y lo comparo con reserva.clienteId, que es numérico.

33. **¿Cómo buscar un Vehículo por propietario sin duplicar su nombre? En nuestro proyecto, Reserva por cliente.**
    Uso reserva.clienteId para resolver el cliente en la lista cargada y comparar su nombre. La reserva no necesita guardar otro nombre editable. MESERO usa la proyección que ya entrega su API autorizada.

34. **¿Qué ventaja tiene clientesById Map para resolver propietarios/clientes?**
    Se construye una vez para esa derivación y permite acceder por id, evitando recorrer clientes con find por cada reserva. Tampoco se hace una petición por fila.

35. **¿Cuándo es adecuada la paginación cliente?**
    Cuando la lista completa es pequeña y razonable de descargar. Después, navegar y filtrar es rápido porque trabaja con los datos ya recibidos.

36. **¿Cuándo debe migrarse la paginación al backend?**
    Cuando traer todos los registros cuesta demasiada red, memoria o tiempo, o se necesita búsqueda global actualizada sobre muchos datos.

37. **¿Cómo cambiaría GET /clientes para paginación servidor?**
    Tendría un contrato de filtros, orden, página y tamaño; devolvería sólo esa página junto con totales. Habría que implementarlo en Spring y adaptar el service; hoy devuelve un arreglo, no esa respuesta paginada.

38. **¿Por qué una UI Pagination puede reutilizarse con paginación cliente o servidor?**
    Porque sólo recibe números y callbacks. No le importa si las filas salieron de slice o de una respuesta de la API.

39. **¿Qué significa single source of truth aplicado a filtros y resultados?**
    Tener una sola fuente para cada dato: la lista y los criterios guardados en estado. Los resultados se calculan desde ellos, no se mantienen como otra lista independiente.

40. **Explique el flujo desde escribir «ana» hasta ver sólo coincidencias.**
    SearchInput avisa el nuevo texto; la página guarda search y vuelve a la página 1. Normaliza el texto, busca coincidencias, aplica los otros filtros, calcula el total, obtiene la página con slice y pasa esas filas a la tabla. No hace otro GET en esta paginación local.

## Referencias técnicas

Comportamiento modal: [MDN dialog](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/dialog). Memoización opcional: [React useMemo](https://react.dev/reference/react/useMemo). Fuente del ejercicio: guía 09 DOCX entregada por el docente; las adaptaciones de dominio anteriores evitan agregar vehículos/campos ajenos al contrato.
