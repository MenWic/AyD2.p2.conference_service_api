package ayd2.p2b.conference_service_api.unit.feature.report.earnings_by_congress;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.report.application.earnings_by_congress.EarningsByCongressReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsCongressScopePort;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsByCongressReportResponse;
import ayd2.p2b.conference_service_api.integration.dto.WalletEarningsByCongressItem;
import ayd2.p2b.conference_service_api.integration.dto.WalletEarningsByCongressReportResponse;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EarningsByCongressReportUseCaseTest {

    @Mock
    private ParticipantsCongressScopePort congressScopePort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private WalletReportPort walletReportPort;

    private EarningsByCongressReportUseCase useCase;

    private static final UUID CONGRESS_ID = UUID.randomUUID();
    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID REQUESTER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new EarningsByCongressReportUseCase(congressScopePort, iamUserLookupPort, walletReportPort);
    }

    @Test
    void non_congress_admin_throws_forbidden_and_wallet_not_called() {
        ReportRequesterContext requester = context(Set.of(Role.PARTICIPANT));

        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, null, requester))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getCode()).isEqualTo("auth.forbidden");
                });

        verifyNoInteractions(walletReportPort);
    }

    @Test
    void missing_congress_id_throws_validation_failed_and_wallet_not_called() {
        ReportRequesterContext requester = context(Set.of(Role.CONGRESS_ADMIN));

        assertThatThrownBy(() -> useCase.execute(null, null, null, requester))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("validation.failed");
                });

        verifyNoInteractions(walletReportPort);
    }

    @Test
    void missing_congress_throws_not_found_and_wallet_not_called() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.empty());
        ReportRequesterContext requester = context(Set.of(Role.CONGRESS_ADMIN));

        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, null, requester))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiEx.getCode()).isEqualTo("resource.not_found");
                });

        verifyNoInteractions(walletReportPort);
    }

    @Test
    void unlinked_congress_admin_throws_forbidden_and_wallet_not_called() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(REQUESTER_ID, INSTITUTION_ID, "token"))
                .thenReturn(false);
        ReportRequesterContext requester = context(Set.of(Role.CONGRESS_ADMIN));

        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, null, requester))
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
        ReportRequesterContext requester = context(Set.of(Role.CONGRESS_ADMIN));

        assertThatThrownBy(() -> useCase.execute(
                CONGRESS_ID, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), requester))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("validation.failed");
                });

        verifyNoInteractions(walletReportPort);
        verify(congressScopePort, never()).findCongressSummary(any());
    }

    @Test
    void valid_scoped_congress_admin_calls_wallet_and_preserves_totals() {
        LocalDate dateFrom = LocalDate.of(2026, 1, 1);
        LocalDate dateTo = LocalDate.of(2026, 12, 31);
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(REQUESTER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(walletReportPort.getEarningsByCongress(CONGRESS_ID, INSTITUTION_ID, dateFrom, dateTo))
                .thenReturn(walletResponse());

        EarningsByCongressReportResponse response = useCase.execute(
                CONGRESS_ID, dateFrom, dateTo, context(Set.of(Role.CONGRESS_ADMIN)));

        verify(walletReportPort).getEarningsByCongress(eq(CONGRESS_ID), eq(INSTITUTION_ID), eq(dateFrom), eq(dateTo));
        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getGrandTotalAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.getGrandTotalCommission()).isEqualByComparingTo("120.00");
        assertThat(response.getGrandTotalNet()).isEqualByComparingTo("1080.00");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getCongressId()).isEqualTo(CONGRESS_ID);
        assertThat(response.getItems().get(0).getCongressName()).isEqualTo("AydConf 2026");
        assertThat(response.getItems().get(0).getTotalAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.getItems().get(0).getCommissionAmount()).isEqualByComparingTo("120.00");
        assertThat(response.getItems().get(0).getNetAmount()).isEqualByComparingTo("1080.00");
        assertThat(response.getItems().get(0).getPaymentCount()).isEqualTo(24L);
    }

    @Test
    void wallet_unavailable_is_propagated_as_503() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(REQUESTER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(walletReportPort.getEarningsByCongress(CONGRESS_ID, INSTITUTION_ID, null, null))
                .thenThrow(IntegrationExceptions.walletUnavailable("Wallet is down"));

        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, null, context(Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiEx.getCode()).isEqualTo("system.integration_error");
                });
    }

    private WalletEarningsByCongressReportResponse walletResponse() {
        return WalletEarningsByCongressReportResponse.builder()
                .items(List.of(WalletEarningsByCongressItem.builder()
                        .congressId(CONGRESS_ID)
                        .institutionId(INSTITUTION_ID)
                        .institutionName("USAC")
                        .congressName("AydConf 2026")
                        .totalAmount(new BigDecimal("1200.00"))
                        .commissionAmount(new BigDecimal("120.00"))
                        .netAmount(new BigDecimal("1080.00"))
                        .paymentCount(24L)
                        .build()))
                .totalItems(1)
                .grandTotalAmount(new BigDecimal("1200.00"))
                .grandTotalCommission(new BigDecimal("120.00"))
                .grandTotalNet(new BigDecimal("1080.00"))
                .build();
    }

    private CongressInstitutionSummary congressSummary() {
        return CongressInstitutionSummary.builder()
                .congressId(CONGRESS_ID)
                .institutionId(INSTITUTION_ID)
                .congressName("AydConf 2026")
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
