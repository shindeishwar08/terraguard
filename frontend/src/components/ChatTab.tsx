import { useEffect, useRef, useState } from 'react';
import { useChatRoom } from '../hooks/useChatRoom';

interface ChatTabProps {
    incidentId: string;
}

const SIGNAL_TAGS = ['ROAD_BLOCKED', 'POWER_OUTAGE', 'MEDICAL_NEED', 'MISINFORMATION', 'ALL_CLEAR'];

const TAG_ICONS: Record<string, string> = {
    ROAD_BLOCKED: '🚧',
    POWER_OUTAGE: '⚡',
    MEDICAL_NEED: '🏥',
    MISINFORMATION: '⚠',
    ALL_CLEAR: '✅',
};

export const ChatTab = ({ incidentId }: ChatTabProps) => {
    const { messages, connected, sendMessage } = useChatRoom(incidentId);
    const [input, setInput] = useState('');
    const [selectedTag, setSelectedTag] = useState<string | null>(null);
    const [displayName, setDisplayName] = useState(() =>
        localStorage.getItem('terraguard-display-name') ?? ''
    );
    const [nameSet, setNameSet] = useState(() =>
        !!localStorage.getItem('terraguard-display-name')
    );
    const messagesRef = useRef<HTMLDivElement>(null);

    // Auto-scroll to bottom on new message
    useEffect(() => {
        if (messagesRef.current) {
            messagesRef.current.scrollTop = messagesRef.current.scrollHeight;
        }
    }, [messages]);

    const handleSetName = () => {
        if (!displayName.trim()) return;
        localStorage.setItem('terraguard-display-name', displayName.trim());
        setNameSet(true);
    };

    const handleSend = () => {
        if (!input.trim() || !nameSet) return;
        sendMessage(input.trim(), displayName, selectedTag);
        setInput('');
        setSelectedTag(null);
    };

    // Name input screen
    if (!nameSet) {
        return (
            <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
                <div style={{ color: '#aaa', fontSize: 12 }}>Enter your display name to join the chat:</div>
                <input
                    value={displayName}
                    onChange={e => setDisplayName(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && handleSetName()}
                    placeholder="e.g. Field Officer - Mumbai"
                    style={{
                        background: '#1a1a1a',
                        border: '1px solid #444',
                        borderRadius: 4,
                        padding: '8px 10px',
                        color: '#fff',
                        fontFamily: 'monospace',
                        fontSize: 12,
                    }}
                />
                <button onClick={handleSetName} style={{
                    background: '#ff8c00',
                    border: 'none',
                    borderRadius: 4,
                    padding: '8px 12px',
                    color: '#000',
                    fontFamily: 'monospace',
                    fontSize: 12,
                    fontWeight: 'bold',
                    cursor: 'pointer',
                }}>
                    JOIN CHAT
                </button>
            </div>
        );
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0, height: '100%' }}>

            {/* Connection status */}
            <div style={{
                padding: '6px 16px',
                fontSize: 11,
                color: connected ? '#33cc66' : '#ff3232',
                borderBottom: '1px solid #222',
                fontFamily: 'monospace',
            }}>
                {connected ? '● CONNECTED' : '● CONNECTING...'}
                <span style={{ color: '#555', marginLeft: 8 }}>as {displayName}</span>
            </div>

            {/* Message list */}
            <div ref={messagesRef} style={{
                flex: 1,
                overflowY: 'auto',
                padding: '8px 16px',
                display: 'flex',
                flexDirection: 'column',
                gap: 8,
                minHeight: 0,
            }}>
                {messages.length === 0 && (
                    <div style={{ color: '#444', fontSize: 11, textAlign: 'center', marginTop: 16 }}>
                        No messages yet. Be the first to report.
                    </div>
                )}
                {messages.map((msg, i) => (
                    <div key={i} style={{
                        borderLeft: msg.type === 'HIGHLIGHT' ? '3px solid #ff3232' : '3px solid #333',
                        paddingLeft: 10,
                        fontFamily: 'monospace',
                    }}>
                        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 2 }}>
                            <span style={{ color: '#ff8c00', fontSize: 11, fontWeight: 'bold' }}>
                                {msg.displayName}
                            </span>
                            {msg.tag && (
                                <span style={{
                                    background: '#222',
                                    color: '#aaa',
                                    fontSize: 10,
                                    padding: '1px 6px',
                                    borderRadius: 3,
                                }}>
                                    {TAG_ICONS[msg.tag]} {msg.tag}
                                </span>
                            )}
                            {msg.type === 'HIGHLIGHT' && (
                                <span style={{ color: '#ff3232', fontSize: 10 }}>📌 HIGHLIGHT</span>
                            )}
                        </div>
                        <div style={{ color: '#ddd', fontSize: 12 }}>{msg.content}</div>
                        <div style={{ color: '#444', fontSize: 10, marginTop: 2 }}>
                            {new Date(msg.timestamp).toLocaleTimeString()}
                        </div>
                    </div>
                ))}
            </div>

            {/* Signal tags */}
            <div style={{
                padding: '6px 16px',
                display: 'flex',
                gap: 6,
                flexWrap: 'wrap',
                borderTop: '1px solid #222',
            }}>
                {SIGNAL_TAGS.map(tag => (
                    <button
                        key={tag}
                        onClick={() => setSelectedTag(prev => prev === tag ? null : tag)}
                        style={{
                            background: selectedTag === tag ? '#333' : 'none',
                            border: `1px solid ${selectedTag === tag ? '#ff8c00' : '#444'}`,
                            color: selectedTag === tag ? '#ff8c00' : '#666',
                            borderRadius: 4,
                            padding: '3px 8px',
                            fontSize: 10,
                            cursor: 'pointer',
                            fontFamily: 'monospace',
                        }}
                    >
                        {TAG_ICONS[tag]} {tag.replace('_', ' ')}
                    </button>
                ))}
            </div>

            {/* Input */}
            <div style={{
                padding: '8px 16px',
                display: 'flex',
                gap: 8,
                borderTop: '1px solid #222',
            }}>
                <input
                    value={input}
                    onChange={e => setInput(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && handleSend()}
                    placeholder="Report situation..."
                    style={{
                        flex: 1,
                        background: '#1a1a1a',
                        border: '1px solid #444',
                        borderRadius: 4,
                        padding: '6px 10px',
                        color: '#fff',
                        fontFamily: 'monospace',
                        fontSize: 12,
                    }}
                />
                <button onClick={handleSend} style={{
                    background: '#ff8c00',
                    border: 'none',
                    borderRadius: 4,
                    padding: '6px 12px',
                    color: '#000',
                    fontFamily: 'monospace',
                    fontSize: 12,
                    fontWeight: 'bold',
                    cursor: 'pointer',
                }}>
                    SEND
                </button>
            </div>
        </div>
    );
};