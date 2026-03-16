import { useEffect } from 'react';
import { fetchSnapshot } from '../api';
import { useMapContext } from '../context/MapContext';
import type { GlobalEventResponse } from '../types';

const POLL_INTERVAL = 30000;

export const useSnapshot = () => {
    const { setSnapshot, setIsLoading, setSnapshotError, setLastUpdated } = useMapContext();

    const load = async () => {
        try {
            const raw = await fetchSnapshot();
            const data: GlobalEventResponse[] = JSON.parse(raw);
            setSnapshot(data);
            setSnapshotError(null);
            setLastUpdated(new Date());
        } catch (err) {
            console.error('[useSnapshot] Failed to fetch snapshot:', err);
            setSnapshotError('Data temporarily unavailable');
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        load();
        const interval = setInterval(load, POLL_INTERVAL);
        return () => clearInterval(interval);
    }, []);
};