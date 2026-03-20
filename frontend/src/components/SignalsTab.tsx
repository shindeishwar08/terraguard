import { useSignals } from '../hooks/useSignals';

interface SignalsTabProps {
    incidentId: string;
}

const SIGNAL_CONFIG: Record<string, { icon: string; label: string; color: string }> = {
    ROAD_BLOCKED: { icon: '🚧', label: 'Road Blocked', color: '#ffaa00' },
    POWER_OUTAGE: { icon: '⚡', label: 'Power Outage', color: '#ffdd00' },
    MEDICAL_NEED: { icon: '🏥', label: 'Medical Need', color: '#ff3232' },
    MISINFORMATION: { icon: '⚠️', label: 'Misinformation', color: '#888888' },
    ALL_CLEAR: { icon: '✅', label: 'All Clear', color: '#33cc66' },
};

export const SignalsTab = ({ incidentId }: SignalsTabProps) => {
    const { tally, flash } = useSignals(incidentId);

    const total = Object.values(tally).reduce((sum, v) => sum + (Number(v) || 0), 0);

    return (
        <div style={{ flex: 1, overflowY: 'auto', padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>

            {/* Total count */}
            <div style={{ color: '#aaa', fontSize: 11, marginBottom: 4 }}>
                CROWD SIGNALS — {total} total reports
            </div>

            {/* Signal rows */}
            {Object.entries(SIGNAL_CONFIG).map(([key, config]) => {
                const count = Number((tally as Record<string, number>)[key] ?? 0);
                const isFlashing = flash === key;

                return (
                    <div key={key} style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        padding: '10px 12px',
                        background: isFlashing ? '#1a1a1a' : '#111',
                        borderRadius: 6,
                        border: `1px solid ${isFlashing ? config.color : '#222'}`,
                        transition: 'border-color 0.3s ease, background 0.3s ease',
                    }}>
                        <span style={{ fontSize: 20 }}>{config.icon}</span>
                        <span style={{ flex: 1, color: '#ccc', fontSize: 12 }}>{config.label}</span>
                        <span style={{
                            color: count > 0 ? config.color : '#444',
                            fontWeight: 'bold',
                            fontSize: 18,
                            fontFamily: 'monospace',
                            transition: 'color 0.3s ease',
                        }}>
                            {count}
                        </span>
                    </div>
                );
            })}

            {total === 0 && (
                <div style={{ color: '#444', fontSize: 11, textAlign: 'center', marginTop: 16 }}>
                    No crowd signals reported yet.
                </div>
            )}

            <div style={{ color: '#333', fontSize: 10, marginTop: 8, textAlign: 'center' }}>
                Updates every 10 seconds
            </div>
        </div>
    );
};