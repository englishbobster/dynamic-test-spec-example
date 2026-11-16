package stos.dynamictestspec.cart;

public sealed interface CartCommand {

    record AddItem(Product product) implements CartCommand {
    }
}
