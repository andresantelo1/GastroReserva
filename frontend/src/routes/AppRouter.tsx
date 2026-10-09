import { Route, Routes } from 'react-router'
import MainLayout from '../layouts/MainLayout'
import DashboardPage from '../pages/DashboardPage'
import NotFoundPage from '../pages/NotFoundPage'
import AcercaPage from '../pages/AcercaPage'
import ClientesPage from '../features/clientes/pages/ClientesPage'
import MesasPage from '../features/mesas/pages/MesasPage'
import ReservasPage from '../features/reservas/pages/ReservasPage'
import LoginPage from '../features/auth/LoginPage'
import AccessBoundary from '../features/auth/AccessBoundary'
export default function AppRouter() {
  return <Routes><Route element={<MainLayout />}>
    <Route index element={<DashboardPage />} /><Route path="login" element={<LoginPage />} />
    <Route path="clientes" element={<AccessBoundary roles={['ADMINISTRADOR','HOST']}><ClientesPage /></AccessBoundary>} />
    <Route path="mesas" element={<AccessBoundary roles={['ADMINISTRADOR','HOST','MESERO']}><MesasPage /></AccessBoundary>} />
    <Route path="reservas" element={<AccessBoundary roles={['ADMINISTRADOR','HOST','MESERO']}><ReservasPage /></AccessBoundary>} />
    <Route path="acerca" element={<AcercaPage />} /><Route path="*" element={<NotFoundPage />} />
  </Route></Routes>
}

