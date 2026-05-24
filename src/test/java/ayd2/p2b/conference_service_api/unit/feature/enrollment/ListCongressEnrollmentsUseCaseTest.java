package ayd2.p2b.conference_service_api.unit.feature.enrollment;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.enrollment.application.list.ListCongressEnrollmentsUseCase;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.Enrollment;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.EnrollmentRequesterContext;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.response.EnrollmentResponse;
import ayd2.p2b.conference_service_api.feature.enrollment.mapper.EnrollmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCongressEnrollmentsUseCaseTest {

  @Mock
  private EnrollmentRepositoryPort enrollmentRepositoryPort;
  @Mock
  private EnrollmentMapper enrollmentMapper;

  private ListCongressEnrollmentsUseCase useCase;

  private static final UUID CONGRESS_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final Pageable PAGEABLE = PageRequest.of(0, 20);

  @BeforeEach
  void setUp() {
    useCase = new ListCongressEnrollmentsUseCase(
        enrollmentRepositoryPort, enrollmentMapper);
  }

  @Test
  void congress_admin_can_list_enrollments_for_congress() {
    Enrollment enrollment = sampleEnrollment();
    EnrollmentResponse response = sampleResponse();

    when(enrollmentRepositoryPort.findByCongressId(CONGRESS_ID, PAGEABLE))
        .thenReturn(new PageImpl<>(List.of(enrollment)));
    when(enrollmentMapper.toResponse(enrollment)).thenReturn(response);

    EnrollmentRequesterContext requester = requesterFor(Role.CONGRESS_ADMIN);
    PageResponse<EnrollmentResponse> result = useCase.execute(CONGRESS_ID, PAGEABLE, requester);

    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getTotalItems()).isEqualTo(1);
  }

  @Test
  void participant_without_congress_admin_returns_403_before_query() {
    EnrollmentRequesterContext requester = requesterFor(Role.PARTICIPANT);

    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, PAGEABLE, requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(apiEx.getCode()).isEqualTo("auth.forbidden");
        });

    verifyNoInteractions(enrollmentRepositoryPort);
  }

  @Test
  void system_admin_without_congress_admin_returns_403() {
    EnrollmentRequesterContext requester = requesterFor(Role.SYSTEM_ADMIN);

    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, PAGEABLE, requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        });

    verifyNoInteractions(enrollmentRepositoryPort);
  }

  @Test
  void returns_empty_page_when_congress_has_no_enrollments() {
    when(enrollmentRepositoryPort.findByCongressId(CONGRESS_ID, PAGEABLE))
        .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0L));

    EnrollmentRequesterContext requester = requesterFor(Role.CONGRESS_ADMIN);
    PageResponse<EnrollmentResponse> result = useCase.execute(CONGRESS_ID, PAGEABLE, requester);

    assertThat(result.getItems()).isEmpty();
    assertThat(result.getTotalItems()).isZero();
  }

  @Test
  void page_response_shape_matches_spa_contract() {
    Enrollment e1 = sampleEnrollment();
    Enrollment e2 = sampleEnrollment();
    EnrollmentResponse r1 = sampleResponse();
    EnrollmentResponse r2 = sampleResponse();

    when(enrollmentRepositoryPort.findByCongressId(CONGRESS_ID, PAGEABLE))
        .thenReturn(new PageImpl<>(List.of(e1, e2), PAGEABLE, 2));
    when(enrollmentMapper.toResponse(e1)).thenReturn(r1);
    when(enrollmentMapper.toResponse(e2)).thenReturn(r2);

    EnrollmentRequesterContext requester = requesterFor(Role.CONGRESS_ADMIN);
    PageResponse<EnrollmentResponse> result = useCase.execute(CONGRESS_ID, PAGEABLE, requester);

    assertThat(result.getItems()).hasSize(2);
    assertThat(result.getPage()).isZero();
    assertThat(result.getSize()).isEqualTo(20);
    assertThat(result.getTotalItems()).isEqualTo(2);
    assertThat(result.getTotalPages()).isEqualTo(1);
  }

  // ──────────────────────────────────────────────────────────────────
  // helpers
  // ──────────────────────────────────────────────────────────────────

  private EnrollmentRequesterContext requesterFor(Role... roles) {
    return EnrollmentRequesterContext.builder()
        .userId(USER_ID)
        .roles(Set.of(roles))
        .accessToken("token")
        .build();
  }

  private Enrollment sampleEnrollment() {
    return Enrollment.builder()
        .id(UUID.randomUUID())
        .congressId(CONGRESS_ID)
        .userId(USER_ID)
        .paymentId(PAYMENT_ID)
        .enrolledAt(Instant.now())
        .paymentDate(LocalDate.of(2026, 6, 15))
        .createdBy(USER_ID)
        .build();
  }

  private EnrollmentResponse sampleResponse() {
    return EnrollmentResponse.builder()
        .id(UUID.randomUUID())
        .congressId(CONGRESS_ID)
        .userId(USER_ID)
        .paymentId(PAYMENT_ID)
        .enrolledAt(Instant.now())
        .paymentDate(LocalDate.of(2026, 6, 15))
        .build();
  }
}
