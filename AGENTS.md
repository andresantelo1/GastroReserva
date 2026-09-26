# Instrucciones del proyecto GastroReserva

Antes de modificar el proyecto, leer `README.md`, `docs/README.md` y `docs/PA-03-status.md`. Los documentos `docs/01-vision-context.md` a `docs/08-backlog-acceptance.md` contienen el alcance, actores, requisitos, reglas, modelo, arquitectura y backlog contractual.

## Stack obligatorio

- Java 21 y Spring Boot.
- PostgreSQL con migraciones Flyway.
- React + TypeScript para web.
- React Native + TypeScript para móvil.
- Docker y GitHub Actions.

No sustituir estas tecnologías ni ampliar el MVP hacia contabilidad, facturación fiscal o pagos.

## Trabajo incremental

1. Inspeccionar y preservar el estado existente antes de editar.
2. Relacionar cada incremento con RF, RN y criterios de aceptación.
3. Aplicar reglas en el backend, que es la autoridad final.
4. No exponer entidades JPA; usar DTO y contratos REST explícitos.
5. Agregar migración, permisos, pruebas de éxito/error y documentación cuando corresponda.
6. Ejecutar verificaciones relevantes y actualizar `docs/PA-03-status.md` sin marcar funciones futuras como completas.

## Reglas críticas

- RN-01 a RN-08 se definen en `docs/04-business-rules.md`.
- Preservar trazabilidad de estados y reasignaciones.
- Mantener secretos fuera del repositorio.
- Las migraciones aplicadas no se reescriben; se crea una nueva versión.
- La integración de IA futura debe tener puerto, timeout y fallback, y no bloquear el flujo principal.

## Verificación backend

Activar JDK 21 y ejecutar:

```powershell
.\mvnw.cmd verify
```

