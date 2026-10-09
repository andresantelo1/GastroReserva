import type { ReactNode } from 'react'

interface FormFieldProps {
  id: string
  label: string
  error?: string
  children: ReactNode
  wide?: boolean
}

export default function FormField({ id, label, error, children, wide = false }: FormFieldProps) {
  return (
    <div className={`form-field${wide ? ' form-span-2' : ''}`}>
      <label htmlFor={id}>{label}</label>
      {children}
      {error && <small id={`${id}-error`} className="field-error">{error}</small>}
    </div>
  )
}
