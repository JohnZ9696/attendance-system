export function Button({ variant = 'secondary', className = '', children, ...props }) {
  return (
    <button className={`btn btn-${variant} ${className}`.trim()} {...props}>
      {children}
    </button>
  );
}

export function Panel({ className = '', children }) {
  return <section className={`panel ${className}`.trim()}>{children}</section>;
}

export function PageHeader({ title, description, actions }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}

export function ErrorBanner({ message, onRetry }) {
  if (!message) return null;
  return (
    <div className="error-banner" role="alert">
      <span>{message}</span>
      {onRetry && <Button onClick={onRetry}>Thử lại</Button>}
    </div>
  );
}
