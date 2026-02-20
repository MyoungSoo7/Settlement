package github.lms.lemuel.payment.application.port.in;

import github.lms.lemuel.payment.domain.PaymentDomain;

public interface RefundPaymentPort {
    PaymentDomain refundPayment(Long paymentId);
}
