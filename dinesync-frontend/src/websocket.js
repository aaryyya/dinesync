import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const API_BASE = process.env.REACT_APP_API_BASE_URL || 'http://localhost:5002';

// ─────────────────────────────────────────────────────────
// Shared singleton STOMP client
// One connection is shared across all subscribers.
// ─────────────────────────────────────────────────────────
let sharedClient = null;
const pendingSubscriptions = []; // queued while connection is establishing

/** Returns the singleton client, creating and activating it if needed. */
const getClient = () => {
    if (sharedClient) return sharedClient;

    sharedClient = new Client({
        webSocketFactory: () => new SockJS(`${API_BASE}/ws`),
        reconnectDelay: 5000,

        onConnect: () => {
            // Drain any subscriptions that registered before we were connected
            while (pendingSubscriptions.length > 0) {
                const { topic, callback, resolve } = pendingSubscriptions.shift();
                const sub = sharedClient.subscribe(topic, (msg) =>
                    callback(JSON.parse(msg.body))
                );
                resolve(sub);
            }
        },

        onStompError: (frame) => console.error('[DineSync STOMP error]', frame),
        onWebSocketError: (err) => console.error('[DineSync WS error]', err),
    });

    sharedClient.activate();
    return sharedClient;
};

/**
 * Internal helper: registers a topic subscription.
 * If not yet connected, queues it for the onConnect flush.
 * Returns a Promise<StompSubscription> — call .unsubscribe() to clean up.
 */
const registerSubscription = (topic, callback) =>
    new Promise((resolve) => {
        const client = getClient();
        if (client.connected) {
            resolve(client.subscribe(topic, (msg) => callback(JSON.parse(msg.body))));
        } else {
            pendingSubscriptions.push({ topic, callback, resolve });
        }
    });

// ─────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────

/**
 * KitchenView: subscribe to the broadcast kitchen feed.
 * Fixes C5: returns a cleanup function — call it inside useEffect return.
 *
 * Usage:
 *   useEffect(() => {
 *     const cleanup = connectKitchen(order => setOrders(prev => [...prev, order]));
 *     return cleanup;
 *   }, []);
 */
export const connectKitchen = (onOrder) => {
    let subscription = null;
    let unmounted = false;

    registerSubscription('/topic/kitchen', onOrder).then((sub) => {
        subscription = sub;
        if (unmounted) subscription.unsubscribe(); // cleanup raced the promise
    });

    return () => {
        unmounted = true;
        subscription?.unsubscribe();
    };
};

/**
 * CustomerView: subscribe to session-specific order status updates.
 * Used in Phase 3 when kitchen pushes status changes back to the customer's phone.
 * Fixes W5: customers only receive updates for their own session.
 *
 * Usage:
 *   useEffect(() => {
 *     const cleanup = connectSession(sessionUuid, update => handleUpdate(update));
 *     return cleanup;
 *   }, [sessionUuid]);
 */
export const connectSession = (sessionUuid, onUpdate) => {
    let subscription = null;
    let unmounted = false;

    registerSubscription(`/topic/session/${sessionUuid}`, onUpdate).then((sub) => {
        subscription = sub;
        if (unmounted) subscription.unsubscribe();
    });

    return () => {
        unmounted = true;
        subscription?.unsubscribe();
    };
};

/**
 * Send an order payload via STOMP.
 * Note: sessionId (JWT) is sent here but stripped by the server before kitchen broadcast.
 */
export const sendOrder = (order) => {
    const client = getClient();
    if (client.connected) {
        client.publish({
            destination: '/app/order',
            body: JSON.stringify(order),
        });
    } else {
        console.error('[DineSync] WebSocket not ready — order not sent.');
    }
};

/**
 * Graceful full teardown (use if you ever need to fully disconnect).
 * Individual component cleanups use the returned unsubscribe functions instead.
 */
export const disconnect = () => {
    sharedClient?.deactivate();
    sharedClient = null;
};