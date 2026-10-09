# Estructura comprobada — guía 01

Carpeta: `C:\Users\antel\WebstormProjects\gastroreserva1`. Verificada con `tree src /F /A` y listado de archivos. Los directorios reservados aún vacíos contienen `.gitkeep`.

```text
src/
  main.tsx
  app/
    App.tsx
  assets/
  components/common/
  config/
  features/
    auth/
      components/
      pages/
      services/
      types/
    clientes/
      components/ClienteCard.tsx
      pages/
      services/
      types/Cliente.ts
    mesas/
      components/MesaCard.tsx
      pages/
      services/
      types/Mesa.ts
    reservas/
      components/ReservaCard.tsx
      pages/
      services/
      types/Reserva.ts
  hooks/
  layouts/
  pages/
  routes/
  services/http/
  styles/global.css
  types/
  utils/
```

En la raíz permanecen `package.json`, `package-lock.json`, `vite.config.ts`, `tsconfig*.json`, `.env.example`, `.gitignore`, `index.html`, `README.md` y `README-front.md`. `public/favicon.svg` es el icono de GastroReserva. `docs` contiene seguimiento y evidencias.

El proyecto no es todavía un repositorio Git propio; no se inicializó ni se publicó en esta guía. El archivo `.gitignore` y los `.gitkeep` están preparados para ese paso posterior.
