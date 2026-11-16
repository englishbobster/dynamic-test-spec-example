package stos.dynamictestspec.cart;

import java.util.ArrayList;
import java.util.List;

public final class CartEventStore {

    private final List<CartEvent> events = new ArrayList<>();

    public void append(CartEvent event) {
        events.add(event);
    }

    public Cart load() {
        return new Cart(events);
    }
}
