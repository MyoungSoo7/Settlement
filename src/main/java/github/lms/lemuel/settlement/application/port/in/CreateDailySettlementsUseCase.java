package github.lms.lemuel.settlement.application.port.in;

import java.time.LocalDate;

/**
 * 일일 정산 생성 UseCase (Inbound Port)
 */
public interface CreateDailySettlementsUseCase {

    CreateSettlementResult createDailySettlements(CreateSettlementCommand command);

    record CreateSettlementCommand(LocalDate targetDate) {
        public CreateSettlementCommand {
            if (targetDate == null) {
                throw new IllegalArgumentException("Target date is required");
            }
        }
    }

    record CreateSettlementResult(int createdCount, int totalPayments) {}
}
