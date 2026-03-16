import { useEffect, useRef } from 'react';
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { useMapContext } from '../context/MapContext';

const MAP_STYLE = 'https://tiles.openfreemap.org/styles/dark';

export const MapView = () => {
    const containerRef = useRef<HTMLDivElement>(null);
    const { mapRef, setViewState, setMapLoaded } = useMapContext();

    useEffect(() => {
        if (!containerRef.current) return;

        const map = new maplibregl.Map({
            container: containerRef.current,
            style: MAP_STYLE,
            center: [0, 20],
            zoom: 2,
            attributionControl: false,
        });

        map.addControl(new maplibregl.NavigationControl(), 'top-right');
        mapRef.current = map;

        // Sync every map move to context viewState
        const syncViewState = () => {
            const center = map.getCenter();
            setViewState({
                longitude: center.lng,
                latitude: center.lat,
                zoom: map.getZoom(),
                bearing: map.getBearing(),
                pitch: map.getPitch(),
            });
        };

        map.on('move', syncViewState);
        map.on('zoom', syncViewState);
        map.on('load', () => {
            setMapLoaded(true);
        });

        return () => {
            map.remove();
            mapRef.current = null;
        };
    }, []);

    return (
        <div
            ref={containerRef}
            style={{ width: '100vw', height: '100vh' }}
        />
    );
};