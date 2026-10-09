# Guía 03 — Modelos TypeScript, mocks y tablas

Implementación adaptada a GastroReserva el 2026-10-08. La guía del profesor usa Cliente y Vehiculo; aquí son Cliente y Reserva. Un cliente puede tener varias reservas. Se conserva el layout y la navegación de la guía 02, sin llamar todavía al backend.

## Cómo verlo

En la terminal de WebStorm, dentro de gastroreserva1, ejecutar `npm.cmd run dev` y abrir la URL de Vite. Entrar a Clientes y Reservas desde el menú. Si ya hay un servidor funcionando no iniciar otro en el mismo puerto. Para verificar el proyecto usar `npm.cmd run lint` y `npm.cmd run build`.

## Archivos y responsabilidades

Dentro de `src/features/clientes` y `src/features/reservas`:

- `models/Cliente.ts` y `models/Reserva.ts`: interfaces, sin `any`, más la unión de estados de reserva.
- `data/clientes.mock.ts` y `data/reservas.mock.ts`: datos ficticios tipados.
- `components/ClienteTable.tsx` y `components/ReservaTable.tsx`: tablas que sólo reciben props.
- `pages/ClientesPage.tsx` y `pages/ReservasPage.tsx`: importan datos, calculan contadores y componen la pantalla.

Los archivos de `types` de la guía 01 reexportan los modelos para mantener compatibilidad; no hay dos definiciones distintas. Los componentes Card anteriores se conservan, pero las dos páginas ahora usan Table. Mesas no se amplía en esta guía.

## Correspondencia con GastroReserva

Cliente usa `id`, `nombre`, `email`, `telefono: string | null` y `activo`, campos de ClienteResponse. `nombre` ya contiene el nombre completo: no agregamos `apellido` ni `documento` que el DTO real no expone. No es necesario representar marcas temporales ni usuarioId para este listado.

Reserva es un modelo de vista plano, no una copia exacta de ReservaResponse. Para integrar la API habrá que transformar `respuesta.cliente.id` en `clienteId` y `respuesta.mesa.numero` en `mesaNumero`; el número visible de mesa no sustituye al id usado en las operaciones HTTP. El DTO contiene además turno, autor y fechas de auditoría que este listado aún no utiliza. No hay llamadas ni adaptador HTTP implementado todavía.

Los seis estados coinciden con RN-03. No inventamos un booleano activo para reservas: el contador «En curso» agrupa SOLICITADA, CONFIRMADA y SENTADA; «Terminadas» agrupa FINALIZADA, CANCELADA y NO_SHOW. La etiqueta «No asistió» representa NO_SHOW. Son conteos de ejemplos, no un reporte de ocupación real.

Fechas y horarios se conservan como cadenas ISO locales, sin convertirlas a UTC. El cliente inactivo de una reserva histórica no pierde su nombre: la búsqueda es por id, no por actividad. Los nulos se muestran como «Sin teléfono» o «Sin observaciones».

## Flujo que hay que entender

```text
clientes.mock.ts (Cliente[]) → ClientesPage (prepara datos y contadores)
→ ClienteTable (recibe props) → map (genera filas con key=id)
→ JSX → React actualiza el DOM → navegador muestra la tabla
```

ReservaTable recibe además el arreglo de clientes. `find()` busca por `clienteId` y muestra el nombre, o «Sin cliente asociado» si no encuentra coincidencia. TypeScript no comprueba claves foráneas entre arreglos; ése es el motivo del fallback. En el sistema real la integridad de la base y las reglas del backend siguen siendo la autoridad.

## Práctica obligatoria y evidencia

| Ejercicio adaptado | Resultado comprobado |
|---|---|
| Dos clientes nuevos | Valentina #5 activa y Mateo #6 inactivo: 6 clientes, 4 activos y 2 inactivos. |
| Tres reservas nuevas con clientes existentes | Reservas #5, #6 y #7. Los clientes #1 y #4 tienen más de una reserva, mostrando 1:N. |
| Una reserva con clienteId 999 | Reserva #8: «Sin cliente asociado». Ejemplo deliberadamente incompleto, no válido para enviarlo a la API. |
| Tipo temporal incorrecto | Se cambió cantidadPersonas de 4 a '4'; TypeScript rechazó la compilación. Después se restauró 4. |
| clientesMock vacío | Se sustituyó temporalmente por []; aparecieron el mensaje vacío y contadores 0/0/0, sin tabla. Datos restaurados. |
| Cambio en una sola fila | Se cambió temporalmente la observación de la reserva #1. Comparando las 8 filas antes/después, sólo cambió la primera. Restaurada. |
| Compilación final | `npm.cmd run build` y `npm.cmd run lint` correctos con todos los ejemplos restaurados. |

Captura textual del error intencional:

```text
src/features/reservas/data/reservas.mock.ts(8,5): error TS2322:
Type 'string' is not assignable to type 'number'.
```

Explicación: las comillas convierten 4 en texto. El modelo exige number. Esto es un error detectado antes de ejecutar, no una validación de reglas de negocio ni de respuestas HTTP.

También se comprobó ReservaTable con un arreglo vacío: mensaje de reservas vacías y contadores 0/0/0. Con clientes vacíos y las 8 reservas presentes, todas muestran el fallback sin fallar. La entrega conserva los 6 clientes y las 8 reservas originales.

Pruebas de navegador: 6 filas de clientes y 8 de reservas; estadísticas 6/4/2 y 8/5/3; Ana Rojas aparece en dos reservas; sólo #8 carece de cliente. Navegación por las cinco secciones, menú activo y recarga de /reservas correctos. No se observaron errores ni advertencias en la consola.

