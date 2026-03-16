import { useState } from 'react';
import { MapProvider } from './context/MapContext';
import { MapView } from './components/MapView';
import { DeckOverlay } from './components/DeckOverlay';
import { LayerToggle } from './components/LayerToggle';
import { useSnapshot } from './hooks/useSnapshot';
import { useGeolocation } from './hooks/useGeolocation';

const ALL_TYPES = new Set(['EARTHQUAKE', 'WILDFIRE', 'FLOOD', 'CYCLONE']);

const AppInner = () => {
  useSnapshot();
  useGeolocation();

  const [visibleTypes, setVisibleTypes] = useState<Set<string>>(new Set(ALL_TYPES));

  const handleToggle = (type: string) => {
    setVisibleTypes(prev => {
      const next = new Set(prev);
      if (next.has(type)) next.delete(type);
      else next.add(type);
      return next;
    });
  };

  return (
    <div style={{ position: 'relative', width: '100vw', height: '100vh' }}>
      <MapView />
      <DeckOverlay visibleTypes={visibleTypes} />
      <LayerToggle visibleTypes={visibleTypes} onToggle={handleToggle} />
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