export type StatusTone = 'success' | 'danger' | 'warning' | 'info' | 'neutral'
export default function StatusBadge({ label, tone = 'neutral' }: { label: string; tone?: StatusTone }) {
  return <span className={`ui-badge ui-badge--${tone}`}>{label}</span>
}
