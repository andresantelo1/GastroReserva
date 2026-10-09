# Guía 04 — Formularios React + TypeScript

Adaptación de `ParkFlow360_Guia_04_Estudiante_Formularios_React_TS.docx` a GastroReserva. Verificada el 2026-10-08. Alcance: formularios controlados y validación local; **sin HTTP, CRUD, inserción en tablas ni persistencia**. No hace falta encender Spring Boot.

## Qué probar

1. Abrir `/clientes` y pulsar **+ Nuevo cliente**.
2. Pulsar Guardar cliente vacío: aparecen 4 errores (nombre, email, documento y dirección).
3. Usar datos inventados: nombre `Ana de prueba`, email `ANA@EXAMPLE.COM`, teléfono `70010009`, documento `DEMO04`, dirección `Calle de ejemplo 123`. Marcar o desmarcar Cliente activo.
4. Guardar: durante 500 ms los campos y botones se bloquean. Luego se muestra el objeto compatible con el backend y, por separado, los datos del ejercicio. El email queda en minúsculas; el listado sigue con 6 clientes.
5. Cambiar un campo elimina el resultado anterior. **Limpiar** borra campos, errores y resultado, y vuelve a marcar el checkbox. Cerrar también descarta los datos.
6. En `/reservas`, pulsar **+ Nueva reserva**. Guardar vacío muestra 6 errores. El selector sólo ofrece los cuatro clientes activos.
7. Elegir Ana, una fecha válida con el calendario, Almuerzo, Mesa 2 y cantidad 5: debe rechazar el exceso de capacidad. Cantidad 1.5 tampoco se admite.
8. Cambiar a 4 personas y escribir referencia `visita-ana`. Guardar prepara IDs/cantidad numéricos; `mesaId` es **3**, porque número de mesa e identificador no son lo mismo.
9. Expandir el resultado del ejercicio: la referencia aparece `VISITA-ANA`; el input conserva el texto escrito. El listado sigue con 8 reservas.
10. Probar Limpiar, cerrar/reabrir y pantalla angosta. No se guardan datos al recargar ni al salir de la página.

El contador refleja los errores detectados al enviar. Al editar un campo se quita su error anterior, pero el formulario completo se valida nuevamente al pulsar Guardar; que el contador llegue a cero mientras se escribe no prueba que todos los datos sean válidos.

## Adaptación sin cambiar el contrato

| Ejercicio de ParkFlow360 | GastroReserva |
|---|---|
| Cliente y vehículo; relación 1:N | Cliente y reserva; relación 1:N por clienteId. |
| Nombre y apellido | Nombre completo, como el contrato existente. |
| Año numérico escrito como string | Cantidad de personas escrita como string, validada como entero positivo antes de convertir. |
| Select de cliente | Muestra el nombre, guarda el id en el estado y lo convierte a número en el objeto de envío. |
| Placa en mayúsculas | Referencia ficticia local normalizada al preparar el resultado; no se agrega una placa al dominio. |
| Documento, dirección y checkbox activo | Sección de práctica local identificada; no forman parte de ClienteCreateRequest. |

El documento de práctica exige 5–30 caracteres; dirección es obligatoria y tiene máximo 200. El checkbox permite practicar `checked`, pero **POST /api/clientes crea siempre un cliente activo**. No permite registrar usuarios ni contraseñas. La referencia local es obligatoria en esta práctica y tiene máximo 40 caracteres. Estas exigencias son académicas, no nuevas reglas del negocio GastroReserva.

Objetos preparados, siempre por mapeo explícito:

```ts
// ClienteCreatePayload: compatible con ClienteCreateRequest
{ nombre: string, email: string, telefono: string | null }

// ReservaOperativaPayload: compatible con ReservaOperativaCreateRequest
{ clienteId: number, fecha: string, turnoId: number,
  mesaId: number, cantidadPersonas: number, observaciones: string | null }
```

No se usa `...formData` para armar el objeto de la API: eso filtraría campos del ejercicio y enviaría strings donde el backend espera números. No se agrega `id`, `estado` ni marcas de auditoría, que pertenecen al servidor. La reserva propia del cliente usa otro contrato sin `clienteId`; este formulario practica el alta operativa de administrador/host, sin implementar todavía su sesión web.

## Checklist técnico y práctica evaluada

