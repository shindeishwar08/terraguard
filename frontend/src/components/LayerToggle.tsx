interface LayerToggleProps {
    visibleTypes: Set<string>;
    onToggle: (type: string) => void;
}

const DISASTER_TYPES = ['EARTHQUAKE', 'WILDFIRE', 'FLOOD', 'CYCLONE'];

const TYPE_COLORS: Record<string, string> = {
    EARTHQUAKE: '#ff8c00',
    WILDFIRE: '#ff3232',
    FLOOD: '#1e90ff',
    CYCLONE: '#b400ff',
};

export const LayerToggle = ({ visibleTypes, onToggle }: LayerToggleProps) => {
    return (
        <div style={{
            position: 'absolute',
            top: 16,
            left: 16,
            background: 'rgba(0,0,0,0.75)',
            padding: '12px 16px',
            borderRadius: 8,
            color: '#fff',
            zIndex: 10,
            display: 'flex',
            flexDirection: 'column',
            gap: 8,
            fontFamily: 'monospace',
            fontSize: 13,
        }}>
            <div style={{ fontWeight: 'bold', marginBottom: 4 }}>TERRAGUARD</div>
            {DISASTER_TYPES.map(type => (
                <label key={type} style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                    <input
                        type="checkbox"
                        checked={visibleTypes.has(type)}
                        onChange={() => onToggle(type)}
                    />
                    <span style={{ color: TYPE_COLORS[type] }}>●</span>
                    {type}
                </label>
            ))}
        </div>
    );
};