interface Props { label: string; value: number; helper?: string }

export default function MetricCard({ label, value, helper }: Props) {
  return <article className="stat-card" aria-label={label}>
    <span>{label}</span><strong>{value}</strong>
    {helper && <small className="metric-helper">{helper}</small>}
  </article>
}
