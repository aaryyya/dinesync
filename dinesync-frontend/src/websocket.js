import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

let stompClient = null;

export const connect = (onMessageReceived) => {

    const socket = new SockJS("http://localhost:5002/ws");

    stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,

        onConnect: () => {
            console.log("Connected to WebSocket");

            stompClient.subscribe("/topic/kitchen", (message) => {
                onMessageReceived(JSON.parse(message.body));
            });
        },

        onStompError: (frame) => {
            console.log("Broker error:", frame);
        },

        onWebSocketError: (error) => {
            console.log("WebSocket error:", error);
        }
    });

    stompClient.activate();
};

export const sendOrder = (order) => {

    if (stompClient && stompClient.connected) {
        stompClient.publish({
            destination: "/app/order",
            body: JSON.stringify(order)
        });
    } else {
        console.log("Not connected");
    }
};