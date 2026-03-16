interface TimeRewindBarProps {
  sliderValue: number;
  min: number;
  max: number;
  isPlaying: boolean;
  label: string;
  onSliderChange: (value: number) => void;
  onTogglePlay: () => void;
}

export const TimeRewindBar = ({
  sliderValue, min, max, isPlaying, label, onSliderChange, onTogglePlay
}: TimeRewindBarProps) => {
  return (
    <div style={{
      position: 'absolute',
      bottom: 24,
      left: '50%',
      transform: 'translateX(-50%)',
      background: 'rgba(0,0,0,0.80)',
      padding: '10px 20px',
      borderRadius: 8,
      zIndex: 10,
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      fontFamily: 'monospace',
      fontSize: 12,
      color: '#fff',
      minWidth: 400,
    }}>
      <button
        onClick={onTogglePlay}
        style={{
          background: 'none',
          border: '1px solid #555',
          color: '#fff',
          borderRadius: 4,
          padding: '4px 10px',
          cursor: 'pointer',
          fontSize: 12,
        }}
      >
        {isPlaying ? '⏸ PAUSE' : '▶ PLAY'}
      </button>

      <input
        type="range"
        min={min}
        max={max}
        value={sliderValue}
        onChange={(e) => onSliderChange(Number(e.target.value))}
        style={{ flex: 1, accentColor: '#ff8c00' }}
      />

      <span style={{ color: '#ff8c00', whiteSpace: 'nowrap' }}>{label}</span>
    </div>
  );
};
