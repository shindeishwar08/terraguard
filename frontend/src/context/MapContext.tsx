import { createContext, useContext, useState, useRef } from 'react';
import type { ReactNode } from 'react';
import type { Map } from 'maplibre-gl';
import type { GlobalEventResponse } from '../types';

interface ViewState {
    longitude: number;
    latitude: number;
    zoom: number;
    bearing: number;
    pitch: number;
}

interface MapContextType {
    mapRef: React.MutableRefObject<Map | null>;
    snapshot: GlobalEventResponse[];
    setSnapshot: (data: GlobalEventResponse[]) => void;
    selectedEvent: GlobalEventResponse | null;
    setSelectedEvent: (event: GlobalEventResponse | null) => void;
    nearbyEvents: GlobalEventResponse[];
    setNearbyEvents: (events: GlobalEventResponse[]) => void;
    viewState: ViewState;
    setViewState: (vs: ViewState) => void;
    mapLoaded: boolean;
    setMapLoaded: (loaded: boolean) => void;
    isLoading: boolean;
    setIsLoading: (v: boolean) => void;
    snapshotError: string | null;
    setSnapshotError: (e: string | null) => void;
    lastUpdated: Date | null;
    setLastUpdated: (d: Date | null) => void;
}

const MapContext = createContext<MapContextType | null>(null);

const DEFAULT_VIEW_STATE: ViewState = {
    longitude: 0,
    latitude: 20,
    zoom: 2,
    bearing: 0,
    pitch: 0,
};

export const MapProvider = ({ children }: { children: ReactNode }) => {
    const mapRef = useRef<Map | null>(null);
    const [snapshot, setSnapshot] = useState<GlobalEventResponse[]>([]);
    const [selectedEvent, setSelectedEvent] = useState<GlobalEventResponse | null>(null);
    const [nearbyEvents, setNearbyEvents] = useState<GlobalEventResponse[]>([]);
    const [viewState, setViewState] = useState<ViewState>(DEFAULT_VIEW_STATE);
    const [mapLoaded, setMapLoaded] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [snapshotError, setSnapshotError] = useState<string | null>(null);
    const [lastUpdated, setLastUpdated] = useState<Date | null>(null);


    return (
        <MapContext.Provider value={{
            mapRef,
            snapshot,
            setSnapshot,
            selectedEvent,
            setSelectedEvent,
            nearbyEvents,
            setNearbyEvents,
            viewState,
            setViewState,
            mapLoaded,
            setMapLoaded,
            isLoading,
            setIsLoading,
            snapshotError,
            setSnapshotError,
            lastUpdated,
            setLastUpdated,
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