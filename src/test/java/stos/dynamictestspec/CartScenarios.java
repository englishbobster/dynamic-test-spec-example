package stos.dynamictestspec;

import stos.dynamictestspec.cart.Cart;
import stos.dynamictestspec.cart.CartCommand;
import stos.dynamictestspec.cart.CartEvent;
import stos.dynamictestspec.cart.CartResult;
import stos.dynamictestspec.cart.CartStatus;

import java.util.List;

public final class CartScenarios {

    private CartScenarios() {
    }

    public record Scenario(String name, Cart given, CartCommand when, List<Expectation> then) {
    }

    public sealed interface Expectation {
    }

    public record ResultIs(CartResult expected) implements Expectation {
    }

    public record StatusIs(CartStatus expected) implements Expectation {
    }

    public record NewEvents(List<Class<? extends CartEvent>> expected) implements Expectation {
    }

    public record Projected(CartStatus expected) implements Expectation {
    }

    public record NothingProjected() implements Expectation {
    }

    public static Scenario scenario(String name, Cart given, CartCommand when, List<Expectation> then) {
        return new Scenario(name, given, when, then);
    }

    public static Cart given(Cart cart) {
        return cart;
    }

    public static CartCommand when(CartCommand command) {
        return command;
    }

    public static List<Expectation> then(Expectation... expectations) {
        return List.of(expectations);
    }

    public static Expectation result(CartResult expected) {
        return new ResultIs(expected);
    }

    public static Expectation status(CartStatus expected) {
        return new StatusIs(expected);
    }

    @SafeVarargs
    public static Expectation newEvents(Class<? extends CartEvent>... expected) {
        return new NewEvents(List.of(expected));
    }

    public static Expectation projected(CartStatus expected) {
        return new Projected(expected);
    }

    public static Expectation nothingProjected() {
        return new NothingProjected();
    }
}
