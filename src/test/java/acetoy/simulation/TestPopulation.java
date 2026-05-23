package acetoy.simulation;

import io.gatling.javaapi.core.PopulationBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;

public class TestPopulation {

    public static PopulationBuilder instantUsers =
            TestScenario.defaultLoadTest
                    .injectOpen(
                            nothingFor(Duration.ofSeconds(5)),
                            atOnceUsers(10)
                    );

    public static PopulationBuilder rampUsers =
            TestScenario.defaultLoadTest
                    .injectOpen(
                            nothingFor(Duration.ofSeconds(5)),
                            rampUsers(10).during(Duration.ofSeconds(20))
                    );

    public static PopulationBuilder complexInjection =
            TestScenario.defaultLoadTest
                    .injectOpen(
                            constantUsersPerSec(5).during(Duration.ofSeconds(20)).randomized(),
                            rampUsersPerSec(5).to(10).during(Duration.ofSeconds(20)).randomized()
                    );

    public static PopulationBuilder closedModel =
            TestScenario.highPurchaseLoadTest
                    .injectClosed(
                            constantConcurrentUsers(10).during(Duration.ofSeconds(20)),
                            rampConcurrentUsers(10).to(20).during(Duration.ofSeconds(20))
                    );
}
