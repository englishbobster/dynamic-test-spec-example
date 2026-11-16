package stos.dynamictestspec.cart;

import java.util.List;

public final class CartService {

    private final CartEventStore store;
    private final CartProjectionPublisher publisher;

    public CartService(CartEventStore store, CartProjectionPublisher publisher) {
        this.store = store;
        this.publisher = publisher;
    }

    public CartResult handle(CartCommand command) {
        List<CartEvent> newEvents = store.load().decide(command);
        if (newEvents.isEmpty()) {
            return CartResult.REJECTED;
        }
        for (CartEvent event : newEvents) {
            store.append(event);
            publisher.publish(store.load().snapshot());
        }
        return CartResult.SUCCEEDED;
    }
}
