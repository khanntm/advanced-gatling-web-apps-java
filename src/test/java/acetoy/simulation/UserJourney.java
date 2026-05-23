package acetoy.simulation;

import acetoy.pageobjects.Cart;
import acetoy.pageobjects.Category;
import acetoy.pageobjects.Customer;
import acetoy.pageobjects.Products;
import acetoy.pageobjects.StaticPages;
import acetoy.session.UserSession;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;

public class UserJourney {

    private static final Duration LOW_PAUSE = Duration.ofMillis(1000);
    private static final Duration HIGH_PAUSE = Duration.ofMillis(3000);

    private static final FeederBuilder<String> CATEGORY_FEEDER =
            csv("data/Category.csv").circular();
    private static final FeederBuilder<String> USER_FEEDER =
            csv("data/Users.csv").random();
    private static final FeederBuilder<Object> PRODUCT_FEEDER =
            jsonFile("data/ProductDetails.json").random();

    public static ChainBuilder browserStore =
            exec(UserSession.init)
                    .exec(StaticPages.homepage)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .exec(StaticPages.ourStory)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .exec(StaticPages.getInTouch)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .repeat(3).on(
                            feed(CATEGORY_FEEDER)
                                    .exec(Category.listProducts)
                                    .pause(LOW_PAUSE, HIGH_PAUSE)
                                    .exec(Category.cyclePagesOfProducts)
                                    .pause(LOW_PAUSE, HIGH_PAUSE)
                                    .feed(PRODUCT_FEEDER)
                                    .exec(Products.viewProduct)
                                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    );

    public static ChainBuilder abandonBasket =
            exec(UserSession.init)
                    .exec(StaticPages.homepage)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .feed(CATEGORY_FEEDER)
                    .exec(Category.listProducts)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .feed(PRODUCT_FEEDER)
                    .exec(Products.viewProduct)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .exec(Cart.addToCart);

    public static ChainBuilder completePurchase =
            exec(UserSession.init)
                    .exec(StaticPages.homepage)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .feed(CATEGORY_FEEDER)
                    .exec(Category.listProducts)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .feed(PRODUCT_FEEDER)
                    .exec(Products.viewProduct)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .exec(Cart.addToCart)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .feed(USER_FEEDER)
                    .exec(Customer.login)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .exec(Cart.viewCart)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .exec(Cart.checkout)
                    .pause(LOW_PAUSE, HIGH_PAUSE)
                    .exec(Customer.logout);
}
