import { useEffect, useState } from 'react';
import axios from 'axios';
import { connectKitchen } from './websocket';

const API_BASE      = process.env.REACT_APP_API_BASE_URL    || 'http://localhost:5002';
const KITCHEN_SECRET = process.env.REACT_APP_KITCHEN_SECRET || 'dinesync-kitchen-secret-2024';

// C1 fix: all status update calls include this header so the server can reject non-kitchen clients
const kitchenHeaders = { 'X-Kitchen-Secret': KITCHEN_SECRET };

const STATUS_COLORS = {
    RECEIVED:  { bg: '#fef3c7', border: '#f59e0b' },
    PREPARING: { bg: '#dbeafe', border: '#3b82f6' },
    SERVED:    { bg: '#dcfce7', border: '#22c55e' },
};

const STATUS_LABELS = {
    RECEIVED:  '⏳ Received',
    PREPARING: '👨‍🍳 Preparing',
    SERVED:    '✅ Served',
};

function KitchenView() {
    const [orders,   setOrders]   = useState([]);
    const [updating, setUpdating] = useState({});

    useEffect(() => {
        const cleanup = connectKitchen((message) => {
            setOrders((prev) => {
                const existingIdx = prev.findIndex((o) => o.orderId === message.orderId);

                if (existingIdx >= 0) {
                    // Existing order — this is a STATUS UPDATE pushed from server
                    const updated = [...prev];
                    updated[existingIdx] = { ...updated[existingIdx], status: message.status };
                    return updated;
                }

                // New order arriving from a customer
                return [...prev, { ...message, status: message.status || 'RECEIVED' }];
            });
        });

        return cleanup;
    }, []);

    const updateStatus = async (orderId, newStatus) => {
        setUpdating((prev) => ({ ...prev, [orderId]: true }));
        try {
            await axios.put(
                `${API_BASE}/api/orders/${orderId}/status`,
                { status: newStatus },
                { headers: kitchenHeaders }   // C1: kitchen secret header
            );
            // State update arrives via WebSocket — no manual setState needed here
        } catch (err) {
            console.error(`Failed to update order #${orderId}:`, err);
            const msg = err.response?.status === 403
                ? 'Not authorized. Is the kitchen secret configured correctly?'
                : `Failed to update order #${orderId}. Please try again.`;
            alert(msg);
        } finally {
            setUpdating((prev) => ({ ...prev, [orderId]: false }));
        }
    };

    const pendingCount = orders.filter((o) => o.status === 'RECEIVED').length;
    const cookingCount = orders.filter((o) => o.status === 'PREPARING').length;
    const servedCount  = orders.filter((o) => o.status === 'SERVED').length;

    return (
        <div style={{ maxWidth: '800px', margin: '0 auto', padding: '20px', fontFamily: 'sans-serif' }}>
            <h2 style={{ textAlign: 'center', marginBottom: '4px' }}>👨‍🍳 Kitchen Display</h2>

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', marginBottom: '24px' }}>
                <Chip label="Pending"  count={pendingCount}  color="#f59e0b" />
                <Chip label="Cooking"  count={cookingCount}  color="#3b82f6" />
                <Chip label="Served"   count={servedCount}   color="#22c55e" />
            </div>

            {orders.length === 0 ? (
                <p style={{ textAlign: 'center', color: '#888', marginTop: '40px' }}>
                    No orders yet. Waiting…
                </p>
            ) : (
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                        <tr style={{ background: '#f1f5f9', textAlign: 'left' }}>
                            <th style={TH_STYLE}>Order #</th>
                            <th style={TH_STYLE}>Table</th>
                            <th style={TH_STYLE}>Item</th>
                            <th style={TH_STYLE}>Status</th>
                            <th style={TH_STYLE}>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        {orders.map((o, i) => {
                            const colors = STATUS_COLORS[o.status] || { bg: '#fff', border: '#e2e8f0' };
                            const busy   = updating[o.orderId];
                            return (
                                <tr
                                    key={o.orderId ?? i}
                                    style={{
                                        background:   colors.bg,
                                        borderLeft:   `4px solid ${colors.border}`,
                                        borderBottom: '1px solid #e2e8f0',
                                        transition:   'background 0.4s ease',
                                    }}
                                >
                                    <td style={TD_STYLE}><strong>{o.orderId ? `#${o.orderId}` : '—'}</strong></td>
                                    <td style={TD_STYLE}>Table {o.table}</td>
                                    <td style={TD_STYLE}>{o.item}</td>
                                    <td style={TD_STYLE}>{STATUS_LABELS[o.status] || o.status}</td>
                                    <td style={TD_STYLE}>
                                        {o.status === 'RECEIVED' && (
                                            <ActionButton
                                                label="▶ Start Preparing"
                                                color="#3b82f6"
                                                busy={busy}
                                                onClick={() => updateStatus(o.orderId, 'PREPARING')}
                                            />
                                        )}
                                        {o.status === 'PREPARING' && (
                                            <ActionButton
                                                label="✓ Mark Served"
                                                color="#22c55e"
                                                busy={busy}
                                                onClick={() => updateStatus(o.orderId, 'SERVED')}
                                            />
                                        )}
                                        {o.status === 'SERVED' && (
                                            <span style={{ color: '#22c55e', fontSize: '0.85rem' }}>Done ✓</span>
                                        )}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            )}
        </div>
    );
}

// ─── constants ──────────────────────────────────────────────
// N4 fix: UPPERCASE naming signals these are module-level constants
const TH_STYLE = { padding: '10px 12px', fontWeight: 600 };
const TD_STYLE = { padding: '10px 12px', verticalAlign: 'middle' };

// ─── sub-components ─────────────────────────────────────────
function Chip({ label, count, color }) {
    return (
        <div style={{
            background: color + '22', border: `1px solid ${color}`,
            borderRadius: '20px', padding: '4px 14px',
            fontSize: '0.85rem', color: '#333',
        }}>
            <strong style={{ color }}>{count}</strong> {label}
        </div>
    );
}

function ActionButton({ label, color, busy, onClick }) {
    return (
        <button
            onClick={onClick}
            disabled={busy}
            style={{
                padding: '6px 14px',
                background: busy ? '#ccc' : color,
                color: 'white', border: 'none', borderRadius: '6px',
                cursor: busy ? 'not-allowed' : 'pointer',
                fontSize: '0.82rem', fontWeight: 600,
                transition: 'background 0.2s',
            }}
        >
            {busy ? '…' : label}
        </button>
    );
}

export default KitchenView;