package ayd2.p2b.conference_service_api.unit.feature.enrollment;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.enrollment.application.list.ListUserEnrollmentsUseCase;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUserEnrollmentsUseCaseTest {

  @Mock
  private EnrollmentRepositoryPort enrollmentRepositoryPort;
  @Mock
  private EnrollmentMapper enrollmentMapper;

  private ListUserEnrollmentsUseCase useCase;

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID OTHER_USER_ID = UUID.randomUUID();
  private static final UUID CONGRESS_ID = UUID.randomUUID();
  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final Pageable PAGEABLE = PageRequest.of(0, 20);

  @BeforeEach
  void setUp() {
    useCase = new ListUserEnrollmentsUseCase(enrollmentRepositoryPort, enrollmentMapper);
  }

  @Test
  void self_user_can_list_own_enrollments() {
    Enrollment enrollment = sampleEnrollment(USER_ID);
    EnrollmentResponse response = sampleResponse(USER_ID);

    when(enrollmentRepositoryPort.findByUserId(USER_ID, PAGEABLE))
        .thenReturn(new PageImpl<>(List.of(enrollment)));
    when(enrollmentMapper.toResponse(enrollment)).thenReturn(response);

    EnrollmentRequesterContext requester = requesterFor(USER_ID, Role.PARTICIPANT);
    PageResponse<EnrollmentResponse> result = useCase.execute(USER_ID, PAGEABLE, requester);

    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getTotalItems()).isEqualTo(1);
    assertThat(result.getItems().get(0).getUserId()).isEqualTo(USER_ID);
    verify(enrollmentRepositoryPort).findByUserId(USER_ID, PAGEABLE);
  }

  @Test
  void other_user_returns_403_before_data_leak() {
    EnrollmentRequesterContext requester = requesterFor(OTHER_USER_ID, Role.PARTICIPANT);

    assertThatThrownBy(() -> useCase.execute(USER_ID, PAGEABLE, requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(apiEx.getCode()).isEqualTo("auth.forbidden");
        });

    verifyNoInteractions(enrollmentRepositoryPort);
  }

  @Test
  void returns_empty_page_when_user_has_no_enrollments() {
    when(enrollmentRepositoryPort.findByUserId(USER_ID, PAGEABLE))
        .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0L));

    EnrollmentRequesterContext requester = requesterFor(USER_ID, Role.PARTICIPANT);
    PageResponse<EnrollmentResponse> result = useCase.execute(USER_ID, PAGEABLE, requester);

    assertThat(result.getItems()).isEmpty();
    assertThat(result.getTotalItems()).isZero();
    assertThat(result.getTotalPages()).isZero();
  }

  @Test
  void page_response_shape_matches_spa_contract() {
    Enrollment e1 = sampleEnrollment(USER_ID);
    Enrollment e2 = sampleEnrollment(USER_ID);
    EnrollmentResponse r1 = sampleResponse(USER_ID);
    EnrollmentResponse r2 = sampleResponse(USER_ID);

    when(enrollmentRepositoryPort.findByUserId(USER_ID, PAGEABLE))
        .thenReturn(new PageImpl<>(List.of(e1, e2), PAGEABLE, 2));
    when(enrollmentMapper.toResponse(e1)).thenReturn(r1);
    when(enrollmentMapper.toResponse(e2)).thenReturn(r2);

    EnrollmentRequesterContext requester = requesterFor(USER_ID, Role.PARTICIPANT);
    PageResponse<EnrollmentResponse> result = useCase.execute(USER_ID, PAGEABLE, requester);

    assertThat(result.getItems()).hasSize(2);
    assertThat(result.getPage()).isZero();
    assertThat(result.getSize()).isEqualTo(20);
    assertThat(result.getTotalItems()).isEqualTo(2);
    assertThat(result.getTotalPages()).isEqualTo(1);
  }

  // ──────────────────────────────────────────────────────────────────
  // helpers
  // ──────────────────────────────────────────────────────────────────

  private EnrollmentRequesterContext requesterFor(UUID userId, Role... roles) {
    return EnrollmentRequesterContext.builder()
        .userId(userId)
        .roles(Set.of(roles))
        .accessToken("token")
        .build();
  }

  private Enrollment sampleEnrollment(UUID userId) {
    return Enrollment.builder()
        .id(UUID.randomUUID())
        .congressId(CONGRESS_ID)
        .userId(userId)
        .paymentId(PAYMENT_ID)
        .enrolledAt(Instant.now())
        .paymentDate(LocalDate.of(2026, 6, 15))
        .createdBy(userId)
        .build();
  }

  private EnrollmentResponse sampleResponse(UUID userId) {
    return EnrollmentResponse.builder()
        .id(UUID.randomUUID())
        .congressId(CONGRESS_ID)
        .userId(userId)
        .paymentId(PAYMENT_ID)
        .enrolledAt(Instant.now())
        .paymentDate(LocalDate.of(2026, 6, 15))
        .build();
  }
}
