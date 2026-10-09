# Guía 10 — evidencia de integración final

Fecha: 2026-10-09. Se usó el frontend **compilado**, servido por Vite preview en 127.0.0.1:5174, Spring Boot en 18082 y PostgreSQL 17 aislado en 64452. Datos ficticios; no se utilizaron contraseñas ni datos habituales. CORS autorizó sólo el origen de esta prueba. Al finalizar se apagaron estos servicios, conservando datos y logs. El build de entrega vuelve a apuntar al localhost:8080 habitual.

## Matriz F01–F12 adaptada

| Caso | Acción observada | Resultado |
|---|---|---|
| F01 | Inicio sin datos; después crear perfiles y reserva; finalmente cancelar | Vacío inicial correcto. Totales 12/11/1/1 antes y 12/11/1/0 después de cancelar; CANCELADA=1. Calculados de GET reales. |
| F02 | Crear Ana Final desde formulario | POST /clientes 201; id=1 asignado por PostgreSQL, tabla actualizada. |
| F03 | Guardar el alta vacía | Errores locales de nombre/email; no se realizó alta hasta completar el formulario. |
| F04 | Editar teléfono de #1 | PUT 200; teléfono 77777777 confirmado en SQL. |
| F05 | Nueva reserva de #1, 2030-03-01, turno/mesa activos, 2 personas | POST /reservas/operativas 201; reserva #1, cliente_id=1, SOLICITADA, version=0. |
| F06 | Corregir titular de la SOLICITADA a #2 con motivo | PUT /reservas/1/datos-cliente 200; mismo id, version=1; historial conserva referencias anterior/nueva. No se modifica fecha/mesa/personas. |
| F07 | Buscar bruno, combinar cliente #2 y SOLICITADA; cambiar a #1 | Una coincidencia y luego vacío correcto. Sin consultas por fila. |
| F08 | 12 clientes, ir a página 2 y buscar ` zoe ` | Zoe Final queda en página 1/1. Se filtra toda la lista antes de paginar; espacios exteriores ignorados. |
| F09 | Cancelar reserva #1 con motivo | PATCH /estado 200; CANCELADA, version=2, fila preservada; detalle muestra tres eventos. No es DELETE físico. |
| F10 | Dar de baja cliente #2 que tiene reserva | DELETE 204, activo=false, FK y reserva preservadas: adaptación de baja lógica del contrato. No corresponde inventar 409 por dependencia. |
| F11 | Entrar directamente y recargar /clientes y /reservas en preview | Se renderiza layout y acceso protegido, sin 404. Otra pestaña/recarga exige ingresar porque la sesión vive en memoria. |
| F12 | Apagar sólo Spring temporal y actualizar Inicio | Mensaje de conexión y Reintentar carga; sin métricas falsas ni pantalla blanca. Reintento fallido conserva mensaje. Recuperación tras error hacia vacío cubierta adicionalmente en Vitest. |

Adicional: editar email de #1 al de #3 produjo **409 real**, conservó el formulario y no alteró SQL: #1 siguió con ana.final@example.test y teléfono 77777777. No se mostró un éxito ficticio.

Datos de apoyo (zona, mesa, turno y 11 perfiles extra para paginar) se prepararon por API en la misma instancia temporal. El cliente #1, la reserva, sus ediciones y su cancelación se ejecutaron desde la UI. No se presenta la preparación HTTP como interacción de navegador.

## Comprobación independiente

SQL final: reserva id=1, cliente_id=2, cliente activo=false, estado=CANCELADA, version=2; tres filas de historial. Access log sin tokens/cuerpos confirma POST 201, PUT 200, DELETE 204, PATCH 200, GET detalle/historial 200 y PUT cliente 409.

El error de render se provocó **sólo en la página de prueba de Vite dev** `/tests/error-boundary.html`: apareció fallback; al retirar el fallo y reintentar reapareció el contenido. No se añadió una ruta rota al producto. Consola del recorrido normal sin errores/warnings antes de la prueba de desconexión; el harness registra su excepción intencional.

## Verificaciones automáticas

- 79 pruebas Node conservadas + 19 Vitest/Testing Library en cuatro archivos: todas aprobadas.
- Lint sin advertencias y build TypeScript/Vite correctos; `dist` probado con preview, no sólo con dev.
- `npm audit`: cero vulnerabilidades reportadas al verificar. Se actualizó únicamente source-map-js para resolver el aviso transitivo encontrado al instalar el runner; no se usó `audit fix --force`.
- Backend `mvnw verify`, Java 21: **129 pruebas, cero fallos/errores/omitidas y BUILD SUCCESS**, con H2 de pruebas. El recorrido anterior sí usó PostgreSQL real; no se afirma haber ejecutado esas 129 pruebas en PostgreSQL nuevamente en esta guía. Advertencia de compatibilidad de versión H2/Flyway sin fallo; no se cambió el stack.
- Se conservan las prácticas manuales anteriores de 8 UI y 12 hooks, pero no se cuentan como reejecutadas aquí.

## Capturas y límites

- [Dashboard antes de cancelación](01-dashboard.png).
- [Detalle e historial](02-historial.png).
- [Conflicto controlado](03-conflicto.png).
- [Dashboard final](04-dashboard-final.png).
- [Vista de escritorio adicional](05-desktop.png).
- [Fallback de render aislado](06-error-boundary.png).
- [Backend apagado](07-api-apagada.png).

Estas capturas son UI, no DevTools Network. La evidencia de métodos/status procede del log de acceso y la persistencia de SQL. El estudiante debe practicar Network y la defensa oral. El intento de emulación a 390 px no cambió el viewport de esta sesión (siguió en 1280): **no se certifica una nueva prueba visual móvil del dashboard**. Se incluyeron reglas CSS 4/2/1 columnas y se conserva la evidencia móvil histórica de la guía 09, sin sustituir esa comprobación pendiente. HTTPS, hosting público y recarga en el dominio final permanecen pendientes.