- [x] useState tipado como fuente del valor de inputs/select/textarea; checkbox con checked.
- [x] ChangeEvent y FormEvent tipados; preventDefault; actualización inmutable con spread y nombres de campos verificados.
- [x] Types de formulario distintos de modelos de lectura y del objeto de envío.
- [x] Validación en utils, errores por campo, aria-invalid y asociación del mensaje al input.
- [x] noValidate para mostrar mensajes manuales consistentes.
- [x] Email, nombre obligatorio, teléfono opcional con mínimo 7 dígitos si se proporciona y límites de longitud.
- [x] Fecha real, IDs positivos existentes/activos, cantidad entera positiva y capacidad individual del catálogo de ejemplo.
- [x] Documento mínimo 5 y dirección obligatoria como práctica local diferenciada.
- [x] Normalización en mayúsculas al preparar, sin cambiar el texto mientras se escribe.
- [x] Selector de clientes activos y mensaje si no hay opciones activas.
- [x] Limpiar ambos formularios y mostrar/ocultar desde cada página.
- [x] Espera de 500 ms, botón deshabilitado, bloqueo de duplicados y cancelación del timer al desmontar.
- [x] Cantidad de errores visibles al enviar; éxito explícitamente simulado.
- [x] Sin inserción en arreglos, localStorage ni API; impresión de objeto en consola sólo en desarrollo.
- [x] Layout de dos columnas en escritorio y una en pantalla angosta.
- [x] Explicación de por qué validar en React no sustituye al backend.

Archivos principales: `features/clientes/components/ClienteForm.tsx`, `features/reservas/components/ReservaForm.tsx`, sus carpetas `types`/`utils`, `components/common/FormField.tsx` y `hooks/useEnvioSimulado.ts`. Los catálogos mock se importan en la página de reservas y llegan al formulario mediante props. Las tablas de la guía 03 y sus datos se conservaron.

## Verificaciones realizadas

```powershell
npm.cmd run build
npm.cmd run lint
npm.cmd test
```

Resultado: compilación y lint correctos; **15 pruebas, 15 aprobadas, 0 fallos**. Sin nuevas dependencias; runner de Node con soporte nativo de TypeScript, comprobado en Node 26.10.0.

Pruebas unitarias: formularios vacíos/espacios, email y longitud, teléfono opcional/inválido, documento/dirección, fecha inexistente y bisiesta, IDs inexistentes/inactivos, mesa/zona/turno inactivos, cantidad cero/negativa/decimal/exponencial/fuera de capacidad, observaciones opcionales/límites, transformación del objeto, normalización local y preservación de mocks. Son pruebas de funciones puras, no una suite automatizada de componentes React.

Pruebas de navegador realizadas: errores de ambos formularios, éxito sin navegación, checkbox booleano, bloqueo en ambos durante la espera, Limpiar, resultado retirado al editar, cuatro clientes activos en el selector, ID de mesa distinto de su número visible, referencia normalizada sólo al preparar, 6/8 filas sin inserciones y desmontaje durante la espera sin resultado tardío. A 390 y 320 px ambos formularios tienen una columna y el documento no desborda; las tablas conservan su desplazamiento interno. No se observaron errores ni advertencias de consola en este recorrido. Fecha comprobada usando el control de calendario del navegador.

Evidencias: [formulario de cliente](evidencias/guia-04/clientes-formulario.png), [errores de cliente](evidencias/guia-04/clientes-validaciones.png) y [reserva en móvil](evidencias/guia-04/reservas-movil.png).

## Por qué React no reemplaza a Spring Boot

La validación frontend ayuda a corregir errores antes de enviar, pero cualquiera puede omitirla y llamar directamente a la API. Los datos también pueden cambiar entre consultar y guardar. Spring Boot debe verificar de nuevo campos, autenticación, permisos, existencia/actividad, capacidad de mesa y turno, solapamientos y estados dentro de la operación transaccional. La comprobación local de capacidad de esta guía no garantiza disponibilidad ni aplica todas las reglas RN-01 a RN-03.

Trazabilidad parcial: RF-03 (validación visual), RF-05 (preparación de cliente), RF-08 (preparación de reserva) y RNF-09 (formularios utilizables y mensajes). No se marcan estos requisitos como terminados de punta a punta por tener una demostración sin HTTP.

