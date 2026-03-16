import { useMemo } from 'react';
import { ScatterplotLayer } from '@deck.gl/layers';
import type { GlobalEventResponse } from '../types';

const DISASTER_COLORS: Record<string, [number, number, number, number]> = {
    EARTHQUAKE: [255, 140, 0, 160],
    WILDFIRE: [255, 50, 50, 160],
    FLOOD: [30, 144, 255, 160],
    CYCLONE: [180, 0, 255, 160],
};

const DEFAULT_COLOR: [number, number, number, number] = [200, 200, 200, 160];

export const useEventLayer = (
    visibleTypes: Set<string>,
    filteredSnapshot: GlobalEventResponse[],
    onEventClick: (event: GlobalEventResponse) => void
) => {
    const filteredData = useMemo(() =>
        filteredSnapshot.filter(e => visibleTypes.has(e.disaster_type)),
        [filteredSnapshot, visibleTypes]
    );

    const escalatingData = useMemo(() =>
        filteredData.filter(e => e.status === 'ESCALATING'),
        [filteredData]
    );

    const baseLayer = useMemo(() => new ScatterplotLayer<GlobalEventResponse>({
        id: 'events-layer',
        data: filteredData,
        getPosition: (e) => [e.longitude, e.latitude],
        getRadius: (e) => Math.max(20000, e.severity_index * 2000),
        getFillColor: (e) => DISASTER_COLORS[e.disaster_type] ?? DEFAULT_COLOR,
        pickable: true,
        onClick: ({ object }) => {
            if (object) onEventClick(object);
        },
        updateTriggers: {
            getFillColor: [visibleTypes],
            getRadius: [filteredSnapshot],
        },
        radiusUnits: 'meters',
        radiusMinPixels: 4,
        radiusMaxPixels: 40,
    }), [filteredData, visibleTypes]);

    const pulseLayer = useMemo(() => new ScatterplotLayer<GlobalEventResponse>({
        id: 'escalating-pulse-layer',
        data: escalatingData,
        getPosition: (e) => [e.longitude, e.latitude],
        getRadius: (e) => Math.max(30000, e.severity_index * 2500),
        getFillColor: [255, 50, 50, 60],
        pickable: false,
        radiusUnits: 'meters',
        radiusMinPixels: 6,
        radiusMaxPixels: 50,
    }), [escalatingData]);

    return [baseLayer, pulseLayer];
};