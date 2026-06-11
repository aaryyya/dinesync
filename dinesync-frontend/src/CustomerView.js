import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { sendOrder, connectSession } from './websocket';

const API_BASE = process.env.REACT_APP_API_BASE_URL || 'http://localhost:5002';

const MENU_ITEMS = [
    { id: 1, name: 'Butter Chicken', price: 280, emoji: '🍛' },
    { id: 2, name: 'Paneer Tikka',   price: 220, emoji: '🧆' },
    { id: 3, name: 'Garlic Naan',    price: 60,  emoji: '🫓' },
    { id: 4, name: 'Dal Makhani',    price: 180, emoji: '🫕' },
    { id: 5, name: 'Biryani',        price: 320, emoji: '🍲' },
    { id: 6, name: 'Masala Cola',    price: 80,  emoji: '🥤' },
    { id: 7, name: 'Mango Lassi',    price: 90,  emoji: '🥛' },
    { id: 8, name: 'Beer',           price: 150, emoji: '🍺' },
];

const STATUS_STEPS = ['RECEIVED', 'PREPARING', 'SERVED'];
const STATUS_META  = {
    RECEIVED:  { label: '⏳ Sent to kitchen',  color: '#f59e0b', bg: '#fffbeb' },
    PREPARING: { label: '👨‍🍳 Being prepared…',  color: '#3b82f6', bg: '#eff6ff' },
    SERVED:    { label: '✅ Served! Enjoy!',    color: '#22c55e', bg: '#f0fdf4' },
};

// N3 fix: extract default checkin path — derive from tableId state where possible
const checkinPath = (tableId) => tableId ? `/checkin/${tableId}` : '/checkin/1';

const decodeJwtPayload = (token) => {
    try {
        const b64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(atob(b64));
    } catch (e) {
        console.warn('[DineSync] JWT decode failed:', e);
        return null;
    }
};

function ProgressBar({ status }) {
    const currentIdx  = STATUS_STEPS.indexOf(status);
    const activeColor = STATUS_META[status]?.color || '#e5e7eb';
    return (
        <div style={{ display: 'flex', gap: '4px', marginTop: '6px' }}>
            {STATUS_STEPS.map((_, idx) => (
                <div key={idx} style={{
                    flex: 1, height: '6px', borderRadius: '3px',
                    background: idx <= currentIdx ? activeColor : '#e5e7eb',
                    transition: 'background 0.5s ease',
                    boxShadow: idx === currentIdx ? `0 0 6px ${activeColor}` : 'none',
                }} />
            ))}
        </div>
    );
}

