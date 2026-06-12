import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

// W7 fix: use environment variable instead of hardcoded localhost
const API_BASE = process.env.REACT_APP_API_BASE_URL || 'http://localhost:5002';

function CheckIn() {
    const { tableId } = useParams();
    const navigate = useNavigate();
    const [status, setStatus] = useState('checking'); // 'checking' | 'success' | 'error'
    const [error, setError] = useState(null);

    useEffect(() => {
        const doCheckIn = async () => {
            try {
                const response = await axios.post(
                    `${API_BASE}/api/sessions/checkin/${tableId}`
                );
                const { sessionToken, tableId: returnedTableId } = response.data;

                // Store JWT and tableId for use by CustomerView
                localStorage.setItem('dinesync_session_token', sessionToken);
                localStorage.setItem('dinesync_table_id', String(returnedTableId));

                setStatus('success');
                setTimeout(() => navigate('/customer'), 1500);
            } catch (err) {
                setError('Check-in failed. Please try again or ask staff for help.');
                setStatus('error');
                console.error('Check-in error:', err);
            }
        };

        doCheckIn();
    }, [tableId, navigate]);

    return (
        <div style={{ textAlign: 'center', marginTop: '60px', fontFamily: 'sans-serif' }}>
            <h2 style={{ fontSize: '2rem', marginBottom: '8px' }}>🍽️ DineSync</h2>
            <h3 style={{ color: '#888', fontWeight: 'normal' }}>Table {tableId}</h3>

            <div style={{ marginTop: '32px', fontSize: '1.1rem' }}>
                {status === 'checking' && <p>⏳ Checking you in…</p>}

                {status === 'success' && (
                    <p style={{ color: '#22c55e' }}>✅ Checked in! Taking you to the menu…</p>
                )}

                {status === 'error' && (
                    <div>
                        <p style={{ color: '#ef4444' }}>{error}</p>
                        <button
                            onClick={() => window.location.reload()}
                            style={{
                                marginTop: '12px', padding: '10px 24px',
                                background: '#6366f1', color: 'white',
                                border: 'none', borderRadius: '8px',
                                cursor: 'pointer', fontSize: '1rem',
                            }}
                        >
                            Retry
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}

export default CheckIn;
