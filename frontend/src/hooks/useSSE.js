import { useState, useEffect } from 'react';

export function useSSE(endpoint) {
  const [events, setEvents] = useState([]);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return;

    let eventSource;
    let reconnectTimer;

    const connect = () => {
      const url = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'}${endpoint}?token=${token}`;
      eventSource = new EventSource(url);

      eventSource.onopen = () => {
        setConnected(true);
      };

      eventSource.onmessage = (event) => {
        try {
          const parsed = JSON.parse(event.data);
          // add timestamp for display
          parsed.timestamp = new Date();
          setEvents(prev => [parsed, ...prev].slice(0, 50)); // keep last 50 events
        } catch (e) {
          console.error("Error parsing SSE data", e);
        }
      };

      eventSource.onerror = (err) => {
        console.error("SSE Error", err);
        setConnected(false);
        eventSource.close();
        reconnectTimer = setTimeout(connect, 5000);
      };
    };

    connect();

    return () => {
      clearTimeout(reconnectTimer);
      if (eventSource) {
        eventSource.close();
      }
    };
  }, [endpoint]);

  return { events, connected };
}
