import { sendOrder } from "./websocket";

function CustomerView() {

    const orderBeer = () => {
        sendOrder({
            table: 5,
            item: "Beer"
        });
    };

    return (
        <div>
            <h2>Customer View</h2>

            <button onClick={orderBeer}>
                Order Beer
            </button>
        </div>
    );
}

export default CustomerView;