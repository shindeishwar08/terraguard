import { MapProvider } from './context/MapContext';
import { MapView } from './components/MapView';
import { useSnapshot } from './hooks/useSnapshot';
import { useGeolocation } from './hooks/useGeolocation';

const AppInner = () => {
  useSnapshot();
  useGeolocation();
  return <MapView />;
};

function App() {
  return (
    <MapProvider>
      <AppInner />
    </MapProvider>
  );
}

export default App;