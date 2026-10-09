export default function EmptyState({ title, description }: { title: string; description?: string }) {
  return <section className="ui-empty" role="status"><h4>{title}</h4>{description && <p>{description}</p>}</section>
}
