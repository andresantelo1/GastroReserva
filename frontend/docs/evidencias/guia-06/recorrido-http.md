# Guía 06 — evidencia del CRUD de clientes

Fecha: 2026-10-08. Datos enteramente ficticios, API temporal en 127.0.0.1:18081, Vite de verificación en 127.0.0.1:5174 y PostgreSQL 17 separado en loopback:51745. No se conectó esta prueba a la base habitual del usuario.

## Resultado

| Caso | Resultado observado |
|---|---|
| GET detalle al editar | 200; formulario precargado con el registro actual. |
| Cancelar edición | Cambios locales descartados, alta vacía; no se guardó el texto escrito. |
| POST cliente Ana CRUD | 201; id 3 del servidor incorporado a la tabla. |
| PUT con email de Otro cliente | 409; error visible y campos escritos conservados, sin cambio falso en la fila. |
| PUT válido | 200; Ana editada con el mismo id, teléfono actualizado y sin duplicar filas. |
| Cancelar baja | Confirmación cerrada sin DELETE. |
| DELETE cliente 1 con una reserva | 204 sin body; vista Activos oculta el perfil y aumenta Inactivos. |
| Relación Cliente 1:N Reserva | GET de reservas mantuvo la reserva 1 y cliente 1. El selector para una nueva reserva excluyó al inactivo. |
| Reactivación | PUT 200, mismo id y estado activo. |
| GET de fila ya inexistente | 404; no abrió un formulario con datos viejos. |
| DELETE de fila ya inexistente | 404; confirmación muestra el error, sin éxito ni eliminación local falsa. |
| Actualizar lista | GET sincronizó la fila obsoleta. |
| Cambiar activo mediante PUT | Al guardar se selecciona Inactivos/Activos automáticamente según el resultado; comprobado en cliente 2. |

Para generar el 404 auténtico se retiró únicamente el cliente ficticio 3 sin reservas de la **base temporal**, después de cargarlo en la UI. Se verificaron base/usuario, id/email y ausencia de hijos antes del DELETE de preparación. No se borró ningún dato real. Las bajas de la aplicación son lógicas; ese SQL de preparación no forma parte de la implementación.

## Capturas de interfaz

- [Formulario de edición, versión final](formulario-edicion.png).
- [Cliente actualizado](cliente-editado.png).
- [Baja lógica confirmada](baja-logica.png).
- [Reserva conservada](reserva-conservada.png).
- [Detalle inexistente 404](detalle-404.png).
- [Baja inexistente 404](baja-404.png).

Los archivos muestran distintos momentos del recorrido; por eso pueden variar el número de clientes y el estado visible. Son capturas de UI, **no de la pestaña DevTools Network**.

## Registro HTTP real

Extracto del access log temporal, configurado para guardar sólo método, ruta y status (sin Authorization, contraseñas ni cuerpos):

```text
GET_/api/clientes_200
GET_/api/clientes/1_200
POST_/api/clientes_201
GET_/api/clientes/3_200
PUT_/api/clientes/3_409
PUT_/api/clientes/3_200
DELETE_/api/clientes/1_204
GET_/api/reservas_200
GET_/api/clientes_200
GET_/api/turnos_200
GET_/api/mesas_200
GET_/api/clientes/1_200
PUT_/api/clientes/1_200
GET_/api/clientes/3_404
DELETE_/api/clientes/3_404
GET_/api/clientes_200
GET_/api/clientes/2_200
```

## Pruebas automáticas y límites

- Frontend: npm test, **44 aprobadas**; npm run lint y npm run build correctos.
- Backend: Maven verify con JDK 21, **118 pruebas, 0 fallos, 0 errores, 0 omitidas**, BUILD SUCCESS, con H2 efímera.
- No se ejecutaron las 118 pruebas completas en PostgreSQL; allí se hizo el recorrido HTTP anterior.
- Los procesos de API, Vite de prueba y PostgreSQL temporal se detienen al finalizar. Se conservan datos y logs en `C:/Users/antel/AppData/Local/Temp/gastro-guia06-bb846f1baf914c4db1f20d3a6a08e245` para revisión local; no contienen credenciales habituales del usuario.
- No se hizo una nueva prueba de carga ni se añadió control de conflictos de edición simultánea entre diferentes usuarios.
- Swagger UI no está instalado: contratos contrastados con código y HTTP. El estudiante debe completar las capturas Network y la defensa oral. El procedimiento y 35 respuestas están en [guia-06.md](../../guia-06.md).
