import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import './styles/global.css'
import './styles/tokens.css'
import './styles/ui.css'
import App from './app/App.tsx'
import AppErrorBoundary from './app/AppErrorBoundary'
import './styles/dashboard.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppErrorBoundary>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </AppErrorBoundary>
  </StrictMode>,
)
