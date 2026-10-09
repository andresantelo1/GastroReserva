import type { ComponentPropsWithRef } from 'react'

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost'
type Props = ComponentPropsWithRef<'button'> & { variant?: ButtonVariant; loading?: boolean; loadingText?: string }

export default function Button({ variant = 'primary', loading = false, loadingText = 'Procesando…', disabled, className = '', type = 'button', children, ...props }: Props) {
  return <button {...props} type={type} className={`ui-button ui-button--${variant} ${className}`}
    disabled={disabled || loading} aria-busy={loading || undefined}>{loading ? loadingText : children}</button>
}
