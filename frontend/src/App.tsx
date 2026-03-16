import { useState, useMemo } from 'react';
import { MapProvider, useMapContext } from './context/MapContext';
import { MapView } from './components/MapView';
import { DeckOverlay } from './components/DeckOverlay';
import { LayerToggle } from './components/LayerToggle';
import { IncidentHub } from './components/IncidentHub';
import { Header } from './components/Header';
import { LoadingSkeleton } from './components/LoadingSkeleton';
import { useSnapshot } from './hooks/useSnapshot';
import { useGeolocation } from './hooks/useGeolocation';
import { useTimeRewind } from './hooks/useTimeRewind';
import { TimeRewindBar } from './components/TimeRewindBar';

const ALL_TYPES = new Set(['EARTHQUAKE', 'WILDFIRE', 'FLOOD', 'CYCLONE']);

const AppInner = () => {
  useSnapshot();
  useGeolocation();

  const { snapshot, isLoading } = useMapContext();
  const { sliderValue, setSliderValue, isPlaying, togglePlay, min, max, getLabel } = useTimeRewind();

  const filteredSnapshot = useMemo(() =>
    snapshot.filter(e => new Date(e.created_at).getTime() <= sliderValue),
    [snapshot, sliderValue]
  );

  const [visibleTypes, setVisibleTypes] = useState<Set<string>>(new Set(ALL_TYPES));

  const handleToggle = (type: string) => {
    setVisibleTypes(prev => {
      const next = new Set(prev);
      if (next.has(type)) next.delete(type);
      else next.add(type);
      return next;
    });
  };

  const [isDark, setIsDark] = useState(() => {
    return localStorage.getItem('terraguard-theme') !== 'light';
  });

  const toggleTheme = () => {
    setIsDark(prev => {
      const next = !prev;
      localStorage.setItem('terraguard-theme', next ? 'dark' : 'light');
      return next;
    });
  };

  const isMobile = window.innerWidth < 768;

  return (
    <div style={{ position: 'relative', width: '100vw', height: '100vh' }}>
      {isLoading && <LoadingSkeleton />}
      <Header isDark={isDark} onToggleTheme={toggleTheme} />
      <div style={{ paddingTop: 48, height: '100vh', boxSizing: 'border-box' }}>
        <MapView isDark={isDark} />
        <DeckOverlay visibleTypes={visibleTypes} filteredSnapshot={filteredSnapshot} />
      </div>
      <LayerToggle visibleTypes={visibleTypes} onToggle={handleToggle} />
      <TimeRewindBar
        sliderValue={sliderValue}
        min={min}
        max={max}
        isPlaying={isPlaying}
        label={getLabel()}
        onSliderChange={setSliderValue}
        onTogglePlay={togglePlay}
      />
      <IncidentHub isMobile={isMobile} />
    </div>
  );
};

function App() {
  return (
    <MapProvider>
      <AppInner />
    </MapProvider>
  );
}

export default App;