Responsive: escritorio de 1366 px y pantallas de 390/320 px, sin desbordamiento horizontal del documento. A menos de 900 px las estadísticas se apilan. Las tablas conservan un ancho mínimo (820/960 px) y se desplazan dentro de su contenedor. El contenedor recibe foco por teclado; una pulsación de flecha derecha desplazó la tabla 40 px. Los estados tienen texto, no dependen sólo del color; las tablas incluyen caption y encabezados scope.

Evidencias: [clientes en escritorio](evidencias/guia-03/clientes-escritorio.jpg), [reservas en escritorio](evidencias/guia-03/reservas-escritorio.jpg), [reservas en móvil](evidencias/guia-03/reservas-movil.jpg) y [clientes vacíos durante la prueba](evidencias/guia-03/clientes-vacio.jpg).

## Repaso de las 25 preguntas

1. **¿Entidad JPA e interface TS son lo mismo?** No. JPA participa en la persistencia del backend; la interface describe los datos que espera el frontend. Nos alineamos con los DTO públicos, no con los detalles internos de JPA.
2. **¿Para qué sirve Cliente[]?** Indica un arreglo de clientes. Evita que pongamos un texto o un objeto incompatible donde debe ir un cliente.
3. **¿Por qué separar mocks y página?** Para cambiar el origen de datos sin mezclarlo con la presentación de la pantalla.
4. **¿Qué significa import type?** Importa sólo una definición para el compilador; no agrega un valor JavaScript en ejecución.
5. **¿Qué recibe ClienteTable?** Una prop llamada clientes, de tipo Cliente[].
6. **¿Qué pasa si le mando un string?** TypeScript señala una incompatibilidad. No debemos ocultarla con any.
7. **¿Qué hace map()?** Recorre los clientes y devuelve una fila JSX por cada uno.
8. **¿Para qué sirve key?** Da a React una identidad estable para reconocer las filas entre renderizados.
9. **¿Por qué id y no índice?** El id sigue identificando al mismo registro aunque reordenemos, agreguemos o eliminemos elementos. El índice puede cambiar.
10. **¿Puedo leer cliente.key?** No por haber escrito key en el JSX. key es especial para React y no se pasa como prop normal. Nuestro objeto tiene id; si un componente necesita ese dato, se lo pasamos explícitamente.
11. **¿Qué hace el retorno temprano?** Si el arreglo está vacío, devuelve el mensaje y no construye una tabla sin registros.
12. **¿Para qué usamos filter()?** Para seleccionar los clientes activos y contar su cantidad. No modifica el arreglo original.
13. **¿Cómo se representa 1:N?** Cada reserva tiene clienteId apuntando al id de un cliente. Varias reservas pueden apuntar al mismo cliente.
14. **¿Puede repetirse clienteId?** Sí: un cliente puede volver al restaurante varias veces. Lo que no se repite es el id de cada reserva.
15. **¿Qué hace find() en ReservaTable?** Devuelve el primer cliente cuyo id coincide con el clienteId de la reserva.
16. **¿Y si no encuentra al cliente?** Devuelve undefined; el componente muestra «Sin cliente asociado».
17. **¿Por qué recibe dos arreglos?** Uno contiene las reservas y otro permite resolver los nombres de sus clientes.
18. **¿Página o tabla?** La página elige el origen y prepara estadísticas; la tabla representa lo recibido mediante props.
19. **¿Por qué evitar any?** Desactiva comprobaciones útiles y puede dejar errores que aparecerán recién en ejecución.
20. **¿La interface existe en PostgreSQL o en runtime?** No. Se elimina al producir JavaScript; no crea tablas ni objetos automáticamente.
21. **¿Qué es chequeo estático?** Revisar coherencia de tipos antes de ejecutar, al editar o compilar. No comprueba por sí solo datos externos ni reglas de negocio.
22. **¿Qué cambiará con el backend?** Un servicio obtendrá los DTO, los validaremos/adaptaremos y la página manejará carga, error y vacío. Las tablas podrán seguir recibiendo props.
23. **¿Qué dejará de ser el origen?** Los archivos *.mock.ts. Se pueden conservar para pruebas, pero no representar la información real de producción.
24. **¿Por qué table-responsive?** Encierra el desplazamiento horizontal de una tabla ancha para no ensanchar toda la página en un celular.
25. **¿Cuál es el flujo completo?** El mock exporta datos; la página los importa y pasa por props; la tabla usa map para crear JSX; React actualiza el DOM y el navegador lo muestra.

## Checklist de entrega

- [x] Cliente y Reserva en models, tipos coherentes y sin any.
- [x] Mocks tipados Cliente[] y Reserva[].
- [x] Tablas con props, map, key por id y relación con find.
- [x] Páginas con tablas y contadores, sin importación de mocks en las tablas.
- [x] Activo/inactivo para clientes y seis estados propios de reserva.
- [x] Vacíos, teléfono nullable y relación ausente controlados.
- [x] Responsive, teclado, build y lint comprobados.
- [x] Ejercicios temporales realizados y restaurados; no quedan tipos incorrectos.
- [ ] Practicar la defensa oral con el estudiante: las respuestas escritas no prueban dominio del tema.

No hay fetch, Axios, formularios, autenticación, persistencia local ni operaciones de negocio nuevas. La siguiente guía anuncia formularios con useState. RF-02, RF-05 y RF-08 avanzan sólo en representación visual; RN-01/RN-03 conservan sus estados y todas las reglas siguen siendo autoridad del backend.
