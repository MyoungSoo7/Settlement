package github.lms.lemuel.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.lms.lemuel.payment.domain.Payment;
import github.lms.lemuel.payment.domain.PaymentStatus;
import github.lms.lemuel.payment.domain.exception.InvalidOrderStateException;
import github.lms.lemuel.payment.domain.exception.OrderNotFoundException;
import github.lms.lemuel.payment.domain.exception.PaymentNotFoundException;
import github.lms.lemuel.payment.port.in.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController WebMvc Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreatePaymentPort createPaymentPort;

    @MockitoBean
    private AuthorizePaymentPort authorizePaymentPort;

    @MockitoBean
    private CapturePaymentPort capturePaymentPort;

    @MockitoBean
    private RefundPaymentPort refundPaymentPort;

    @MockitoBean
    private GetPaymentPort getPaymentPort;

    @Test
    @DisplayName("POST /payments - Success: Create payment")
    void shouldCreatePaymentSuccessfully() throws Exception {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(1L);
        request.setPaymentMethod("CARD");

        Payment payment = new Payment(
            1L, 1L, new BigDecimal("10000.00"), BigDecimal.ZERO,
            PaymentStatus.READY, "CARD", null, null, null, null
        );

        given(createPaymentPort.createPayment(any(CreatePaymentCommand.class))).willReturn(payment);

        // When & Then
        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value(1))
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.amount").value(10000.00))
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.paymentMethod").value("CARD"));

        verify(createPaymentPort).createPayment(any(CreatePaymentCommand.class));
    }

    @Test
    @DisplayName("POST /payments - Failure: Order not found")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(999L);
        request.setPaymentMethod("CARD");

        given(createPaymentPort.createPayment(any(CreatePaymentCommand.class)))
            .willThrow(new OrderNotFoundException(999L));

        // When & Then
        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /payments - Failure: Invalid order state")
    void shouldReturn400WhenOrderStateInvalid() throws Exception {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(1L);
        request.setPaymentMethod("CARD");

        given(createPaymentPort.createPayment(any(CreatePaymentCommand.class)))
            .willThrow(new InvalidOrderStateException("Order must be in CREATED status"));

        // When & Then
        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /payments/{id}/authorize - Success")
    void shouldAuthorizePaymentSuccessfully() throws Exception {
        // Given
        Long paymentId = 1L;
        Payment payment = new Payment(
            paymentId, 10L, new BigDecimal("10000.00"), BigDecimal.ZERO,
            PaymentStatus.AUTHORIZED, "CARD", "PG-12345", null, null, null
        );

        given(authorizePaymentPort.authorizePayment(paymentId)).willReturn(payment);

        // When & Then
        mockMvc.perform(patch("/payments/{id}/authorize", paymentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(paymentId))
            .andExpect(jsonPath("$.status").value("AUTHORIZED"))
            .andExpect(jsonPath("$.pgTransactionId").value("PG-12345"));

        verify(authorizePaymentPort).authorizePayment(paymentId);
    }

    @Test
    @DisplayName("PATCH /payments/{id}/authorize - Failure: Payment not found")
    void shouldReturn404WhenPaymentNotFoundForAuthorize() throws Exception {
        // Given
        Long paymentId = 999L;
        given(authorizePaymentPort.authorizePayment(paymentId))
            .willThrow(new PaymentNotFoundException(paymentId));

        // When & Then
        mockMvc.perform(patch("/payments/{id}/authorize", paymentId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /payments/{id}/capture - Success")
    void shouldCapturePaymentSuccessfully() throws Exception {
        // Given
        Long paymentId = 1L;
        Payment payment = new Payment(
            paymentId, 10L, new BigDecimal("10000.00"), BigDecimal.ZERO,
            PaymentStatus.CAPTURED, "CARD", "PG-12345", null, null, null
        );

        given(capturePaymentPort.capturePayment(paymentId)).willReturn(payment);

        // When & Then
        mockMvc.perform(patch("/payments/{id}/capture", paymentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(paymentId))
            .andExpect(jsonPath("$.status").value("CAPTURED"));

        verify(capturePaymentPort).capturePayment(paymentId);
    }

    @Test
    @DisplayName("PATCH /payments/{id}/refund - Success")
    void shouldRefundPaymentSuccessfully() throws Exception {
        // Given
        Long paymentId = 1L;
        Payment payment = new Payment(
            paymentId, 10L, new BigDecimal("10000.00"), BigDecimal.ZERO,
            PaymentStatus.REFUNDED, "CARD", "PG-12345", null, null, null
        );

        given(refundPaymentPort.refundPayment(paymentId)).willReturn(payment);

        // When & Then
        mockMvc.perform(patch("/payments/{id}/refund", paymentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(paymentId))
            .andExpect(jsonPath("$.status").value("REFUNDED"));

        verify(refundPaymentPort).refundPayment(paymentId);
    }

    @Test
    @DisplayName("GET /payments/{id} - Success")
    void shouldGetPaymentSuccessfully() throws Exception {
        // Given
        Long paymentId = 1L;
        Payment payment = new Payment(
            paymentId, 10L, new BigDecimal("10000.00"), BigDecimal.ZERO,
            PaymentStatus.READY, "CARD", null, null, null, null
        );

        given(getPaymentPort.getPayment(paymentId)).willReturn(payment);

        // When & Then
        mockMvc.perform(get("/payments/{id}", paymentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(paymentId))
            .andExpect(jsonPath("$.orderId").value(10))
            .andExpect(jsonPath("$.status").value("READY"));

        verify(getPaymentPort).getPayment(paymentId);
    }

    @Test
    @DisplayName("GET /payments/{id} - Failure: Payment not found")
    void shouldReturn404WhenPaymentNotFoundForGet() throws Exception {
        // Given
        Long paymentId = 999L;
        given(getPaymentPort.getPayment(paymentId))
            .willThrow(new PaymentNotFoundException(paymentId));

        // When & Then
        mockMvc.perform(get("/payments/{id}", paymentId))
            .andExpect(status().isNotFound());
    }
}
