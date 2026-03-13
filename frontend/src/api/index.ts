const BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const fetchSnapshot = async (): Promise<string> => {
    const res = await fetch(`${BASE_URL}/api/v1/events/snapshot`);
    if (!res.ok) throw new Error('Failed to fetch snapshot');
    return res.text(); // raw JSON string from Redis
};

export const fetchNearby = async (lat: number, lon: number, radiusKm: number) => {
    const res = await fetch(
        `${BASE_URL}/api/v1/events/nearby?lat=${lat}&lon=${lon}&radiusKm=${radiusKm}`
    );
    if (!res.ok) throw new Error('Failed to fetch nearby events');
    return res.json();
};

export const fetchIncidentDetail = async (id: string) => {
    const res = await fetch(`${BASE_URL}/api/v1/incidents/${id}`);
    if (!res.ok) throw new Error('Failed to fetch incident detail');
    return res.json();
};

export const fetchIncidentTimeline = async (id: string) => {
    const res = await fetch(`${BASE_URL}/api/v1/incidents/${id}/timeline`);
    if (!res.ok) throw new Error('Failed to fetch timeline');
    return res.json();
};

export const fetchSignalTally = async (id: string) => {
    const res = await fetch(`${BASE_URL}/api/v1/incidents/${id}/signals`);
    if (!res.ok) throw new Error('Failed to fetch signals');
    return res.json();
};

export const fetchImpactRadius = async (id: string) => {
    const res = await fetch(`${BASE_URL}/api/v1/incidents/${id}/impact`);
    if (!res.ok) throw new Error('Failed to fetch impact radius');
    return res.json();
};