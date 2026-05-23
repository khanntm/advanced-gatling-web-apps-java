package acetoy.pageobjects;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class Cart {

    public static ChainBuilder addToCart =
            exec(session -> {
                int itemsInBasket = session.getInt("itemsInBasket");
                return session.set("itemsInBasket", ++itemsInBasket);
            })
                    .exec(
                            http("Add Product id = #{id} to cart")
                                    .get("/cart/add/#{id}")
                                    .check(status().in(200, 302))
                    );

    public static ChainBuilder increaseQuantity =
            exec(
                    http("Increase quantity of product id = #{id}")
                            .get("/cart/add/#{id}?cartPage=true")
                            .check(status().in(200, 302))
            );

    public static ChainBuilder decreaseQuantity =
            exec(
                    http("Decrease quantity of product id = #{id}")
                            .get("/cart/remove/#{id}")
                            .check(status().in(200, 302))
            );

    public static ChainBuilder viewCart =
            doIf(session -> !session.getBoolean("customerLoggedIn"))
                    .then(exec(Customer.login))
                    .exec(
                            http("View Cart")
                                    .get("/cart/view")
                                    .check(status().in(200, 302))
                    );

    public static ChainBuilder checkout =
            exec(
                    http("Checkout")
                            .get("/cart/checkout")
                            .check(status().is(200))
                            .check(substring("Order complete!"))
                            .check(substring("Your products are on their way to you now"))
            );
}
