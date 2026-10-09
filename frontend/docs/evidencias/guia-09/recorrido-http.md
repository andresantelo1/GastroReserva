# Evidencia — guía 09

Fecha: 2026-10-09. UI real de React contra Spring Boot y PostgreSQL 17 **temporales**, en loopback. No se usó el PostgreSQL habitual del usuario ni se cambiaron sus credenciales, `.env`, dependencias o CORS permanentes.

Vite 5174 tomó la URL pública de prueba 18082 desde su proceso. La API autorizó ese origen exacto sólo durante esta ejecución. Se usaron 21 clientes ficticios, una mesa, un turno y seis reservas de febrero de 2030, creados para probar varias páginas; no se agregan estos datos a la aplicación entregada.

## Recorrido comprobado

| Acción en navegador | Resultado |
|---|---|
| Login administrativo y listado | POST login 200 y GET clientes 200. Tabla con 21 clientes activos. |
| Páginas de clientes, tamaño 10 | 1→2→3; última página 21–21 de 21, sólo cliente #8 según el orden alfabético existente. |
| Abrir baja de #8, Cancelar; reabrir y Escape | Sin DELETE. Foco inicial en Cancelar baja; Shift+Tab al cierre; Escape devuelve foco a Eliminar #8. |
| Confirmar baja de #8 | DELETE 204. 20 activos y 1 inactivo, total conservado 21. Vuelve automáticamente de página 3 a 2; foco de respaldo en Nuevo cliente porque desapareció el botón de esa fila. |
| Buscar «  ANA  » | 11 coincidencias, página 1, sin sensibilidad a mayúsculas ni espacios exteriores. Tamaño 5 da 3 páginas. |
| Combinar ANA con Inactivos | EmptyState, rango 0–0, página 1/1, Anterior/Siguiente deshabilitados. |
| Limpiar texto, editar cliente #8 y reactivar | GET detalle 200 y PUT 200; 21 activos, mismo id. La lista vuelve a página 1. |
| Cambiar tamaño a 20 | 1–20 de 21, página 1/2. |
| Crear Carla Guía 9 y buscar CARLA | POST 201, id #22 del servidor; una fila coincidente. |
| Listar seis reservas, tamaño 5, Siguiente | GET clientes/reservas 200; página 2 muestra sólo la sexta reserva. |
| Combinar texto « bruno », cliente #2, CANCELADA | Una reserva (#6), página 1/1. Cambiar controles no hace GET por fila. |
| Limpiar filtros | Recupera las seis reservas y la primera página. |
| Cancelar reserva #1 sin motivo | Error local; el diálogo permanece abierto y todavía no hay PATCH. |
| Completar motivo y confirmar | PATCH estado 200, misma fila #1 con CANCELADA; el total de reservas sigue en 6. |
| Consultar detalle de #1 | GET reserva e historial 200. Se ve CREACION y CAMBIO_ESTADO con motivo y responsable. |
| Corregir cliente de reserva #3 | GET detalle 200 y PUT datos-cliente 200. Mismo id y programación; el filtro pasa al cliente #2 y muestra cuatro reservas relacionadas. |
| Abrir cancelación de #2 a 390×844 y Escape | Diálogo cabe dentro del viewport, botones apilados, foco en Volver sin cancelar; no se envía PATCH. Ancho del documento 375 ≤ viewport 390. |

Los códigos proceden de los logs de acceso de la API y de las respuestas reflejadas en la UI. Esto no se presenta como una captura de DevTools Network. La cancelación final de reserva no es reversible; sólo se probó sobre la reserva ficticia aislada. No hubo eliminación física de reservas ni tablas.

## Pruebas automatizadas

- `npm.cmd test`: **79/79** aprobadas. Incluye 12 nuevas de búsqueda/filtros, relación, inmutabilidad, rangos y filtro antes de página; conserva las 67 de guías anteriores.
- `npm.cmd run lint`: correcto, sin advertencias.
- `npm.cmd run build`: TypeScript y Vite correctos, 152 módulos; URL de producción de ejemplo proporcionada sólo al proceso, sin modificar `.env`.
- `/tests/ui.html`: **8/8** en navegador con React/StrictMode; paginación, reducción y posterior crecimiento de lista, vacío, Button y diálogo nativo, foco, cancelación, cierre/reapertura, bloqueo/error y cleanup sin mutación.
- `/tests/hooks.html`: **12/12** de regresión aprobadas en esta ejecución; asincronía, cancelación y separación de mutaciones.
- Consola del recorrido de aplicación: sin errores ni warnings capturados.

Las 20 pruebas de navegador son distintas de las 79 Node; no se cuentan dentro de npm test. Los errores HTTP del transporte se ejercitan en pruebas unitarias/regresiones; en este recorrido no se inyectó un nuevo error HTTP de mutación real. No se reejecutó Maven, no hubo cambios Java/SQL.

## Capturas revisadas

- [Confirmación de baja y última página](01-confirmacion.png).
- [Búsqueda de clientes, mayúsculas y estado](02-busqueda-clientes.png).
- [Tres filtros y página resultante de reservas](03-filtros-reservas.png).
- [8 pruebas UI](04-pruebas-ui.png).
- [12 pruebas de hooks](05-pruebas-hooks.png).
- [Confirmación adaptable en móvil](06-modal-movil.png).

Los servicios de prueba se apagan al terminar. Datos/logs conservados en `C:/Users/antel/AppData/Local/Temp/gastro-guia07-17f400e96e44441998d94da3b5fa972c` (el prefijo viene del helper reutilizado; la instancia es de esta prueba). No se publicaron ni incluyeron secretos de acceso en el repositorio. Capturas Network y defensa oral del estudiante no se certifican como completadas.
