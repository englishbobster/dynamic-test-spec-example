package stos.dynamictestspec.cart;

import stos.dynamictestspec.cart.CartCommand.AddItem;
import stos.dynamictestspec.cart.CartEvent.CartOpened;
import stos.dynamictestspec.cart.CartEvent.ItemAdded;

import java.util.ArrayList;
import java.util.List;

public final class Cart {

    private final List<CartEvent> history;
    private final CartStatus status;
    private final List<Product> items;

    public Cart(List<CartEvent> history) {
        CartStatus foldedStatus = CartStatus.EMPTY;
        List<Product> foldedItems = new ArrayList<>();
        for (CartEvent event : history) {
            switch (event) {
                case CartOpened opened -> foldedStatus = CartStatus.OPEN;
                case ItemAdded added -> foldedItems.add(added.product());
            }
        }
        this.history = List.copyOf(history);
        this.status = foldedStatus;
        this.items = List.copyOf(foldedItems);
    }

    public static Cart emptyCart() {
        return new Cart(List.of());
    }

    public List<CartEvent> decide(CartCommand command) {
        return switch (command) {
            case AddItem addItem -> List.of(new CartOpened(), new ItemAdded(addItem.product()));
        };
    }

    public List<CartEvent> history() {
        return history;
    }

    public CartStatus status() {
        return status;
    }

    public List<Product> items() {
        return items;
    }

    public CartSnapshot snapshot() {
        return new CartSnapshot(status, items);
    }
}
