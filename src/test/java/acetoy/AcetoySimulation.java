package acetoy;

import acetoy.simulation.TestPopulation;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class AcetoySimulation extends Simulation {

  private static final String BASE_URL = System.getProperty("baseUrl", "https://acetoys.uk");
  private static final String TEST_TYPE = System.getProperty("TEST_TYPE", "INSTANT_USERS");

  private final HttpProtocolBuilder httpProtocol = http
    .baseUrl(BASE_URL)
    .inferHtmlResources(AllowList(), DenyList(".*\\.js", ".*\\.css", ".*\\.gif", ".*\\.jpeg", ".*\\.jpg", ".*\\.ico", ".*\\.woff", ".*\\.woff2", ".*\\.(t|o)tf", ".*\\.png", ".*detectportal\\.firefox\\.com.*"))
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("vi-VN,vi;q=0.9,fr-FR;q=0.8,fr;q=0.7,en-US;q=0.6,en;q=0.5");

  {
    switch (TEST_TYPE) {
      case "INSTANT_USERS":     setUp(TestPopulation.instantUsers).protocols(httpProtocol); break;
      case "RAMP_USERS":        setUp(TestPopulation.rampUsers).protocols(httpProtocol); break;
      case "COMPLEX_INJECTION": setUp(TestPopulation.complexInjection).protocols(httpProtocol); break;
      case "CLOSED_MODEL":      setUp(TestPopulation.closedModel).protocols(httpProtocol); break;
      default:                  setUp(TestPopulation.instantUsers).protocols(httpProtocol); break;
    }
  }
}
