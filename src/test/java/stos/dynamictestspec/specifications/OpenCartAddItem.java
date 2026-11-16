package stos.dynamictestspec.specifications;

import stos.dynamictestspec.CartScenarios.Scenario;
import stos.dynamictestspec.cart.Cart;
import stos.dynamictestspec.cart.CartCommand.AddItem;
import stos.dynamictestspec.cart.CartEvent.CartOpened;
import stos.dynamictestspec.cart.CartEvent.ItemAdded;

import java.util.List;

import static stos.dynamictestspec.CartScenarios.given;
import static stos.dynamictestspec.CartScenarios.newEvents;
import static stos.dynamictestspec.CartScenarios.projected;
import static stos.dynamictestspec.CartScenarios.result;
import static stos.dynamictestspec.CartScenarios.scenario;
import static stos.dynamictestspec.CartScenarios.status;
import static stos.dynamictestspec.CartScenarios.then;
import static stos.dynamictestspec.CartScenarios.when;
import static stos.dynamictestspec.cart.CartResult.SUCCEEDED;
import static stos.dynamictestspec.cart.CartStatus.OPEN;
import static stos.dynamictestspec.cart.Product.BOOK;
import static stos.dynamictestspec.cart.Product.PEN;

public final class OpenCartAddItem {

    private OpenCartAddItem() {
    }

    public static Scenario specification() {
        return scenario("adding another item keeps the cart open",
                given(new Cart(List.of(new CartOpened(), new ItemAdded(BOOK)))),
                when(new AddItem(PEN)),
                then(result(SUCCEEDED),
                        status(OPEN),
                        newEvents(ItemAdded.class),
                        projected(OPEN)));
    }
}
