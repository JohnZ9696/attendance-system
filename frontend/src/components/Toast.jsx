import { useCallback, useMemo, useRef, useState } from 'react';
import { CheckCircle2, CircleAlert, Info, X } from 'lucide-react';
import { ToastContext } from './useToast.js';

const icons = { success: CheckCircle2, error: CircleAlert, info: Info };
let nextId = 1;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timers = useRef(new Map());

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
    const timer = timers.current.get(id);
    if (timer) { window.clearTimeout(timer); timers.current.delete(id); }
  }, []);

  const notify = useCallback((message, type = 'info') => {
    const id = nextId++;
    setToasts((current) => [...current, { id, message, type }].slice(-4));
    timers.current.set(id, window.setTimeout(() => dismiss(id), 3500));
  }, [dismiss]);

  const value = useMemo(() => notify, [notify]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-stack" aria-live="polite">
        {toasts.map(({ id, message, type }) => {
          const Icon = icons[type] || Info;
          return (
            <div className={`toast toast-${type}`} key={id} role="status">
              <Icon size={17} />
              <span>{message}</span>
              <button type="button" className="toast-close" onClick={() => dismiss(id)} aria-label="Đóng thông báo"><X size={14} /></button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}
