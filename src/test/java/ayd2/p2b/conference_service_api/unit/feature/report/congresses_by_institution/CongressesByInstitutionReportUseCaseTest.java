package ayd2.p2b.conference_service_api.unit.feature.report.congresses_by_institution;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.report.application.congresses_by_institution.CongressesByInstitutionReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.congresses_by_institution.port.CongressesByInstitutionQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressByInstitutionItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressesByInstitutionReportResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CongressesByInstitutionReportUseCaseTest {

    @Mock
    private CongressesByInstitutionQueryPort queryPort;

    private CongressesByInstitutionReportUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CongressesByInstitutionReportUseCase(queryPort);
    }

    @Test
    void non_system_admin_throws_403() {
        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        assertThatThrownBy(() -> useCase.execute(null, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(((ApiException) ex).getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void participant_throws_403() {
        ReportRequesterContext ctx = context(Set.of(Role.PARTICIPANT));
        assertThatThrownBy(() -> useCase.execute(null, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void system_admin_with_no_filters_returns_all() {
        UUID instId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        when(queryPort.query(any(), any())).thenReturn(List.of(
                CongressByInstitutionItem.builder()
                        .institutionId(instId)
                        .institutionName("USAC")
                        .congressId(congressId)
                        .congressName("AydConf 2026")
                        .startDate(LocalDate.of(2026, 10, 1))
                        .endDate(LocalDate.of(2026, 10, 5))
                        .location("Guatemala City")
                        .price(new BigDecimal("100.00"))
                        .build()
        ));

        ReportRequesterContext ctx = context(Set.of(Role.SYSTEM_ADMIN));
        CongressesByInstitutionReportResponse response = useCase.execute(null, null, ctx);

        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getInstitutionName()).isEqualTo("USAC");
        assertThat(response.getItems().get(0).getCongressName()).isEqualTo("AydConf 2026");
    }

    @Test
    void system_admin_empty_result_returns_empty_response() {
        when(queryPort.query(any(), any())).thenReturn(List.of());

        ReportRequesterContext ctx = context(Set.of(Role.SYSTEM_ADMIN));
        CongressesByInstitutionReportResponse response = useCase.execute(null, null, ctx);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalItems()).isZero();
    }

    private ReportRequesterContext context(Set<Role> roles) {
        return ReportRequesterContext.builder()
                .userId(USER_ID)
                .roles(roles)
                .accessToken("token")
                .build();
    }
}
