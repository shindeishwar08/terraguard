import { useState, useEffect } from 'react';
import { fetchNearby } from '../api';
import { useMapContext } from '../context/MapContext';
import type { GlobalEventResponse } from '../types';

export const useGeolocation = () => {
    const { setNearbyEvents } = useMapContext();
    const [location, setLocation] = useState<{ lat: number; lon: number } | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!navigator.geolocation) {
            setError('Geolocation not supported by this browser');
            return;
        }

        navigator.geolocation.getCurrentPosition(
            async (position) => {
                const lat = position.coords.latitude;
                const lon = position.coords.longitude;
                setLocation({ lat, lon });

                try {
                    const nearby: GlobalEventResponse[] = await fetchNearby(lat, lon, 500);
                    setNearbyEvents(nearby);
                    console.log(`[useGeolocation] Found ${nearby.length} nearby events within 500km`);
                } catch (err) {
                    console.error('[useGeolocation] Failed to fetch nearby events:', err);
                }
            },
            (err) => {
                setError(err.message);
                console.warn('[useGeolocation] Location access denied:', err.message);
            }
        );
    }, []);

    return { location, error };
};