## 25 respuestas sencillas para defender la guía

1. **¿Qué es un formulario controlado?** Uno donde React guarda los valores y se los pasa a los campos; cada cambio actualiza ese estado.
2. **¿Cuál es la fuente de verdad?** El estado `formData`, no una variable suelta ni consultar los inputs al final.
3. **¿Para qué usamos useState?** Para recordar datos entre renderizados y actualizar lo que se ve cuando cambian.
4. **¿Por qué no hacer formData.nombre = algo?** Porque muta el objeto existente y no notifica correctamente una actualización a React. Usamos el setter con un objeto nuevo.
5. **¿Qué hace ...prev?** Copia los campos anteriores para conservarlos al cambiar sólo uno.
6. **¿Para qué sirve name?** Identifica qué propiedad corresponde al campo que cambió.
7. **¿Qué significa [name]?** Es una clave calculada: actualiza la propiedad cuyo nombre llegó del input, no una propiedad literal llamada name.
8. **¿Value y checked son iguales?** No. Value guarda el texto; checked indica si un checkbox está marcado, como booleano.
9. **¿Qué es ChangeEvent?** El tipo TypeScript del evento de cambio. Aquí identificamos si proviene de input, select o textarea.
10. **¿Qué es FormEvent?** El tipo del evento del formulario, utilizado por el manejador de submit.
11. **¿Qué hace preventDefault?** Impide el envío HTML tradicional y la recarga; nuestro código controla lo que sucede.
12. **¿Qué pasa con value sin onChange?** El input controlado queda sin una forma de actualizar su valor y React puede advertirlo; no se escribe normalmente en él.
13. **¿Por qué la cantidad empieza como string?** Porque el input entrega texto y debe poder estar vacío mientras se edita.
14. **¿Cuándo la convertimos a number?** Después de validar que hay un entero positivo válido, al preparar el objeto de envío.
15. **¿Por qué no basta con Number()?** Porque Number('') da cero y acepta entradas que nuestra regla no permite; además hay que comprobar rango y formato.
16. **¿Qué relación representa el select de clientes?** Una reserva pertenece a un cliente y un cliente puede tener muchas reservas: 1:N.
17. **¿Por qué mostrar nombre pero usar id como value?** Para que la persona vea algo comprensible y el sistema identifique al registro sin depender de nombres repetidos.
18. **¿Para qué sirve Object.keys(errors).length?** Cuenta cuántos campos tienen errores y permite impedir el envío cuando hay al menos uno.
19. **¿Por qué errors también es estado?** Para que React vuelva a mostrar la interfaz con los mensajes actualizados.
20. **¿Por qué agrupar valores en un objeto?** Porque pertenecen al mismo formulario y facilita actualizar, validar y limpiar todo de manera consistente.
21. **¿Qué riesgo tiene un handleChange genérico?** Usar un name incorrecto o tratar un checkbox como texto. Verificamos las claves y usamos checked para el booleano.
22. **¿Por qué validar también en el backend?** Porque el navegador se puede saltar o manipular y las reglas deben cumplirse incluso con usuarios concurrentes.
23. **¿Modelo y FormData son lo mismo?** No. El modelo representa datos del negocio; FormData permite estados temporales como IDs vacíos en string y campos didácticos locales.
24. **¿Qué es payload?** El objeto listo para enviar. Se construye explícitamente con los campos esperados, textos normalizados y tipos correctos; aquí sólo se muestra, no se envía.
25. **¿Cuál es el flujo?** Escribir → onChange → actualizar estado → React muestra el valor → Guardar → preventDefault → validar → preparar objeto → esperar 500 ms → mostrar resultado. Todavía no hay petición HTTP.

## Límite de la entrega y siguiente guía

Las guías técnicas 01–04 están implementadas; la defensa oral corresponde practicarla al estudiante. No significa 40% del esfuerzo total del proyecto ni frontend completo. Antes de conectar la API se deben retirar o separar los ejercicios locales obligatorios que no pertenecen al dominio, obtener catálogos reales, agregar sesión/permisos y manejar carga/errores/respuestas del servidor. No anticipamos el contenido concreto de las guías 05–10 aún no recibidas. Backend, PostgreSQL, móvil, Docker, GitHub e IA no se modificaron aquí.
