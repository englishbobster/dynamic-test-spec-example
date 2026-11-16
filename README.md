# Handover: scenario specifications spike (toy cart)

Throwaway spike from 2026-11-13. It is not committed anywhere. The goal: learn how to write booking-machine's flow
tests as short, declarative **specifications** (start state, one trigger, expected end state) run as JUnit 5 dynamic
tests, before we touch booking-machine itself. To keep the domain out of the way, it uses a toy shopping cart.

## Install and run

Moved out of booking-machine on 2026-11-16 into this standalone project (package `stos.dynamictestspec`).

1. Open the folder in IntelliJ as a Maven project, or use the command line.
2. Run: `mvn test`. Plain Java 25, JUnit 5 and AssertJ. No Spring, no Docker.
3. To see a failure message, change `status(OPEN)` to `status(EMPTY)` in `specifications/EmptyCartAddItem.java`.

`pom.xml` already has the Surefire setting that shows scenario names in the test reports instead of
`cartScenarios()[1]`:

```xml
<build>
    <plugins>
        <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <statelessTestsetReporter implementation="org.apache.maven.plugin.surefire.extensions.junit5.JUnit5Xml30StatelessReporter">
                    <usePhrasedTestCaseMethodName>true</usePhrasedTestCaseMethodName>
                </statelessTestsetReporter>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Then `grep -o '<testcase name="[^"]*"' target/surefire-reports/TEST-*CartScenarioTest.xml` prints
`cartScenarios() EMPTY[] · adding an item opens the cart`. The Maven console still shows only the index; a small
JUnit listener would be needed for names there.

## Layout

| Path | What it holds |
|---|---|
| `src/main/java/.../cart/` | The toy domain. Ordinary code that knows nothing about tests |
| `src/test/java/.../specifications/` | One class per scenario, data only |
| `src/test/java/.../CartScenarios.java` | What a scenario *is*: records and builders, no test logic |
| `src/test/java/.../CartScenarioTest.java` | What *runs* it: the `@TestFactory`, the runner and the asserts |

## The abstractions

**Domain (`cart/`)**
- `Cart` is built from its event history: `new Cart(events)`, or `Cart.emptyCart()`. `decide(command)` returns the
  events a command produces; `snapshot()` gives the projection.
- `CartEvent` (`CartOpened`, `ItemAdded`) and `CartCommand` (`AddItem`) are sealed interfaces with records inside.
- `CartService.handle(command)` loads the cart, appends the new events and publishes a snapshot after each one.
- `CartEventStore` and `CartProjectionPublisher` are in-memory stand-ins for the ledger and the outbox.
- Shortcut: `decide` always opens a new cart. A second scenario (add to an open cart) would force a state check.

**A specification** is a class with one static method:

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

`given` takes a domain object, `when` a domain command. A start state can also be written as events:
`given(new Cart(List.of(new CartOpened(), new ItemAdded(BOOK))))`.

**`CartScenarios`: describes a scenario.** No JUnit, no AssertJ, no service.
- `Scenario(name, given, when, then)`
- `Expectation` is a sealed interface; each kind of check is a record: `ResultIs`, `StatusIs`, `NewEvents`,
  `Projected`.
- Builders that only create records: `scenario`, `given`, `when`, `then`, `result`, `status`, `newEvents`,
  `projected`.

**`CartScenarioTest`: does everything.**
- `SCENARIOS` lists the specifications; the `@TestFactory` makes one `DynamicTest` each, named start state plus
  scenario name.
- `run` puts the given cart's history into a fresh store, sends the command and collects an `Outcome` (result, cart
  afterwards, new events, published snapshots).
- `verify` is a `switch` over the sealed expectations, with one `assertXxx` method per kind. `assertStatus` has a
  plain-language failure message: "Expected the cart to be EMPTY afterwards, but it was OPEN."

## Why it is shaped like this

- **Specifications depend only on the description, never on the test class.** An earlier version had the
  expectations as lambdas with assertions inside, which made the definitions import from the test.
- **Expectations are records, not lambdas.** The description stays free of logic, and the `switch` over a sealed
  interface won't compile until a new kind of expectation has a check.
- **`@BeforeEach` and `@AfterEach` run once per `@TestFactory`, not per dynamic test.** Anything per scenario (fresh
  store, clock, IDs) happens in `run`.

## How it would map to booking-machine's flow tests

The specifications and `CartScenarios` keep their shape; only the runner changes. The test class becomes a
`@SpringBootTest` that extends `FlowTestInfrastructure`, and `run` talks to the running service:

| Step | Toy | Spring flow test |
|---|---|---|
| given | Copy the cart's events into a new store | Append the start-state events with `BookingEventLedgerService.append` (publishes no projection) |
| when | `service.handle(command)` | Publish to `booking-command`, or set `MutableClock` and publish `TimeTickApplicationEvent` |
| wait | Not needed | Request: wait for its command audit row. Tick: not needed |
| then | Read the in-memory store and publisher | Read `BOOKING_EVENT_LEDGER` (rows after the start state), `OUTBOX`, `COMMAND_AUDIT_LOG` |

Checked in booking-machine's code (master `f437286`):
- Every request that becomes a command leaves an audit row, including `NO_OP` and `VALIDATION_FAILED`
  (`CommandAuditService.executeAndAudit`). So the runner can tell when a "nothing should happen" request is done.
- The tick is synchronous: `BookingTickProcessor` is a plain `@EventListener`.

The Spring runner also has to:
- create new `bookingId`, `accountId` and `productId` per scenario and filter what it reads by them (one context and
  one database are shared; specifications contain no UUIDs);
- set and reset the clock inside `run`.

## Triggers (not in the spike yet)

Same idea as the expectations: the specification says what happens, the runner decides how to deliver it.

```java
sealed interface Trigger {
    record RequestBooking(BookingDraft booking) implements Trigger {}
    record TickAt(Instant time) implements Trigger {}
}
```

```java
switch (trigger) {
    case RequestBooking request -> publishBookingRequest(request);
    case TickAt tick -> {
        mutableClock.set(tick.time());
        applicationEventPublisher.publishEvent(new TimeTickApplicationEvent(tick.time()));
    }
}
```

- Name domain events, not transports. `PubsubMessage(json, attributes)` or `RestCall(...)` triggers would put
  delivery details back into every specification.
- Unreadable or incomplete messages (DLQ cases) test the subscriber, not the booking rules. They probably stay
  ordinary tests.

## Open questions

- Strict expectations: today a scenario checks only what it lists. Should anything it doesn't mention (an extra
  message sent) fail it?
- Soft assertions, to see every mismatch in one run instead of only the first.
- Failure messages should name the scenario, since the console shows only `cartScenarios()[1]`.
- Expected projection: only the fields that matter, or the full snapshot?
- Start states as events can describe a history real inputs never produce. Keep a few ordinary flow tests that prove
  real inputs reach the named start states?
- Run the same specifications against both an in-memory runner (fast) and the Spring runner (real flow)?
- Where does `blackbox-tests/` (PR #15) fit, if at all?

## Suggested next steps

1. Bring back a tick: a `Trigger` with `Command(CartCommand)` and `TickAt(Instant)`, and a second specification for
   "an open cart with no activity for 24 hours is abandoned on the tick".
2. Plain-language messages for the other three asserts, with the scenario name in them.
3. Try one real `BookingTickFlowTest` scenario through a Spring runner.
