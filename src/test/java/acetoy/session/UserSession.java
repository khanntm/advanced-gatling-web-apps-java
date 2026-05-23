package acetoy.session;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class UserSession {

    public static final String CUSTOMER_LOGGED_IN_KEY = "customerLoggedIn";
    public static final String ITEMS_IN_BASKET_KEY = "itemsInBasket";
    public static final String PRODUCTS_LIST_PAGE_NUMBER_KEY = "productsListPageNumber";

    public static ChainBuilder init =
            exec(flushCookieJar())
                    .exec(session -> session
                            .set(PRODUCTS_LIST_PAGE_NUMBER_KEY, 1)
                            .set(CUSTOMER_LOGGED_IN_KEY, false)
                            .set(ITEMS_IN_BASKET_KEY, 0));

    public static ChainBuilder markLoggedIn =
            exec(session -> session.set(CUSTOMER_LOGGED_IN_KEY, true));

    public static ChainBuilder markLoggedOut =
            exec(session -> session.set(CUSTOMER_LOGGED_IN_KEY, false));
}
