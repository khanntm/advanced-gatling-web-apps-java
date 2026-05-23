package acetoy.pageobjects;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class Products {

    public static ChainBuilder viewProduct =
            exec(
                    http("View Product: #{name}")
                            .get("/product/#{slug}")
                            .check(status().is(200))
                            .check(substring("#{name}").exists())
            );
}
