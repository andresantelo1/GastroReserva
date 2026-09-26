# GastroReserva — documentación del proyecto

Esta carpeta reúne los entregables académicos y técnicos del PA-03. El contenido inicial preparado para visión, glosario y backlog fue consolidado aquí y se mantiene actualizado contra el backend y las migraciones reales; última actualización funcional: 2026-08-20.

## Entregables del Parcial 1

| Requisito de entrega | Documento | Estado documental |
|---|---|---|
| Visión y contexto del cliente | [01-vision-context.md](01-vision-context.md) | Completo |
| Matriz de actores, objetivos y responsabilidades | [02-actors-responsibilities.md](02-actors-responsibilities.md) | Completo |
| Catálogo RF/RNF trazable | [03-requirements-catalog.md](03-requirements-catalog.md) | Completo; estado funcional indicado por requisito |
| Reglas de negocio numeradas | [04-business-rules.md](04-business-rules.md) | Completo; RN-01 a RN-03 y RN-08 implementadas en backend; RN-04 a RN-07 pendientes |
| Casos de uso o mapa de historias | [05-use-cases-story-map.md](05-use-cases-story-map.md) | Completo |
| Modelo conceptual, DER y diccionario inicial | [06-data-model.md](06-data-model.md) | Completo para modelo actual; conceptos futuros identificados |
| Arquitectura, modularidad, hexagonal y Git | [07-architecture-decisions.md](07-architecture-decisions.md) | Completo como decisión; Git aún no inicializado |
| Backlog y criterios de aceptación | [08-backlog-acceptance.md](08-backlog-acceptance.md) | Completo y priorizado |

## Seguimiento y evidencia

- [PA-03-status.md](PA-03-status.md): matriz viva de cobertura real, brechas y verificaciones.
- [README principal](../README.md): requisitos locales, configuración, arranque, pruebas y API actual.
- `auth.http`, `usuarios.http`, `clientes.http`, `zonas.http`, `mesas.http`, `turnos.http` y `reservas.http`: evidencia ejecutable desde IntelliJ HTTP Client.
- `src/main/resources/db/migration`: esquema versionado mediante Flyway.
- `src/test`: pruebas automáticas de contratos, reglas y permisos.

## Regla de mantenimiento

Los documentos de alcance describen todo lo obligatorio, mientras `PA-03-status.md` distingue lo completo, parcial, ausente y bloqueado. No se debe presentar una función futura como implementada hasta que tenga código y evidencia de verificación.
