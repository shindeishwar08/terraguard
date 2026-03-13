import { useEffect } from 'react';
import { fetchSnapshot } from '../api';
import { useMapContext } from '../context/MapContext';
import type { GlobalEventResponse } from '../types';

const POLL_INTERVAL = 30000; // 30 seconds

export const useSnapshot = () => {
    const { setSnapshot } = useMapContext();

    const load = async () => {
        try {
            const raw = await fetchSnapshot();
            const data: GlobalEventResponse[] = JSON.parse(raw);
            setSnapshot(data);
        } catch (err) {
            console.error('[useSnapshot] Failed to fetch snapshot:', err);
        }
    };

    useEffect(() => {
        load(); // fetch on mount
        const interval = setInterval(load, POLL_INTERVAL);
        return () => clearInterval(interval); // cleanup on unmount
    }, []);
};