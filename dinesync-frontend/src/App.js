import { Routes, Route, Navigate } from 'react-router-dom';
import CheckIn from './CheckIn';
import CustomerView from './CustomerView';
import KitchenView from './KitchenView';

function App() {
    return (
        <Routes>
            {/* QR code destination: /checkin/:tableId */}
            <Route path="/checkin/:tableId" element={<CheckIn />} />

            {/* Customer menu & ordering (requires prior check-in) */}
            <Route path="/customer" element={<CustomerView />} />

            {/* Kitchen real-time order screen */}
            <Route path="/kitchen" element={<KitchenView />} />

            {/* Default: redirect to table 5 check-in for demo */}
            <Route path="/" element={<Navigate to="/checkin/5" replace />} />
        </Routes>
    );
}

export default App;