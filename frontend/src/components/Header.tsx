import { useMapContext } from '../context/MapContext';

interface HeaderProps {
  isDark: boolean;
  onToggleTheme: () => void;
}

const TYPE_COLORS: Record<string, string> = {
  EARTHQUAKE: '#ff8c00',
  WILDFIRE: '#ff3232',
  FLOOD: '#1e90ff',
  CYCLONE: '#b400ff',
};

export const Header = ({ isDark, onToggleTheme }: HeaderProps) => {
  const { snapshot, lastUpdated, snapshotError } = useMapContext();

  const escalatingCount = snapshot.filter(e => e.status === 'ESCALATING').length;

  const typeCounts = snapshot.reduce((acc, e) => {
    acc[e.disaster_type] = (acc[e.disaster_type] ?? 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  const isLive = !snapshotError;

  return (
    <div style={{
      position: 'absolute',
      top: 0,
      left: 0,
      right: 0,
      height: 48,
      background: 'rgba(0,0,0,0.85)',
      borderBottom: '1px solid #333',
      display: 'flex',
      alignItems: 'center',
      padding: '0 16px',
      zIndex: 25,
      fontFamily: 'monospace',
      gap: 12,
      fontSize: 12,
      color: '#fff',
    }}>

      {/* Logo */}
      <span style={{ color: '#ff8c00', fontWeight: 'bold', fontSize: 14 }}>
        TERRAGUARD
      </span>

      {/* Divider */}
      <span style={{ color: '#333' }}>|</span>

      {/* Live indicator */}
      <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{
          width: 8,
          height: 8,
          borderRadius: '50%',
          background: isLive ? '#33cc66' : '#ff3232',
          display: 'inline-block',
          animation: isLive ? 'livepulse 1.5s ease-in-out infinite' : 'none',
        }} />
        <span style={{ color: isLive ? '#33cc66' : '#ff3232', fontSize: 11 }}>
          {isLive ? 'LIVE' : 'OFFLINE'}
        </span>
      </span>

      {/* Divider */}
      <span style={{ color: '#333' }}>|</span>

      {/* Active count */}
      <span style={{
        background: '#ff3232',
        color: '#fff',
        borderRadius: 4,
        padding: '2px 8px',
        fontSize: 11,
        fontWeight: 'bold',
      }}>
        {snapshot.length} ACTIVE
      </span>

      {/* Escalating count */}
      {escalatingCount > 0 && (
        <span style={{
          background: '#7a0000',
          color: '#ff3232',
          borderRadius: 4,
          padding: '2px 8px',
          fontSize: 11,
          fontWeight: 'bold',
          border: '1px solid #ff3232',
        }}>
          ⚠ {escalatingCount} ESCALATING
        </span>
      )}

      {/* Divider */}
      <span style={{ color: '#333' }}>|</span>

      {/* Type breakdown */}
      <span style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
        {Object.entries(typeCounts).map(([type, count]) => (
          <span key={type} style={{ color: TYPE_COLORS[type] ?? '#aaa', fontSize: 11 }}>
            ● {count} {type}
          </span>
        ))}
      </span>

      {/* Spacer */}
      <div style={{ flex: 1 }} />

      {/* Last updated */}
      {lastUpdated && (
        <span style={{ color: '#555', fontSize: 11 }}>
          Updated: {lastUpdated.toLocaleTimeString()}
        </span>
      )}

      {/* Theme toggle */}
      <button onClick={onToggleTheme} style={{
        background: 'none',
        border: '1px solid #555',
        color: '#fff',
        borderRadius: 4,
        padding: '4px 10px',
        cursor: 'pointer',
        fontSize: 11,
        fontFamily: 'monospace',
      }}>
        {isDark ? '☀ LIGHT' : '🌙 DARK'}
      </button>

      <style>{`
        @keyframes livepulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.3; }
        }
      `}</style>
    </div>
  );
};