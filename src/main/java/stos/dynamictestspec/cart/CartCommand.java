package stos.dynamictestspec.cart;

public sealed interface CartCommand {

    record AddItem(Product product) implements CartCommand {
    }

    record RemoveItem(Product product) implements CartCommand {
    }
}
