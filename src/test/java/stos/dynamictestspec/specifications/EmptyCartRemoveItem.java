package stos.dynamictestspec.specifications;

import stos.dynamictestspec.CartScenarios.Scenario;
import stos.dynamictestspec.cart.Cart;
import stos.dynamictestspec.cart.CartCommand.RemoveItem;

import static stos.dynamictestspec.CartScenarios.given;
import static stos.dynamictestspec.CartScenarios.newEvents;
import static stos.dynamictestspec.CartScenarios.nothingProjected;
import static stos.dynamictestspec.CartScenarios.result;
import static stos.dynamictestspec.CartScenarios.scenario;
import static stos.dynamictestspec.CartScenarios.status;
import static stos.dynamictestspec.CartScenarios.then;
import static stos.dynamictestspec.CartScenarios.when;
import static stos.dynamictestspec.cart.CartResult.REJECTED;
import static stos.dynamictestspec.cart.CartStatus.EMPTY;
import static stos.dynamictestspec.cart.Product.BOOK;

public final class EmptyCartRemoveItem {

    private EmptyCartRemoveItem() {
    }

    public static Scenario specification() {
        return scenario("removing an item from an empty cart is rejected",
                given(Cart.emptyCart()),
                when(new RemoveItem(BOOK)),
                then(result(REJECTED),
                        status(EMPTY),
                        newEvents(),
                        nothingProjected()));
    }
}
