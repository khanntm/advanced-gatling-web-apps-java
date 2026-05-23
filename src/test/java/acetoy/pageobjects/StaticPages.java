package acetoy.pageobjects;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class StaticPages {

    public static ChainBuilder homepage =
            exec(
                    http("Load Home Page")
                            .get("/")
                            .check(status().is(200))
                            .check(css("title").is("Ace Toys Online Shop"))
                            .check(css("meta[name='_csrf']", "content").saveAs("csrfToken"))
            );

    public static ChainBuilder ourStory =
            exec(
                    http("Get Our Story Page")
                            .get("/our-story")
                            .check(status().is(200))
                            .check(regex("Our fictional toy store was founded online in \\d{4}").exists())
            );

    public static ChainBuilder getInTouch =
            exec(
                    http("Get In Touch Page")
                            .get("/get-in-touch")
                            .check(status().is(200))
                            .check(substring("we are not actually a real store!"))
            );
}
