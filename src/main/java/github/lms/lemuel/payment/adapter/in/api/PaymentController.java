package github.lms.lemuel.payment.adapter.in.api;

import github.lms.lemuel.payment.adapter.in.dto.PaymentRequest;
import github.lms.lemuel.payment.adapter.in.dto.PaymentResponse;
import github.lms.lemuel.payment.application.port.in.*;
import github.lms.lemuel.payment.domain.PaymentDomain;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Payment API - Maps HTTP requests to use case ports
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final CreatePaymentPort createPaymentPort;
    private final AuthorizePaymentPort authorizePaymentPort;
    private final CapturePaymentPort capturePaymentPort;
    private final RefundPaymentPort refundPaymentPort;
    private final GetPaymentPort getPaymentPort;

    public PaymentController(CreatePaymentPort createPaymentPort,
                             AuthorizePaymentPort authorizePaymentPort,
                             CapturePaymentPort capturePaymentPort,
                             RefundPaymentPort refundPaymentPort,
                             GetPaymentPort getPaymentPort) {
        this.createPaymentPort = createPaymentPort;
        this.authorizePaymentPort = authorizePaymentPort;
        this.capturePaymentPort = capturePaymentPort;
        this.refundPaymentPort = refundPaymentPort;
        this.getPaymentPort = getPaymentPort;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        CreatePaymentCommand command = new CreatePaymentCommand(
            request.getOrderId(),
            request.getPaymentMethod()
        );
        
        PaymentDomain paymentDomain = createPaymentPort.createPayment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PaymentResponse(paymentDomain));
    }

    @PatchMapping("/{id}/authorize")
    public ResponseEntity<PaymentResponse> authorizePayment(@PathVariable Long id) {
        PaymentDomain paymentDomain = authorizePaymentPort.authorizePayment(id);
        return ResponseEntity.ok(new PaymentResponse(paymentDomain));
    }

    @PatchMapping("/{id}/capture")
    public ResponseEntity<PaymentResponse> capturePayment(@PathVariable Long id) {
        PaymentDomain paymentDomain = capturePaymentPort.capturePayment(id);
        return ResponseEntity.ok(new PaymentResponse(paymentDomain));
    }

    @PatchMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long id) {
        PaymentDomain paymentDomain = refundPaymentPort.refundPayment(id);
        return ResponseEntity.ok(new PaymentResponse(paymentDomain));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        PaymentDomain paymentDomain = getPaymentPort.getPayment(id);
        return ResponseEntity.ok(new PaymentResponse(paymentDomain));
    }
}
