package book.store.util;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

@Component
public class StripeUtil {
    private static final String USD = "usd";
    // Port 8088 is used for local port pointed in .env file.
    // You should change it to the port you are going to use.
    private static final String SUCCESS_URL = "http://localhost:8088/api/payments/success";
    private static final String CANCEL_URL = "http://localhost:8088/api/payments/cancel";
    private static final byte DEFAULT_QUANTITY = 1;
    private static final byte MULTIPLIER = 100;

    public Session createSession(
            Long price,
            String unitName)
            throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setSuccessUrl(SUCCESS_URL)
                .setCancelUrl(CANCEL_URL)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency(USD)
                                        .setUnitAmount(price * MULTIPLIER)
                                        .setProductData(SessionCreateParams.LineItem
                                                .PriceData.ProductData.builder()
                                                .setName(unitName)
                                                .build()
                                        )
                                        .build())
                                .setQuantity((long) DEFAULT_QUANTITY)
                                .build()
                )
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .build();
        return Session.create(params);
    }
}
