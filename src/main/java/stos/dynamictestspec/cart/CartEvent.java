package stos.dynamictestspec.cart;

public sealed interface CartEvent {

    record CartOpened() implements CartEvent {
    }

    record ItemAdded(Product product) implements CartEvent {
    }
}
