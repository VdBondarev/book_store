package book.store.service.payment;

import book.store.dto.payment.PaymentResponseDto;
import book.store.model.User;
import com.stripe.exception.StripeException;
import java.net.MalformedURLException;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentResponseDto create(User user) throws StripeException, MalformedURLException;

    PaymentResponseDto getPending(User user);

    PaymentResponseDto cancel(User user);

    PaymentResponseDto success(User user);

    List<PaymentResponseDto> getUserPayments(Long userId, Pageable pageable);
}
