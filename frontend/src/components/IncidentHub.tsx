import { useEffect, useState } from 'react';
import { useMapContext } from '../context/MapContext';
import { fetchIncidentDetail, fetchIncidentTimeline, fetchImpactRadius } from '../api';
import type { IncidentDetailResponse, IncidentTimelineDto, IncidentImpactResponse } from '../types';

const STATUS_COLORS: Record<string, string> = {
    DETECTED: '#888888',
    CONFIRMED: '#3399ff',
    ESCALATING: '#ff3232',
    STABLE: '#33cc66',
    RESOLVED: '#aaaaaa',
    ARCHIVED: '#555555',
};

const getConfidenceBadge = (score: number) => {
    if (score >= 67) return { label: 'HIGH', color: '#33cc66' };
    if (score >= 34) return { label: 'MEDIUM', color: '#ffaa00' };
    return { label: 'LOW', color: '#ff3232' };
};

interface IncidentHubProps {
    isMobile: boolean;
}

export const IncidentHub = ({ isMobile }: IncidentHubProps) => {
    const { selectedEvent, setSelectedEvent, mapRef } = useMapContext();

    const [detail, setDetail] = useState<IncidentDetailResponse | null>(null);
    const [timeline, setTimeline] = useState<IncidentTimelineDto[]>([]);
    const [impact, setImpact] = useState<IncidentImpactResponse | null>(null);
    const [loading, setLoading] = useState(false);

    const isOpen = selectedEvent !== null;

    // Pan map to selected event
    useEffect(() => {
        if (!selectedEvent || !mapRef.current) return;
        mapRef.current.flyTo({
            center: [selectedEvent.longitude, selectedEvent.latitude],
            zoom: 6,
            duration: 1000,
        });
    }, [selectedEvent]);

    // Fetch detail, timeline, impact when event selected
    useEffect(() => {
        if (!selectedEvent) {
            setDetail(null);
            setTimeline([]);
            setImpact(null);
            return;
        }

        const load = async () => {
            setLoading(true);
            try {
                const [d, t, i] = await Promise.all([
                    fetchIncidentDetail(selectedEvent.id),
                    fetchIncidentTimeline(selectedEvent.id),
                    fetchImpactRadius(selectedEvent.id),
                ]);
                setDetail(d);
                setTimeline(t);
                setImpact(i);
            } catch (err) {
                console.error('[IncidentHub] Failed to load incident data:', err);
            } finally {
                setLoading(false);
            }
        };

        load();
    }, [selectedEvent]);

    const handleClose = () => setSelectedEvent(null);

    const badge = detail ? getConfidenceBadge(detail.confidence_score) : null;

    return (
        <div style={{
            position: 'absolute',
            top: isMobile ? 'auto' : 48,
            bottom: isMobile ? 0 : 'auto',
            right: 0,
            width: isMobile ? '100%' : 380,
            height: isMobile ? '50vh' : 'calc(100vh - 48px)',
            background: 'rgba(10, 10, 10, 0.92)',
            color: '#fff',
            fontFamily: 'monospace',
            fontSize: 13,
            transform: isOpen ? 'translateX(0)' : 'translateX(100%)',
            transition: 'transform 0.3s ease',
            zIndex: 20,
            display: 'flex',
            flexDirection: 'column',
            overflowY: 'auto',
            borderLeft: '1px solid #333',
        }}>
            {/* Header */}
            <div style={{
                padding: '16px',
                borderBottom: '1px solid #333',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
            }}>
                <span style={{ fontWeight: 'bold', fontSize: 14 }}>
                    {selectedEvent?.title ?? 'Incident Hub'}
                </span>
                <button onClick={handleClose} style={{
                    background: 'none',
                    border: 'none',
                    color: '#aaa',
                    cursor: 'pointer',
                    fontSize: 18,
                }}>✕</button>
            </div>

            {loading && (
                <div style={{ padding: 16, color: '#aaa' }}>Loading...</div>
            )}

            {!loading && detail && (
                <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>

                    {/* Status + Confidence */}
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                        <span style={{
                            padding: '4px 10px',
                            borderRadius: 4,
                            background: STATUS_COLORS[detail.status] ?? '#888',
                            fontSize: 11,
                            fontWeight: 'bold',
                        }}>{detail.status}</span>

                        {badge && (
                            <span style={{
                                padding: '4px 10px',
                                borderRadius: 4,
                                background: badge.color,
                                fontSize: 11,
                                fontWeight: 'bold',
                                color: '#000',
                            }}>{badge.label} CONFIDENCE</span>
                        )}
                    </div>

                    {/* Severity Gauge */}
                    <div>
                        <div style={{ color: '#aaa', marginBottom: 6 }}>
                            SEVERITY INDEX — {detail.severity_index.toFixed(1)}
                        </div>
                        <div style={{
                            height: 8,
                            background: '#333',
                            borderRadius: 4,
                            overflow: 'hidden',
                        }}>
                            <div style={{
                                height: '100%',
                                width: `${detail.severity_index}%`,
                                background: detail.severity_index > 70
                                    ? '#ff3232'
                                    : detail.severity_index > 40
                                        ? '#ffaa00'
                                        : '#33cc66',
                                borderRadius: 4,
                                transition: 'width 0.5s ease',
                            }} />
                        </div>
                    </div>

                    {/* Metadata */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, color: '#ccc' }}>
                        <div>TYPE: <span style={{ color: '#fff' }}>{detail.disaster_type}</span></div>
                        <div>SOURCE: <span style={{ color: '#fff' }}>{detail.source}</span></div>
                        {detail.magnitude && (
                            <div>MAGNITUDE: <span style={{ color: '#fff' }}>{detail.magnitude}</span></div>
                        )}
                        <div>LAT/LON: <span style={{ color: '#fff' }}>{detail.latitude.toFixed(4)}, {detail.longitude.toFixed(4)}</span></div>
                    </div>

                    {/* Impact Radius */}
                    {impact && (
                        <div>
                            <div style={{ color: '#aaa', marginBottom: 8 }}>IMPACT RADIUS</div>
                            <div style={{ marginBottom: 6, color: '#ff8c00' }}>
                                ● Within 50km ({impact.innerRing.length} cities)
                            </div>
                            {impact.innerRing.slice(0, 5).map((c, i) => (
                                <div key={i} style={{ paddingLeft: 12, color: '#ccc', marginBottom: 2 }}>
                                    {c.name}, {c.country} — {c.distanceKm.toFixed(1)}km
                                </div>
                            ))}
                            <div style={{ marginTop: 8, marginBottom: 6, color: '#ffaa00' }}>
                                ● 50–100km ({impact.outerRing.length} cities)
                            </div>
                            {impact.outerRing.slice(0, 5).map((c, i) => (
                                <div key={i} style={{ paddingLeft: 12, color: '#ccc', marginBottom: 2 }}>
                                    {c.name}, {c.country} — {c.distanceKm.toFixed(1)}km
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Timeline */}
                    {timeline.length > 0 && (
                        <div>
                            <div style={{ color: '#aaa', marginBottom: 8 }}>TIMELINE</div>
                            {timeline.map((entry) => (
                                <div key={entry.id} style={{
                                    borderLeft: '2px solid #333',
                                    paddingLeft: 12,
                                    marginBottom: 12,
                                    color: '#ccc',
                                }}>
                                    <div style={{ color: '#fff', marginBottom: 2 }}>{entry.message}</div>
                                    <div style={{ color: '#666', fontSize: 11 }}>
                                        {entry.previous_status && `${entry.previous_status} → `}{entry.new_status}
                                    </div>
                                    <div style={{ color: '#555', fontSize: 11 }}>
                                        {new Date(entry.created_at).toLocaleString()}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                </div>
            )}
        </div>
    );
};