/** Phase 4: itemized bill receipt modal */
function BillReceipt({ bill, onDismiss }) {
    return (
        <div style={{
            position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100,
        }}>
            <div style={{
                background: 'white', borderRadius: '16px', padding: '24px',
                maxWidth: '360px', width: '90%', maxHeight: '80vh', overflowY: 'auto',
            }}>
                <h3 style={{ textAlign: 'center', marginTop: 0 }}>🧾 Your Bill</h3>
                <p style={{ textAlign: 'center', color: '#888', fontSize: '0.85rem', marginTop: '-8px' }}>
                    Table {bill.tableId} · {new Date(bill.checkedInAt).toLocaleTimeString()}
                </p>

                <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '12px' }}>
                    <thead>
                        <tr style={{ borderBottom: '2px solid #f1f5f9', color: '#888', fontSize: '0.8rem' }}>
                            <th style={{ textAlign: 'left', padding: '6px 0' }}>Item</th>
                            <th style={{ textAlign: 'right', padding: '6px 0' }}>₹</th>
                        </tr>
                    </thead>
                    <tbody>
                        {bill.items.filter(i => i.status === 'SERVED').map((item) => (
                            <tr key={item.orderId}>
                                <td style={{ padding: '6px 0', fontSize: '0.9rem' }}>{item.itemName}</td>
                                <td style={{ padding: '6px 0', textAlign: 'right' }}>₹{item.price ?? '—'}</td>
                            </tr>
                        ))}
                        {bill.items.filter(i => i.status !== 'SERVED').map((item) => (
                            <tr key={item.orderId} style={{ opacity: 0.5 }}>
                                <td style={{ padding: '6px 0', fontSize: '0.85rem', fontStyle: 'italic' }}>
                                    {item.itemName} <span style={{ color: '#f59e0b', fontSize: '0.75rem' }}>(pending)</span>
                                </td>
                                <td style={{ padding: '6px 0', textAlign: 'right', fontStyle: 'italic', fontSize: '0.85rem' }}>
                                    ₹{item.price ?? '—'}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                <div style={{ borderTop: '2px solid #f1f5f9', marginTop: '12px', paddingTop: '10px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                        <span style={{ color: '#666' }}>Served</span>
                        <span style={{ fontWeight: 600 }}>₹{bill.servedTotal}</span>
                    </div>
                    {bill.pendingTotal > 0 && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', opacity: 0.6, marginBottom: '4px' }}>
                            <span style={{ fontSize: '0.85rem', color: '#666' }}>Pending (not yet billed)</span>
                            <span style={{ fontSize: '0.85rem' }}>₹{bill.pendingTotal}</span>
                        </div>
                    )}
                    <div style={{
                        display: 'flex', justifyContent: 'space-between',
                        fontWeight: 700, fontSize: '1.1rem',
                        borderTop: '1px solid #e5e7eb', paddingTop: '8px', marginTop: '8px',
                    }}>
                        <span>Total Due</span>
                        <span style={{ color: '#6366f1' }}>₹{bill.servedTotal}</span>
                    </div>
                </div>

                <button onClick={onDismiss}
                    style={{
                        marginTop: '16px', width: '100%', padding: '12px',
                        background: '#6366f1', color: 'white', border: 'none',
                        borderRadius: '10px', cursor: 'pointer', fontWeight: 700, fontSize: '1rem',
                    }}>
                    Close
                </button>
            </div>
        </div>
    );
}

function CustomerView() {
    const [sessionToken,     setSessionToken]     = useState(null);
    const [sessionUuid,      setSessionUuid]      = useState(null);
    const [tableId,          setTableId]          = useState(null);
    const [error,            setError]            = useState(null);
    const [sentOrders,       setSentOrders]       = useState([]);
    const [activeTab,        setActiveTab]        = useState('menu');
    const [hasShownPrep,     setHasShownPrep]     = useState(false);

    // Phase 4: billing state
    const [bill,             setBill]             = useState(null);
    const [checkedOut,       setCheckedOut]        = useState(false);
    // W5 fix: separate loading states for the two billing operations
    const [billPreviewLoading, setBillPreviewLoading] = useState(false);
    const [checkoutLoading,    setCheckoutLoading]    = useState(false);

    const navigate = useNavigate();

    // Effect 1: initialise session from localStorage
    useEffect(() => {
        const token = localStorage.getItem('dinesync_session_token');
        const table = localStorage.getItem('dinesync_table_id');
        if (!token) { setError('No active session. Please scan the QR code to check in.'); return; }
        const payload = decodeJwtPayload(token);
        if (!payload?.sessionUuid) { setError('Session token is invalid. Please check in again.'); return; }
        setSessionToken(token);
        setSessionUuid(payload.sessionUuid);
        setTableId(table);
    }, []);

    // Effect 2: subscribe to session WS for live status updates (W2 fix from Phase 3 review)
    useEffect(() => {
        if (!sessionUuid) return;
        const cleanup = connectSession(sessionUuid, (update) => {
            if (update.orderId && update.status) {
                setSentOrders((prev) =>
                    prev.map((o) =>
                        o.orderId === update.orderId ? { ...o, status: update.status } : o
                    )
                );
                // W8 fix (Phase 3): only auto-switch tab on the first PREPARING push
                if (update.status === 'PREPARING' && !hasShownPrep) {
                    setHasShownPrep(true);
                    setActiveTab('orders');
                }
            }
        });
        return cleanup;
    }, [sessionUuid]); // eslint-disable-line react-hooks/exhaustive-deps

    const orderItem = (item) => {
        if (!sessionToken) { setError('No session.'); return; }
        // C1 note: price is sent as convenience for the UI to immediately show locally,
        // but the SERVER ignores it and looks up the canonical price from MENU_PRICES.
        sendOrder({ table: parseInt(tableId), item: item.name, price: item.price, sessionId: sessionToken });
        setSentOrders((prev) => [
            ...prev,
            { tempId: Date.now(), orderId: null, name: item.name, price: item.price,
              time: new Date().toLocaleTimeString(), status: 'RECEIVED' },
        ]);
        setActiveTab('orders');
    };

    // C2 fix: send JWT as Authorization header on all billing requests
    const authHeader = sessionToken ? { 'Authorization': `Bearer ${sessionToken}` } : {};

    // Phase 4: bill preview (W5: own loading state)
    const requestBill = async () => {
        setBillPreviewLoading(true);
        try {
            const res = await axios.get(
                `${API_BASE}/api/sessions/${sessionUuid}/bill`,
                { headers: authHeader }                // C2 fix
            );
            setBill(res.data);
        } catch (e) {
            console.error('Bill fetch failed:', e);
            setError('Could not fetch bill. Please try again or ask staff for help.');
        } finally {
            setBillPreviewLoading(false);
        }
    };

    // Phase 4: checkout (W5: own loading state)
    const doCheckout = async () => {
        setCheckoutLoading(true);
        try {
            const res = await axios.post(
                `${API_BASE}/api/sessions/${sessionUuid}/checkout`,
                {},
                { headers: authHeader }                // C2 fix
            );
            // W4 fix: don't clear localStorage yet — tableId state needed for
            // the receipt dismiss navigation. Clear happens in onDismiss below.
            setBill(res.data);
            setCheckedOut(true);
        } catch (e) {
            console.error('Checkout failed:', e);
            setError('Checkout failed. Please try again or ask staff for help.');
        } finally {
            setCheckoutLoading(false);
        }
    };

    // W4 fix: clear localStorage AFTER the receipt modal is dismissed
    const handleReceiptDismiss = () => {
        setBill(null);
        if (checkedOut) {
            localStorage.removeItem('dinesync_session_token');
            localStorage.removeItem('dinesync_table_id');
            // N3 fix: use tableId from React state (still set), not from localStorage (now cleared)
            navigate(checkinPath(tableId));
        }
    };

    if (error) {
        return (
            <div style={{ textAlign: 'center', marginTop: '60px', fontFamily: 'sans-serif' }}>
                <h2>🍽️ DineSync</h2>
                <p style={{ color: '#ef4444' }}>{error}</p>
                {/* N3 + C4 fix: use tableId from state, not hardcoded */}
                <button onClick={() => navigate(checkinPath(tableId))}
                    style={{ padding: '10px 24px', background: '#6366f1', color: 'white',
                             border: 'none', borderRadius: '8px', cursor: 'pointer' }}>
                    Check In Again
                </button>
            </div>
        );
    }

    return (
        <div style={{ maxWidth: '480px', margin: '0 auto', fontFamily: 'sans-serif', minHeight: '100vh', background: '#fafafa' }}>
            {/* Bill modal — W4: localStorage cleared in onDismiss, not on checkout trigger */}
            {bill && <BillReceipt bill={bill} onDismiss={handleReceiptDismiss} />}

            {/* Header */}
            <div style={{
                background: checkedOut ? '#22c55e' : '#6366f1', color: 'white',
                padding: '16px 20px', position: 'sticky', top: 0, zIndex: 10, transition: 'background 0.3s',
            }}>
                <h2 style={{ margin: 0, fontSize: '1.2rem' }}>🍽️ DineSync — Table {tableId}</h2>
                {checkedOut && <p style={{ margin: '4px 0 0', fontSize: '0.85rem', opacity: 0.9 }}>Session checked out ✓</p>}
            </div>

            {/* Tab bar */}
            <div style={{ display: 'flex', borderBottom: '2px solid #e5e7eb', background: 'white' }}>
                {[
                    { key: 'menu',   label: '🍴 Menu' },
                    { key: 'orders', label: `📋 My Orders${sentOrders.length > 0 ? ` (${sentOrders.length})` : ''}` },
                ].map(({ key, label }) => (
                    <button key={key} onClick={() => setActiveTab(key)}
                        style={{
                            flex: 1, padding: '12px', background: 'none', border: 'none',
                            borderBottom: activeTab === key ? '3px solid #6366f1' : '3px solid transparent',
                            color: activeTab === key ? '#6366f1' : '#666',
                            fontWeight: activeTab === key ? 700 : 400,
                            cursor: 'pointer', fontSize: '0.95rem',
                        }}>
                        {label}
                    </button>
                ))}
            </div>

            {/* Menu Tab */}
            {activeTab === 'menu' && (
                <ul style={{ listStyle: 'none', padding: '12px', margin: 0 }}>
                    {MENU_ITEMS.map((item) => (
                        <li key={item.id} style={{
                            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                            padding: '14px 16px', marginBottom: '8px', background: 'white',
                            borderRadius: '12px', boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
                        }}>
                            <span>
                                <span style={{ fontSize: '1.4rem' }}>{item.emoji}</span>
                                {' '}<strong style={{ fontSize: '0.95rem' }}>{item.name}</strong>
                                <br /><small style={{ color: '#888' }}>₹{item.price}</small>
                            </span>
                            <button onClick={() => orderItem(item)} disabled={checkedOut}
                                style={{
                                    padding: '8px 18px', background: checkedOut ? '#ccc' : '#6366f1',
                                    color: 'white', border: 'none', borderRadius: '8px',
                                    cursor: checkedOut ? 'not-allowed' : 'pointer', fontWeight: 600,
                                }}>
                                + Order
                            </button>
                        </li>
                    ))}
                </ul>
            )}

            {/* Orders Tab */}
            {activeTab === 'orders' && (
                <div style={{ padding: '12px' }}>
                    {sentOrders.length === 0 ? (
                        <p style={{ textAlign: 'center', color: '#888', marginTop: '40px' }}>
                            No orders placed yet. Go to Menu to order!
                        </p>
                    ) : (
                        sentOrders.map((o) => {
                            const meta = STATUS_META[o.status] || STATUS_META.RECEIVED;
                            return (
                                <div key={o.orderId ?? o.tempId} style={{
                                    background: meta.bg, border: `1px solid ${meta.color}40`,
                                    borderRadius: '12px', padding: '14px 16px', marginBottom: '10px',
                                    transition: 'background 0.5s ease',
                                }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <strong style={{ fontSize: '0.95rem' }}>{o.name}</strong>
                                        <small style={{ color: '#888' }}>₹{o.price}</small>
                                    </div>
                                    <div style={{ marginTop: '4px', color: meta.color, fontWeight: 600, fontSize: '0.85rem' }}>
                                        {meta.label}
                                    </div>
                                    <ProgressBar status={o.status} />
                                    <div style={{ fontSize: '0.75rem', color: '#aaa', marginTop: '6px' }}>
                                        ordered at {o.time}{o.orderId ? ` · #${o.orderId}` : ''}
                                    </div>
                                </div>
                            );
                        })
                    )}

                    {/* Phase 4: billing actions (W5: independent loading states) */}
                    {sentOrders.length > 0 && !checkedOut && (
                        <div style={{ marginTop: '20px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                            <button onClick={requestBill} disabled={billPreviewLoading || checkoutLoading}
                                style={{
                                    padding: '12px', background: 'white',
                                    color: '#6366f1', border: '2px solid #6366f1', borderRadius: '10px',
                                    cursor: (billPreviewLoading || checkoutLoading) ? 'not-allowed' : 'pointer',
                                    fontWeight: 600, fontSize: '0.95rem',
                                }}>
                                {billPreviewLoading ? '⏳ Loading bill…' : '🧾 View Bill'}
                            </button>
                            <button onClick={doCheckout} disabled={billPreviewLoading || checkoutLoading}
                                style={{
                                    padding: '12px', background: checkoutLoading ? '#ccc' : '#6366f1',
                                    color: 'white', border: 'none', borderRadius: '10px',
                                    cursor: (billPreviewLoading || checkoutLoading) ? 'not-allowed' : 'pointer',
                                    fontWeight: 700, fontSize: '0.95rem',
                                }}>
                                {checkoutLoading ? '⏳ Processing checkout…' : '✓ Checkout & Pay'}
                            </button>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default CustomerView;