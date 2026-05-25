package ayd2.p2b.conference_service_api.unit.feature.report.earnings;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.report.application.earnings.EarningsReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsReportResponse;
import ayd2.p2b.conference_service_api.integration.dto.WalletCongressEarningsItem;
import ayd2.p2b.conference_service_api.integration.dto.WalletInstitutionEarningsItem;
import ayd2.p2b.conference_service_api.integration.dto.WalletPlatformEarningsReportResponse;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.WalletReportPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EarningsReportUseCaseTest {

    @Mock
    private WalletReportPort walletReportPort;

    private EarningsReportUseCase useCase;

    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID CONGRESS_ID = UUID.randomUUID();
    private static final UUID REQUESTER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new EarningsReportUseCase(walletReportPort);
    }

    @Test
    void non_system_admin_throws_forbidden_and_wallet_not_called() {
        assertThatThrownBy(() -> useCase.execute(INSTITUTION_ID, null, null, context(Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getCode()).isEqualTo("auth.forbidden");
                });

        verifyNoInteractions(walletReportPort);
    }

    @Test
    void date_from_after_date_to_throws_validation_failed_and_wallet_not_called() {
        assertThatThrownBy(() -> useCase.execute(
                INSTITUTION_ID,
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 1, 1),
                context(Set.of(Role.SYSTEM_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("validation.failed");
                });

        verifyNoInteractions(walletReportPort);
    }

    @Test
    void valid_system_admin_calls_wallet_and_preserves_nested_totals() {
        LocalDate dateFrom = LocalDate.of(2026, 1, 1);
        LocalDate dateTo = LocalDate.of(2026, 12, 31);
        when(walletReportPort.getPlatformEarnings(INSTITUTION_ID, dateFrom, dateTo))
                .thenReturn(walletResponse());

        EarningsReportResponse response = useCase.execute(
                INSTITUTION_ID,
                dateFrom,
                dateTo,
                context(Set.of(Role.SYSTEM_ADMIN))
        );

        verify(walletReportPort).getPlatformEarnings(INSTITUTION_ID, dateFrom, dateTo);
        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getGrandTotalAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.getGrandTotalCommission()).isEqualByComparingTo("120.00");
        assertThat(response.getGrandTotalNet()).isEqualByComparingTo("1080.00");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getInstitutionId()).isEqualTo(INSTITUTION_ID);
        assertThat(response.getItems().get(0).getInstitutionName()).isEqualTo("USAC");
        assertThat(response.getItems().get(0).getInstitutionTotalAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.getItems().get(0).getInstitutionTotalCommission()).isEqualByComparingTo("120.00");
        assertThat(response.getItems().get(0).getInstitutionTotalNet()).isEqualByComparingTo("1080.00");
        assertThat(response.getItems().get(0).getPaymentCount()).isEqualTo(24L);
        assertThat(response.getItems().get(0).getCongresses()).hasSize(1);
        assertThat(response.getItems().get(0).getCongresses().get(0).getCongressId()).isEqualTo(CONGRESS_ID);
        assertThat(response.getItems().get(0).getCongresses().get(0).getCongressName()).isEqualTo("AydConf 2026");
    }

    @Test
    void wallet_unavailable_is_propagated_as_503() {
        when(walletReportPort.getPlatformEarnings(INSTITUTION_ID, null, null))
                .thenThrow(IntegrationExceptions.walletUnavailable("Wallet is down"));

        assertThatThrownBy(() -> useCase.execute(
                INSTITUTION_ID, null, null, context(Set.of(Role.SYSTEM_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiEx.getCode()).isEqualTo("system.integration_error");
                });
    }

    private WalletPlatformEarningsReportResponse walletResponse() {
        return WalletPlatformEarningsReportResponse.builder()
                .items(List.of(WalletInstitutionEarningsItem.builder()
                        .institutionId(INSTITUTION_ID)
                        .institutionName("USAC")
                        .congresses(List.of(WalletCongressEarningsItem.builder()
                                .congressId(CONGRESS_ID)
                                .congressName("AydConf 2026")
                                .totalAmount(new BigDecimal("1200.00"))
                                .commissionAmount(new BigDecimal("120.00"))
                                .netAmount(new BigDecimal("1080.00"))
                                .paymentCount(24L)
                                .build()))
                        .institutionTotalAmount(new BigDecimal("1200.00"))
                        .institutionTotalCommission(new BigDecimal("120.00"))
                        .institutionTotalNet(new BigDecimal("1080.00"))
                        .paymentCount(24L)
                        .build()))
                .totalItems(1)
                .grandTotalAmount(new BigDecimal("1200.00"))
                .grandTotalCommission(new BigDecimal("120.00"))
                .grandTotalNet(new BigDecimal("1080.00"))
                .build();
    }

    private ReportRequesterContext context(Set<Role> roles) {
        return ReportRequesterContext.builder()
                .userId(REQUESTER_ID)
                .roles(roles)
                .accessToken("token")
                .build();
    }
}
