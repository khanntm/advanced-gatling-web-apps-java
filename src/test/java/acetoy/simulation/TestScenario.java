package acetoy.simulation;

import io.gatling.javaapi.core.Choice;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;

public class TestScenario {

    private static final Duration TEST_DURATION =
            Duration.ofSeconds(Long.parseLong(System.getProperty("TEST_DURATION", "60")));

    public static ScenarioBuilder defaultLoadTest =
            scenario("Default Load Test")
                    .during(TEST_DURATION)
                    .on(
                            randomSwitch().on(
                                    Choice.withWeight(60.0, exec(UserJourney.browserStore)),
                                    Choice.withWeight(30.0, exec(UserJourney.abandonBasket)),
                                    Choice.withWeight(10.0, exec(UserJourney.completePurchase))
                            )
                    );

    public static ScenarioBuilder highPurchaseLoadTest =
            scenario("High Purchase Load Test")
                    .during(TEST_DURATION)
                    .on(
                            randomSwitch().on(
                                    Choice.withWeight(10.0, exec(UserJourney.browserStore)),
                                    Choice.withWeight(30.0, exec(UserJourney.abandonBasket)),
                                    Choice.withWeight(60.0, exec(UserJourney.completePurchase))
                            )
                    );
}
