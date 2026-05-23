package acetoy.pageobjects;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class Category {

    public static ChainBuilder listProducts =
            exec(
                    http("Get Category: #{categoryName}")
                            .get("/category/#{categorySlug}")
                            .check(status().is(200))
                            .check(css("#CategoryName").isEL("#{categoryName}"))
            );

    public static ChainBuilder cyclePagesOfProducts =
            exec(session -> session.set("nextPageNumber", session.getInt("productsListPageNumber") + 1))
                    .asLongAs(session -> session.getInt("productsListPageNumber") < session.getInt("categoryPages")).on(
                            exec(
                                    http("Load page #{nextPageNumber} of Products - Category: #{categoryName}")
                                            .get("/category/#{categorySlug}?page=#{productsListPageNumber}")
                                            .check(status().is(200))
                                            .check(css("#CategoryName").isEL("#{categoryName}"))
                                            .check(css(".page-item.active").isEL("#{nextPageNumber}"))
                            )
                                    .exec(session -> {
                                        int current = session.getInt("productsListPageNumber") + 1;
                                        return session.set("productsListPageNumber", current).set("nextPageNumber", current + 1);
                                    })
                    );
}
