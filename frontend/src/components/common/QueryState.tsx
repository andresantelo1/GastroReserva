interface Props { loading: boolean; error: string; onRetry: () => void; label: string }
export default function QueryState({ loading, error, onRetry, label }: Props) {
  if (loading) return <div className="state-card" role="status">Cargando {label}…</div>
  if (error) return <div className="state-card error" role="alert"><p>{error}</p><button className="btn-secondary" onClick={onRetry}>Reintentar carga</button></div>
  return null
}

