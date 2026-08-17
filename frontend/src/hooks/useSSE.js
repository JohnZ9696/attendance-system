import { useEffect, useState } from 'react';

export function useSSE(endpoint) {
  const [events, setEvents] = useState([]);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    let eventSource;
    let reconnectTimer;

    const pushEvent = (event) => {
      try {
        const parsed = JSON.parse(event.data);

        setEvents((previous) => [
          {
            id: `${Date.now()}-${Math.random()}`,
            type: parsed.type || 'unknown',
            data: parsed.data || {},
            timestamp: new Date(),
          },
          ...previous,
        ].slice(0, 50));
      } catch (error) {
        console.error('Không thể đọc sự kiện SSE:', error, event.data);
      }
    };

    const connect = () => {
      const token = localStorage.getItem('token');
      const baseUrl =
        import.meta.env.VITE_API_BASE_URL ||
        'http://localhost:8080/api/v1';

      // Khi đang bỏ qua đăng nhập, token có thể không tồn tại.
      const query = token
        ? `?token=${encodeURIComponent(token)}`
        : '';

      eventSource = new EventSource(
        `${baseUrl}${endpoint}${query}`
      );

      eventSource.onopen = () => {
        setConnected(true);
      };

      eventSource.onmessage = pushEvent;

      eventSource.onerror = (error) => {
        console.error('SSE connection error:', error);
        setConnected(false);
        eventSource.close();
        reconnectTimer = window.setTimeout(connect, 3000);
      };
    };

    connect();

    return () => {
      window.clearTimeout(reconnectTimer);
      eventSource?.close();
    };
  }, [endpoint]);

  return { events, connected };
}