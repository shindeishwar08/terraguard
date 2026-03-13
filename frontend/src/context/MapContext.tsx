import { createContext, useContext, useState, useRef } from 'react';
import type { ReactNode } from 'react';
import type { Map } from 'maplibre-gl';
import type { GlobalEventResponse } from '../types';

interface MapContextType {
    mapRef: React.MutableRefObject<Map | null>;
    snapshot: GlobalEventResponse[];
    setSnapshot: (data: GlobalEventResponse[]) => void;
    selectedEvent: GlobalEventResponse | null;
    setSelectedEvent: (event: GlobalEventResponse | null) => void;
    nearbyEvents: GlobalEventResponse[];
    setNearbyEvents: (events: GlobalEventResponse[]) => void;
}

const MapContext = createContext<MapContextType | null>(null);

export const MapProvider = ({ children }: { children: ReactNode }) => {
    const mapRef = useRef<Map | null>(null);
    const [snapshot, setSnapshot] = useState<GlobalEventResponse[]>([]);
    const [selectedEvent, setSelectedEvent] = useState<GlobalEventResponse | null>(null);
    const [nearbyEvents, setNearbyEvents] = useState<GlobalEventResponse[]>([]);

    return (
        <MapContext.Provider value={{
            mapRef,
            snapshot,
            setSnapshot,
            selectedEvent,
            setSelectedEvent,
            nearbyEvents,
            setNearbyEvents,
        }}>
            {children}
        </MapContext.Provider>
    );
};

export const useMapContext = () => {
    const ctx = useContext(MapContext);
    if (!ctx) throw new Error('useMapContext must be used within MapProvider');
    return ctx;
};