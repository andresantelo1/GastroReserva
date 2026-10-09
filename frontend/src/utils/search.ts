// Normaliza espacios y mayúsculas, no modifica los datos originales.
export const normalizeSearch = (value: string) => value.trim().toLocaleLowerCase('es')
export function matchesText(values: readonly (string | number | null | undefined)[], search: string) {
  const term = normalizeSearch(search)
  return !term || values.some((value) => value != null && normalizeSearch(String(value)).includes(term))
}
