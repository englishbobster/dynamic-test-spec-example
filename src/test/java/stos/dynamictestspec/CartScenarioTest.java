package stos.dynamictestspec;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import stos.dynamictestspec.CartScenarios.Expectation;
import stos.dynamictestspec.CartScenarios.NewEvents;
import stos.dynamictestspec.CartScenarios.Projected;
import stos.dynamictestspec.CartScenarios.ResultIs;
import stos.dynamictestspec.CartScenarios.Scenario;
import stos.dynamictestspec.CartScenarios.StatusIs;
import stos.dynamictestspec.cart.Cart;
import stos.dynamictestspec.cart.CartEvent;
import stos.dynamictestspec.cart.CartEventStore;
import stos.dynamictestspec.cart.CartProjectionPublisher;
import stos.dynamictestspec.cart.CartResult;
import stos.dynamictestspec.cart.CartService;
import stos.dynamictestspec.cart.CartSnapshot;
import stos.dynamictestspec.specifications.EmptyCartAddItem;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class CartScenarioTest {

    private static final List<Scenario> SCENARIOS = List.of(
            EmptyCartAddItem.specification()
    );

    @TestFactory
    Stream<DynamicTest> cartScenarios() {
        return SCENARIOS.stream().map(CartScenarioTest::toTest);
    }

    private static DynamicTest toTest(Scenario scenario) {
        Cart given = scenario.given();
        return dynamicTest(given.status() + given.items().toString() + " · " + scenario.name(), () -> {
            Outcome actual = run(scenario);
            scenario.then().forEach(expectation -> verify(expectation, actual));
        });
    }

    private static Outcome run(Scenario scenario) {
        CartEventStore store = new CartEventStore();
        scenario.given().history().forEach(store::append);
        CartProjectionPublisher publisher = new CartProjectionPublisher();
        CartService service = new CartService(store, publisher);

        CartResult result = service.handle(scenario.when());

        Cart cart = store.load();
        List<CartEvent> newEvents = cart.history().subList(scenario.given().history().size(), cart.history().size());
        return new Outcome(result, cart, newEvents, publisher.published());
    }

    private static void verify(Expectation expectation, Outcome actual) {
        switch (expectation) {
            case ResultIs expected -> assertResult(expected, actual);
            case StatusIs expected -> assertStatus(expected, actual);
            case NewEvents expected -> assertNewEvents(expected, actual);
            case Projected expected -> assertProjected(expected, actual);
        }
    }

    private static void assertResult(ResultIs expected, Outcome actual) {
        assertThat(actual.result()).as("result").isEqualTo(expected.expected());
    }

    private static void assertStatus(StatusIs expected, Outcome actual) {
        assertThat(actual.cart().status())
                .withFailMessage("Expected the cart to be %s afterwards, but it was %s.",
                        expected.expected(), actual.cart().status())
                .isEqualTo(expected.expected());
    }

    private static void assertNewEvents(NewEvents expected, Outcome actual) {
        assertThat(actual.newEvents())
                .as("new events")
                .extracting(Object::getClass)
                .containsExactlyElementsOf(expected.expected());
    }

    private static void assertProjected(Projected expected, Outcome actual) {
        assertThat(actual.published().getLast().status())
                .as("last projection")
                .isEqualTo(expected.expected());
    }

    private record Outcome(CartResult result, Cart cart, List<CartEvent> newEvents, List<CartSnapshot> published) {
    }
}
