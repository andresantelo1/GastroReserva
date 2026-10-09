import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    // Nunca enviar HTTP real desde las pruebas de componentes.
    env: { VITE_API_URL: 'http://localhost:8080/api' },
    restoreMocks: true,
    clearMocks: true,
  },
})
