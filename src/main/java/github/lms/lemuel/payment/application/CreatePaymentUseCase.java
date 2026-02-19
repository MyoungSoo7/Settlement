package github.lms.lemuel.payment.application;

import github.lms.lemuel.payment.domain.Payment;
import github.lms.lemuel.payment.domain.exception.InvalidOrderStateException;
import github.lms.lemuel.payment.domain.exception.OrderNotFoundException;
import github.lms.lemuel.payment.port.in.CreatePaymentCommand;
import github.lms.lemuel.payment.port.in.CreatePaymentPort;
import github.lms.lemuel.payment.port.out.LoadOrderPort;
import github.lms.lemuel.payment.port.out.PublishEventPort;
import github.lms.lemuel.payment.port.out.SavePaymentPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreatePaymentUseCase implements CreatePaymentPort {

    private final LoadOrderPort loadOrderPort;
    private final SavePaymentPort savePaymentPort;
    private final PublishEventPort publishEventPort;

    public CreatePaymentUseCase(LoadOrderPort loadOrderPort,
                                SavePaymentPort savePaymentPort,
                                PublishEventPort publishEventPort) {
        this.loadOrderPort = loadOrderPort;
        this.savePaymentPort = savePaymentPort;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public Payment createPayment(CreatePaymentCommand command) {
        // Load order information
        LoadOrderPort.OrderInfo order = loadOrderPort.loadOrder(command.getOrderId());

        if (order == null) {
            throw new OrderNotFoundException(command.getOrderId());
        }

        if (!order.isCreated()) {
            throw new InvalidOrderStateException("Order must be in CREATED status to create payment");
        }

        // Create payment domain entity
        Payment payment = new Payment(
            order.getId(),
            order.getAmount(),
            command.getPaymentMethod()
        );

        // Save and publish event
        Payment savedPayment = savePaymentPort.save(payment);
        publishEventPort.publishPaymentCreated(savedPayment.getId(), savedPayment.getOrderId());

        return savedPayment;
    }
}
