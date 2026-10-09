# Evidencias — guía 07

2026-10-08, America/La_Paz. Todo este recorrido usó datos ficticios, sin tocar la base habitual del usuario.

## Entorno y alcance

- Frontend entregado de WebStorm, Vite temporal `127.0.0.1:5174`.
- API temporal `127.0.0.1:18082`, PostgreSQL 17 separado en puerto 49691, base `gastro_guia7`, Flyway V1–V9.
- Dos clientes activos (Ana #1, Bruno #2), un cliente inactivo #3, mesa id=1/número=7, turno id=1, reservas iniciales #1/#2 de Ana y #3 de Bruno, fechas enero de 2030.
- Tokens y contraseñas no se incluyen en las evidencias. CORS del proceso temporal autoriza sólo el origen de prueba. No se modificó la configuración habitual.
- Logs/datos de prueba conservados en `C:/Users/antel/AppData/Local/Temp/gastro-guia07-ddba2f61f0b1462ebf38195293cb5da3`; no publicar esa carpeta. Los servicios temporales se detienen al terminar.

## Recorrido comprobado

| Acción | Resultado observado |
|---|---|
| Login admin y abrir Reservas | GET clientes/reservas 200; tres filas con nombres e ids. |
| Filtro Ana / Bruno / inactivo | 2 / 1 / 0 resultados; sin GET adicional en access log. |
| Nueva reserva | Selector sólo con activos; POST 201, nueva #4 para Ana el 18/01/2030. Total 4 y filtro Ana seleccionado. |
| Editar #1 y descartar | Precarga de Ana y nota original; cambiar y Cancelar edición no ejecuta PUT ni altera la fila. |
| Editar #1 y guardar | Bruno #2, nota «A nombre de Bruno», motivo; PUT 200. Sigue id=1, total=4, mismos fecha/mesa/personas. Filtro Bruno seleccionado. |
| Detalle/historial #1 | GET 200; CREACION y CORRECCION_CLIENTE, cliente #1→#2, nota anterior/nueva, actor y motivo. |
| Abrir cancelación y volver | No PATCH; reserva sigue SOLICITADA. |
| Confirmar cancelación #1 | PATCH 200, CANCELADA, total 4 y una cancelada conservada; editar/cancelar deshabilitados. |
| Conflicto de versión #2 | Formulario abierto en version=0; otro request de prueba aplica PUT version=0 → version=1. El formulario envía un cambio diferente con versión antigua → 409, conserva «Mi texto no debe perderse» y el motivo. |
| Cerrar y Actualizar agenda | GET 200 refleja «Editada por otra sesión de prueba», no el cambio rechazado; nueva #4 permanece. |
| Consola del navegador | Sin mensajes warn/error capturados al finalizar. |

El control nativo date del navegador de automatización necesitó confirmar la fecha con teclado (ArrowUp/ArrowDown) para emitir el cambio; el formulario controlado y su validación rechazaron correctamente una fecha vacía antes del POST. No se cambió código para saltar esa validación.

## Extracto de access log

Es registro del servidor de método/ruta/status, **no captura de DevTools Network**. OPTIONS corresponde al preflight de CORS y no modifica datos. Los pasos de fixture/simulación externa están separados del recorrido UI anterior.

```text
GET_/api/clientes_200
GET_/api/reservas_200
GET_/api/turnos_200
GET_/api/mesas_200
POST_/api/reservas/operativas_201
GET_/api/reservas/1_200
GET_/api/reservas/1_200
PUT_/api/reservas/1/datos-cliente_200
GET_/api/reservas/1_200
GET_/api/reservas/1/historial_200
PATCH_/api/reservas/1/estado_200
GET_/api/reservas/2_200
PUT_/api/reservas/2/datos-cliente_200
PUT_/api/reservas/2/datos-cliente_409
GET_/api/clientes_200
GET_/api/reservas_200
```

El primer PUT de #2 es la simulación externa de otra sesión; el segundo, el envío del formulario desactualizado. No se guardaron cuerpos/headers en el access log.

## Capturas de interfaz

- [Formulario de corrección](formulario-edicion.png).
- [Historial con ambos clientes](historial-correccion.png).
- [Cancelación y fila conservada](reserva-cancelada.png).
- [Conflicto de versión sin perder texto](conflicto-version.png).

## Verificaciones automáticas

- Frontend entregado: `npm.cmd test` **58 aprobadas**, `npm.cmd run lint` sin incidencias y `npm.cmd run build` correcto. Incluye contratos GET/POST/PUT/PATCH, version=0, ids, opciones activas, estado, filtro, Map y sincronización inmutable.
- Backend con JDK 21: verify H2 intermedio, 128 aprobadas. Después se añadió el caso de actualización V8→V9.
- Suite final completa: `scripts/verify-postgres.ps1 -MavenRepository C:/Users/antel/.m2/repository -Offline` → **129 pruebas, 0 fallos, 0 errores, 0 omitidas, BUILD SUCCESS**. Incluye actualización V5→V9 y V8→V9 conservando datos, roles/permisos, errores, propiedad e idempotencia. Los casos previos de concurrencia también se ejecutaron; no se añadió una carrera de dos hilos específica de corrección de cliente.
- Reportes de esa suite: `C:/Users/antel/AppData/Local/Temp/gastro-pg-test-3fc09e51a6f147098a54b33cadac42f5`. Su PostgreSQL se apagó al terminar el script.

No se afirma haber probado aquí todos los roles nuevamente desde UI ni todos los tamaños de pantalla: sus permisos nuevos sí están probados por HTTP y se conservaron estilos adaptables. La defensa oral, capturas Network y demostración personal se preparan con [guía 07](../../guia-07.md). Swagger UI no está instalado. La cancelación sustituye al DELETE del ejemplo por las reglas del dominio, y editar no reprograma reservas.
