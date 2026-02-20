package github.lms.lemuel.settlement.application.service;

import github.lms.lemuel.settlement.application.port.in.CreateDailySettlementsUseCase.CreateSettlementCommand;
import github.lms.lemuel.settlement.application.port.in.CreateDailySettlementsUseCase.CreateSettlementResult;
import github.lms.lemuel.settlement.application.port.out.LoadCapturedPaymentsPort;
import github.lms.lemuel.settlement.application.port.out.LoadCapturedPaymentsPort.CapturedPaymentInfo;
import github.lms.lemuel.settlement.application.port.out.LoadSettlementPort;
import github.lms.lemuel.settlement.application.port.out.PublishSettlementEventPort;
import github.lms.lemuel.settlement.application.port.out.SaveSettlementPort;
import github.lms.lemuel.settlement.domain.Settlement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateDailySettlementsService 단위 테스트")
class CreateDailySettlementsServiceTest {

    @Mock
    private LoadCapturedPaymentsPort loadCapturedPaymentsPort;

    @Mock
    private LoadSettlementPort loadSettlementPort;

    @Mock
    private SaveSettlementPort saveSettlementPort;

    @Mock
    private PublishSettlementEventPort publishSettlementEventPort;

    @InjectMocks
    private CreateDailySettlementsService createDailySettlementsService;

    @Test
    @DisplayName("정산 생성 성공 - 중복 없음")
    void createDailySettlements_Success() {
        // given
        LocalDate targetDate = LocalDate.of(2025, 1, 15);
        CreateSettlementCommand command = new CreateSettlementCommand(targetDate);

        List<CapturedPaymentInfo> payments = List.of(
                new CapturedPaymentInfo(1L, 100L, new BigDecimal("10000"), BigDecimal.ZERO,
                        LocalDateTime.of(2025, 1, 15, 10, 0)),
                new CapturedPaymentInfo(2L, 101L, new BigDecimal("20000"), new BigDecimal("5000"),
                        LocalDateTime.of(2025, 1, 15, 11, 0))
        );

        given(loadCapturedPaymentsPort.findCapturedPaymentsBetween(any(), any())).willReturn(payments);
        given(loadSettlementPort.findByPaymentId(any())).willReturn(Optional.empty());

        Settlement savedSettlement1 = new Settlement();
        savedSettlement1.setId(1L);
        Settlement savedSettlement2 = new Settlement();
        savedSettlement2.setId(2L);

        given(saveSettlementPort.save(any(Settlement.class)))
                .willReturn(savedSettlement1, savedSettlement2);

        // when
        CreateSettlementResult result = createDailySettlementsService.createDailySettlements(command);

        // then
        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.totalPayments()).isEqualTo(2);

        verify(saveSettlementPort, times(2)).save(any(Settlement.class));
        verify(publishSettlementEventPort).publishSettlementCreatedEvent(anyList());
    }

    @Test
    @DisplayName("정산 생성 - 일부 중복 스킵")
    void createDailySettlements_SkipDuplicate() {
        // given
        LocalDate targetDate = LocalDate.of(2025, 1, 15);
        CreateSettlementCommand command = new CreateSettlementCommand(targetDate);

        List<CapturedPaymentInfo> payments = List.of(
                new CapturedPaymentInfo(1L, 100L, new BigDecimal("10000"), BigDecimal.ZERO,
                        LocalDateTime.of(2025, 1, 15, 10, 0)),
                new CapturedPaymentInfo(2L, 101L, new BigDecimal("20000"), BigDecimal.ZERO,
                        LocalDateTime.of(2025, 1, 15, 11, 0))
        );

        given(loadCapturedPaymentsPort.findCapturedPaymentsBetween(any(), any())).willReturn(payments);

        // paymentId=1은 이미 정산 존재
        given(loadSettlementPort.findByPaymentId(1L)).willReturn(Optional.of(new Settlement()));
        given(loadSettlementPort.findByPaymentId(2L)).willReturn(Optional.empty());

        Settlement savedSettlement = new Settlement();
        savedSettlement.setId(2L);
        given(saveSettlementPort.save(any(Settlement.class))).willReturn(savedSettlement);

        // when
        CreateSettlementResult result = createDailySettlementsService.createDailySettlements(command);

        // then
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.totalPayments()).isEqualTo(2);

        verify(saveSettlementPort, times(1)).save(any(Settlement.class));
    }
}
