import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BASE_URL = import.meta.env.VITE_API_BASE_URL;

export interface ChatMessageUI {
    incidentId: string;
    displayName: string;
    content: string;
    tag: string | null;
    type: 'NORMAL' | 'HIGHLIGHT';
    timestamp: string;
}

export const useChatRoom = (incidentId: string | null) => {
    const [messages, setMessages] = useState<ChatMessageUI[]>([]);
    const [connected, setConnected] = useState(false);
    const clientRef = useRef<Client | null>(null);

    useEffect(() => {
        if (!incidentId) return;

        const client = new Client({
            webSocketFactory: () => new SockJS(`${BASE_URL}/ws`),
            reconnectDelay: 5000,
            onConnect: () => {
                setConnected(true);

                // Subscribe to live messages
                client.subscribe(`/topic/incidents/${incidentId}`, (frame) => {
                    const msg: ChatMessageUI = JSON.parse(frame.body);
                    setMessages(prev => [...prev, msg]);
                });

                // Fetch history — sent only to this subscriber
                client.subscribe(`/app/incidents/${incidentId}/history`, (frame) => {
                    const history: ChatMessageUI[] = JSON.parse(frame.body);
                    setMessages(history);
                });

                // Trigger history fetch
                client.publish({
                    destination: `/app/incidents/${incidentId}/history`,
                    body: '',
                });
            },
            onDisconnect: () => setConnected(false),
        });

        client.activate();
        clientRef.current = client;

        // Cleanup on incident change or panel close
        return () => {
            client.deactivate();
            clientRef.current = null;
            setMessages([]);
            setConnected(false);
        };
    }, [incidentId]);

    const sendMessage = useCallback((content: string, displayName: string, tag: string | null) => {
        if (!clientRef.current?.connected || !incidentId) return;

        clientRef.current.publish({
            destination: `/app/incidents/${incidentId}/chat`,
            body: JSON.stringify({
                incidentId,
                displayName,
                content,
                tag,
                type: 'NORMAL',
                timestamp: new Date().toISOString(),
            }),
        });
    }, [incidentId]);

    return { messages, connected, sendMessage };
};