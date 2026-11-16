# dynamic-test-spec-example

Write behaviour tests as short, declarative **specifications** (a start state, one trigger, the expected end state)
and run each one as a JUnit 5 dynamic test.

The domain is a toy shopping cart, so it stays out of the way. The point is the test structure: the specifications
are plain data, and one runner turns them into tests.

```java
public static Scenario specification() {
    return scenario("adding an item opens the cart",
            given(Cart.emptyCart()),
            when(new AddItem(BOOK)),
            then(result(SUCCEEDED),
                    status(OPEN),
                    newEvents(CartOpened.class, ItemAdded.class),
                    projected(OPEN)));
}
```

## Requirements

- JDK 25
- Nothing else. The Maven wrapper (`./mvnw`) downloads Maven on first use.

## Run the tests

```sh
./mvnw test
```

The console prints the results as a tree, with each scenario by name (from
[maven-surefire-junit5-tree-reporter](https://github.com/fabriciorby/maven-surefire-junit5-tree-reporter), set up in
`pom.xml`):

```
[INFO] ── stos.dynamictestspec.CartScenarioTest - 0.059 s
[INFO]    └─ ┬─ cartScenarios() - 0.033 s
[INFO]       ├─ ✔ EMPTY[] · adding an item opens the cart - 0.031 s
[INFO]       ├─ ✔ OPEN[BOOK] · adding another item keeps the cart open - 0.001 s
[INFO]       └─ ✔ EMPTY[] · removing an item from an empty cart is rejected - 0.001 s
```

Surefire's XML report uses the same names:

```sh
grep -o '<testcase name="[^"]*"' target/surefire-reports/TEST-*CartScenarioTest.xml
# <testcase name="cartScenarios() EMPTY[] · adding an item opens the cart"
```

To see a failure, change `status(OPEN)` to `status(EMPTY)` in `EmptyCartAddItem` (import `CartStatus.EMPTY`) and
run again:

```
Expected the cart to be EMPTY afterwards, but it was OPEN.
```

## Open in IntelliJ IDEA

1. **File → Open**, select this folder (or `pom.xml`) and open it as a Maven project.
2. If IntelliJ asks for a JDK, choose a JDK 25 (**File → Project Structure → Project → SDK**).
3. Run the shared **CartScenarioTest** configuration (from `.run/`), or the green arrow next to the test class.

## Project layout

| Path | What it holds |
|---|---|
| `src/main/java/stos/dynamictestspec/cart/` | The cart domain. Ordinary code that knows nothing about tests |
| `src/test/java/stos/dynamictestspec/specifications/` | One class per scenario, data only (`EmptyCartAddItem`, `OpenCartAddItem`, `EmptyCartRemoveItem`) |
| `src/test/java/stos/dynamictestspec/CartScenarios.java` | What a scenario *is*: records and builder methods, no test logic |
| `src/test/java/stos/dynamictestspec/CartScenarioTest.java` | What *runs* a scenario: the `@TestFactory`, the runner and the assertions |

### The cart domain

- `Cart` is built from its event history (`new Cart(events)` or `Cart.emptyCart()`). `decide(command)` returns the
  events a command produces, and `snapshot()` returns a read model:
  - adding an item to an empty cart opens it first (`CartOpened`, `ItemAdded`); adding to an open cart only adds the
    item;
  - removing an item from an empty cart produces no events. Removing from an open cart is not specified yet, so
    `decide` throws "not yet".
- `CartEvent` (`CartOpened`, `ItemAdded`) and `CartCommand` (`AddItem`, `RemoveItem`) are sealed interfaces with
  records inside.
- `CartService.handle(command)` loads the cart, appends the new events, and publishes a snapshot after each one. It
  returns `SUCCEEDED`, or `REJECTED` when the command produces no events.
- `CartEventStore` and `CartProjectionPublisher` are in-memory stand-ins for an event store and a message publisher.

### A scenario

`CartScenarios` describes a scenario. It has no JUnit, no AssertJ and no service in it.

- `Scenario(name, given, when, then)`
- `Expectation` is a sealed interface. Each kind of check is a record: `ResultIs`, `StatusIs`, `NewEvents`,
  `Projected`, `NothingProjected`.
- `scenario`, `given`, `when`, `then`, `result`, `status`, `newEvents`, `projected` and `nothingProjected` only build
  these records, so a specification reads like a sentence.

A start state can be a ready-made cart or a list of events:

```java
given(new Cart(List.of(new CartOpened(), new ItemAdded(BOOK))))
```

### The runner

`CartScenarioTest` does the work:

1. `SCENARIOS` lists the specifications. The `@TestFactory` makes one `DynamicTest` for each, named after the start
   state and the scenario.
2. `run` copies the start state's events into a fresh store, sends the command, and collects an `Outcome`: the
   result, the cart afterwards, the new events and the published snapshots.
3. `verify` is a `switch` over the sealed `Expectation`, with one assert method for each kind.

## Add a scenario

1. Create a class in `specifications/` with a `public static Scenario specification()` method.
2. Add it to `SCENARIOS` in `CartScenarioTest`.
3. Run the tests.

## Add a kind of expectation

1. Add a record that implements `Expectation` in `CartScenarios`, and a builder method for it.
2. The `switch` in `CartScenarioTest.verify` no longer compiles. Add a case and an assert method for the new record.

## Why it is built this way

- **Specifications depend only on the description, never on the test class.** An earlier version used lambdas with
  assertions inside as expectations, which made every specification import from the test.
- **Expectations are records, not lambdas.** The description holds no logic, and the compiler makes sure every kind
  of expectation has a check.
- **Per-scenario setup happens in `run`.** `@BeforeEach` and `@AfterEach` run once per `@TestFactory`, not once per
  dynamic test, so a fresh store (or clock, or IDs) has to be created inside the runner.
- **The runner is the only part that knows how to talk to the system.** The same specifications could run against
  an in-memory runner (fast) and a runner that drives a real service, for example a `@SpringBootTest`.
- **The scenarios run as a flat list, with no `DynamicContainer`s.** Grouping them by command ("adding an item",
  "removing an item") needs a `switch` over the domain's commands in the runner, so every new command would mean a
  change to the test framework. The test names already say the start state and the scenario.

## Dynamic containers in other domains

`DynamicContainer`s are still worth knowing about if this approach is used elsewhere. They fit when the grouping
comes from the test data or the test setup, not from the domain's own types, for example:

- the same specifications run against several runners (in-memory and a real service), one container per runner;
- specifications loaded from files, one container per file or feature;
- one container per scenario, with one test per expectation, so a failure shows exactly which expectations broke.

IntelliJ shows containers as a nested tree. The Maven console tree reporter (`maven-surefire-junit5-tree-reporter`
2.2.0) does not: it builds its tree from classes and methods only, and prints everything below the
`@TestFactory` method on one level.

## Ideas for next steps

- **Triggers other than commands.** A sealed `Trigger` with, for example, `Command(CartCommand)` and
  `TickAt(Instant)`, so a scenario like "an open cart with no activity for 24 hours is abandoned" can be written the
  same way. Name domain events, not transports: a `PubsubMessage(json)` trigger would put delivery details back into
  every specification.
- **Remove an item from an open cart.** `Cart.decide` still throws "not yet" here. A scenario for it would add an
  `ItemRemoved` event, and raise the question of what happens when the last item goes.
- **Better failure messages.** Plain-language messages for every assert, with the scenario name in them.
- **Soft assertions**, to see every mismatch in one run instead of only the first.
- **Strict expectations.** Today a scenario checks only what it lists. Should anything it doesn't mention, such as an
  extra event, fail it?
