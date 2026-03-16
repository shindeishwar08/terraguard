export const LoadingSkeleton = () => (
  <div style={{
    position: 'absolute',
    inset: 0,
    background: '#0a0a0a',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 100,
    fontFamily: 'monospace',
    gap: 12,
    animation: 'fadeIn 0.5s ease',
  }}>
    <div style={{ fontSize: 48, animation: 'pulse 2s ease-in-out infinite' }}>🛡</div>
    <div style={{ fontSize: 20, fontWeight: 'bold', color: '#ff8c00', letterSpacing: 4 }}>
      TERRAGUARD
    </div>
    <div style={{ color: '#555', fontSize: 11, letterSpacing: 2 }}>
      REAL-TIME HUMANITARIAN INTELLIGENCE
    </div>

    <div style={{ marginTop: 24, width: 200, height: 2, background: '#222', borderRadius: 2, overflow: 'hidden' }}>
      <div style={{
        height: '100%',
        background: '#ff8c00',
        borderRadius: 2,
        animation: 'scan 1.2s ease-in-out infinite',
        width: '40%',
      }} />
    </div>

    <div style={{ color: '#333', fontSize: 10, letterSpacing: 1, marginTop: 8 }}>
      LOADING GLOBAL INTELLIGENCE...
    </div>

    <style>{`
      @keyframes fadeIn {
        from { opacity: 0; }
        to { opacity: 1; }
      }
      @keyframes pulse {
        0%, 100% { transform: scale(1); opacity: 1; }
        50% { transform: scale(1.1); opacity: 0.7; }
      }
      @keyframes scan {
        0% { transform: translateX(-100%); }
        100% { transform: translateX(600%); }
      }
    `}</style>
  </div>
);