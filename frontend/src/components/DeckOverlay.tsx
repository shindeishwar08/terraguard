import { useEffect, useRef } from 'react';
import { MapboxOverlay } from '@deck.gl/mapbox';
import { useMapContext } from '../context/MapContext';
import { useEventLayer } from './EventLayer';
import type { GlobalEventResponse } from '../types';

interface DeckOverlayProps {
    visibleTypes: Set<string>;
    filteredSnapshot: GlobalEventResponse[];
}

export const DeckOverlay = ({ visibleTypes, filteredSnapshot }: DeckOverlayProps) => {
    const { mapRef, setSelectedEvent, mapLoaded } = useMapContext();
    const overlayRef = useRef<MapboxOverlay | null>(null);

    const handleEventClick = (event: GlobalEventResponse) => {
        setSelectedEvent(event);
        console.log('[DeckOverlay] Selected event:', event.title);
    };

    const layers = useEventLayer(visibleTypes, filteredSnapshot, handleEventClick);

    // Create overlay once when map loads
    useEffect(() => {
        if (!mapRef.current || !mapLoaded) return;

        const overlay = new MapboxOverlay({
            interleaved: false,
            layers: [],
        });

        mapRef.current.addControl(overlay as any);
        overlayRef.current = overlay;

        return () => {
            mapRef.current?.removeControl(overlay as any);
            overlayRef.current = null;
        };
    }, [mapLoaded]);

    // Update layers without recreating overlay
    useEffect(() => {
        if (!overlayRef.current) return;
        overlayRef.current.setProps({ layers });
    }, [layers]);

    return null;
};