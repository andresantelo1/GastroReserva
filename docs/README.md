# GastroReserva — documentación del proyecto

Esta carpeta reúne los entregables académicos y técnicos del PA-03. El contenido inicial preparado para visión, glosario y backlog fue consolidado aquí y se mantiene actualizado contra el backend y las migraciones reales; última actualización funcional: 2026-10-09 (guía 10 del frontend: dashboard básico, ErrorBoundary, Vitest y preparación de build/despliegue, conservando CRUD clientes y gestión limitada de reservas; sin cambios Java/SQL en esta guía. Resto de web/móvil, publicación, Docker, CI e IA pendientes).

## Entregables del Parcial 1

| Requisito de entrega | Documento | Estado documental |
|---|---|---|
| Visión y contexto del cliente | [01-vision-context.md](01-vision-context.md) | Completo |
| Matriz de actores, objetivos y responsabilidades | [02-actors-responsibilities.md](02-actors-responsibilities.md) | Completo |
| Catálogo RF/RNF trazable | [03-requirements-catalog.md](03-requirements-catalog.md) | Completo; estado funcional indicado por requisito |
| Reglas de negocio numeradas | [04-business-rules.md](04-business-rules.md) | Completo; RN-01 a RN-08 implementadas y probadas en backend |
| Casos de uso o mapa de historias | [05-use-cases-story-map.md](05-use-cases-story-map.md) | Completo |
| Modelo conceptual, DER y diccionario inicial | [06-data-model.md](06-data-model.md) | Completo para Flyway V1–V9 |
| Arquitectura, modularidad, hexagonal y Git | [07-architecture-decisions.md](07-architecture-decisions.md) | Completo como decisión; Git inicializado, flujo de ramas documentado |
| Backlog y criterios de aceptación | [08-backlog-acceptance.md](08-backlog-acceptance.md) | Completo y priorizado |

## Seguimiento y evidencia

- [INICIO-EQUIPO.md](../INICIO-EQUIPO.md): descargar el repositorio conjunto y arrancar backend y frontend con base/credenciales propias. React se incluye en [frontend/](../frontend/README.md); ya no hace falta acceder a la carpeta personal de WebStorm del autor.

- [PA-03-status.md](PA-03-status.md): matriz viva de cobertura real, brechas y verificaciones.
- [README principal](../README.md): requisitos locales, configuración, arranque, pruebas y API actual.
- [09-backend-handoff.md](09-backend-handoff.md): contratos, permisos, límites y guía para integrar el frontend.
- Frontend separado en `C:/Users/antel/WebstormProjects/gastroreserva1`: `docs/guia-10.md` contiene adaptación, aceptación y 50 respuestas; `docs/evidencias/guia-10/matriz-fullstack.md` registra pruebas, capturas y límites; `docs/despliegue.md` prepara publicación sin afirmar que esté desplegada. Las guías anteriores se conservan como evidencia histórica.
- `auth.http`, `usuarios.http`, `clientes.http`, `zonas.http`, `mesas.http`, `turnos.http`, `reservas.http`, `carta.http`, [recorrido-backend.http](../recorrido-backend.http) y [mesas-abiertas.http](../mesas-abiertas.http): ejemplos IntelliJ HTTP Client.
- [scripts/verify-postgres.ps1](../scripts/verify-postgres.ps1): verificación aislada con PostgreSQL real.
- `src/main/resources/db/migration`: esquema versionado mediante Flyway.
- `src/test`: pruebas automáticas de contratos, reglas y permisos.

## Regla de mantenimiento

Los documentos de alcance describen todo lo obligatorio, mientras `PA-03-status.md` distingue lo completo, parcial, ausente y bloqueado. No se debe presentar una función futura como implementada hasta que tenga código y evidencia de verificación.
