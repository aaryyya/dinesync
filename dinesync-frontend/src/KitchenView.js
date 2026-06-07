import { useEffect, useState } from "react";
import { connect } from "./websocket";

function KitchenView() {

    const [orders, setOrders] = useState([]);

    useEffect(() => {

        connect((order) => {
            setOrders((prev) => [...prev, order]);
        });

    }, []);

    useEffect(() => {
        console.log("KitchenView mounted");

        connect((order) => {
            setOrders((prev) => [...prev, order]);
        });
    }, []);

    return (
        <div>
            <h2>Kitchen View</h2>

            <ul>
                {orders.map((o, i) => (
                    <li key={i}>
                        Table {o.table} : {o.item}
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default KitchenView;