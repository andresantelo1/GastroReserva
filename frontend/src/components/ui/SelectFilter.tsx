import { useId } from 'react'

export interface SelectOption { value: string; label: string }
export default function SelectFilter({ label, value, options, onChange, disabled = false }: {
  label: string; value: string; options: readonly SelectOption[]; onChange: (value: string) => void; disabled?: boolean
}) {
  const id = useId()
  return <div className="ui-field"><label htmlFor={id}>{label}</label>
    <select id={id} value={value} disabled={disabled} onChange={(e) => onChange(e.currentTarget.value)}>
      {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
    </select>
  </div>
}
