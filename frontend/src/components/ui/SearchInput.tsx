import { useId } from 'react'

export default function SearchInput({ value, onChange, label = 'Buscar', placeholder = 'Buscar…', disabled = false }: {
  value: string; onChange: (value: string) => void; label?: string; placeholder?: string; disabled?: boolean
}) {
  const id = useId()
  return <div className="ui-field ui-search"><label htmlFor={id}>{label}</label>
    <input id={id} type="search" value={value} disabled={disabled} placeholder={placeholder} onChange={(e) => onChange(e.currentTarget.value)} />
  </div>
}
