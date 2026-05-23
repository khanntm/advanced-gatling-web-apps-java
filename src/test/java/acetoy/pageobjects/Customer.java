package acetoy.pageobjects;

import acetoy.session.UserSession;
import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class Customer {

    public static ChainBuilder login =
            exec(
                    http("Login to Acetoy")
                            .post("/login")
                            .formParam("_csrf", "#{csrfToken}")
                            .formParam("username", "#{username}")
                            .formParam("password", "#{password}")
                            .check(status().in(200, 302))
                            .check(css("meta[name='_csrf']", "content").saveAs("csrfToken"))
                            .check(substring("Logout").exists())
            )
            .exec(UserSession.markLoggedIn);

    public static ChainBuilder logout =
            exec(
                    http("Logout from Acetoy")
                            .post("/logout")
                            .formParam("_csrf", "#{csrfToken}")
                            .check(status().in(200, 302))
                            .check(css("meta[name='_csrf']", "content").saveAs("csrfToken"))
                            .check(substring("Login").exists())
            )
            .exec(UserSession.markLoggedOut);
}
