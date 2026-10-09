import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'
import { setSession } from '../features/auth/session'

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('HTTP real deshabilitado en Vitest; simular el service o transporte.'))))
})
afterEach(() => { cleanup(); setSession(null); vi.unstubAllGlobals() })
