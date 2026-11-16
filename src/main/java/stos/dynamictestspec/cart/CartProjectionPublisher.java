package stos.dynamictestspec.cart;

import java.util.ArrayList;
import java.util.List;

public final class CartProjectionPublisher {

    private final List<CartSnapshot> published = new ArrayList<>();

    public void publish(CartSnapshot snapshot) {
        published.add(snapshot);
    }

    public List<CartSnapshot> published() {
        return List.copyOf(published);
    }
}
