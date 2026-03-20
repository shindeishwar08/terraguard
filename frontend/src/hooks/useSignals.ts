import { useEffect, useState, useCallback } from 'react';
import { fetchSignalTally } from '../api';
import type { SignalTally } from '../types';

const POLL_INTERVAL = 10000; // 10 seconds

export const useSignals = (incidentId: string | null) => {
    const [tally, setTally] = useState<SignalTally>({});
    const [flash, setFlash] = useState<string | null>(null);

    const load = useCallback(async () => {
        if (!incidentId) return;
        try {
            const data = await fetchSignalTally(incidentId);
            setTally(prev => {
                // Detect which key changed for flash animation
                const changed = Object.keys(data).find(
                    k => data[k] !== (prev as Record<string, number>)[k]
                );
                if (changed) {
                    setFlash(changed);
                    setTimeout(() => setFlash(null), 600);
                }
                return data;
            });
        } catch (err) {
            console.error('[useSignals] Failed to fetch signal tally:', err);
        }
    }, [incidentId]);

    useEffect(() => {
        if (!incidentId) {
            setTally({});
            return;
        }
        load();
        const interval = setInterval(load, POLL_INTERVAL);
        return () => clearInterval(interval);
    }, [incidentId]);

    return { tally, flash };
};