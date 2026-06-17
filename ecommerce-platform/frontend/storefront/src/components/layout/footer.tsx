export function Footer() {
  return (
    <footer className="border-t py-8">
      <div className="container flex flex-col items-center justify-between gap-2 text-sm text-muted-foreground sm:flex-row">
        <p>© {new Date().getFullYear()} Northwind. Demo storefront.</p>
        <p>Powered by the e-commerce microservices platform.</p>
      </div>
    </footer>
  )
}
