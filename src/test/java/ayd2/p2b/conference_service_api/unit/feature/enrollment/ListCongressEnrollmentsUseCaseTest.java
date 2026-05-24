package ayd2.p2b.conference_service_api.unit.feature.enrollment;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.enrollment.application.list.ListCongressEnrollmentsUseCase;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentCongressPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.Enrollment;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.CongressEnrollmentSummary;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.EnrollmentRequesterContext;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.response.EnrollmentResponse;
import ayd2.p2b.conference_service_api.feature.enrollment.mapper.EnrollmentMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
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
import java.util.Optional;
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
  private EnrollmentCongressPort enrollmentCongressPort;
  @Mock
  private IamUserLookupPort iamUserLookupPort;
  @Mock
  private EnrollmentMapper enrollmentMapper;

  private ListCongressEnrollmentsUseCase useCase;

  private static final UUID CONGRESS_ID = UUID.randomUUID();
  private static final UUID OWNER_ID = UUID.randomUUID();
  private static final UUID REQUESTER_ID = UUID.randomUUID();
  private static final UUID INSTITUTION_ID = UUID.randomUUID();
  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final Pageable PAGEABLE = PageRequest.of(0, 20);

  @BeforeEach
  void setUp() {
    useCase = new ListCongressEnrollmentsUseCase(
        enrollmentRepositoryPort,
        enrollmentCongressPort,
        iamUserLookupPort,
        enrollmentMapper);
  }

  @Test
  void owner_congress_admin_can_list() {
    Enrollment enrollment = sampleEnrollment();
    EnrollmentResponse response = sampleResponse();
    when(enrollmentCongressPort.findManageableCongressById(CONGRESS_ID)).thenReturn(Optional.of(summary(OWNER_ID)));
    when(enrollmentRepositoryPort.findByCongressId(CONGRESS_ID, PAGEABLE))
        .thenReturn(new PageImpl<>(List.of(enrollment)));
    when(enrollmentMapper.toResponse(enrollment)).thenReturn(response);

    PageResponse<EnrollmentResponse> result = useCase.execute(CONGRESS_ID, PAGEABLE, requester(OWNER_ID, Set.of(Role.CONGRESS_ADMIN)));

    assertThat(result.getItems()).hasSize(1);
    verifyNoInteractions(iamUserLookupPort);
  }

  @Test
  void linked_scoped_congress_admin_can_list() {
    Enrollment enrollment = sampleEnrollment();
    EnrollmentResponse response = sampleResponse();
    when(enrollmentCongressPort.findManageableCongressById(CONGRESS_ID)).thenReturn(Optional.of(summary(OWNER_ID)));
    when(iamUserLookupPort.isCongressAdminLinkedToInstitution(REQUESTER_ID, INSTITUTION_ID, "token")).thenReturn(true);
    when(enrollmentRepositoryPort.findByCongressId(CONGRESS_ID, PAGEABLE))
        .thenReturn(new PageImpl<>(List.of(enrollment)));
    when(enrollmentMapper.toResponse(enrollment)).thenReturn(response);

    PageResponse<EnrollmentResponse> result = useCase.execute(
        CONGRESS_ID,
        PAGEABLE,
        requester(REQUESTER_ID, Set.of(Role.CONGRESS_ADMIN)));

    assertThat(result.getItems()).hasSize(1);
  }

  @Test
  void unrelated_congress_admin_gets_403() {
    when(enrollmentCongressPort.findManageableCongressById(CONGRESS_ID)).thenReturn(Optional.of(summary(OWNER_ID)));
    when(iamUserLookupPort.isCongressAdminLinkedToInstitution(REQUESTER_ID, INSTITUTION_ID, "token")).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, PAGEABLE, requester(REQUESTER_ID, Set.of(Role.CONGRESS_ADMIN))))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(apiEx.getCode()).isEqualTo("auth.forbidden");
        });
  }

  @Test
  void participant_gets_403() {
    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, PAGEABLE, requester(REQUESTER_ID, Set.of(Role.PARTICIPANT))))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

    verifyNoInteractions(enrollmentCongressPort);
    verifyNoInteractions(enrollmentRepositoryPort);
  }

  @Test
  void congress_not_found_returns_404() {
    when(enrollmentCongressPort.findManageableCongressById(CONGRESS_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, PAGEABLE, requester(REQUESTER_ID, Set.of(Role.CONGRESS_ADMIN))))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
          assertThat(apiEx.getCode()).isEqualTo("resource.not_found");
        });

    verifyNoInteractions(iamUserLookupPort);
    verifyNoInteractions(enrollmentRepositoryPort);
  }

  @Test
  void returns_empty_page_when_no_enrollments() {
    when(enrollmentCongressPort.findManageableCongressById(CONGRESS_ID)).thenReturn(Optional.of(summary(OWNER_ID)));
    when(enrollmentRepositoryPort.findByCongressId(CONGRESS_ID, PAGEABLE))
        .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0L));

    PageResponse<EnrollmentResponse> result = useCase.execute(CONGRESS_ID, PAGEABLE, requester(OWNER_ID, Set.of(Role.CONGRESS_ADMIN)));

    assertThat(result.getItems()).isEmpty();
    assertThat(result.getTotalItems()).isZero();
    assertThat(result.getTotalPages()).isZero();
    assertThat(result.getPage()).isZero();
    assertThat(result.getSize()).isEqualTo(20);
    verifyNoInteractions(iamUserLookupPort);
  }

  private EnrollmentRequesterContext requester(UUID userId, Set<Role> roles) {
    return EnrollmentRequesterContext.builder()
        .userId(userId)
        .roles(roles)
        .accessToken("token")
        .build();
  }

  private CongressEnrollmentSummary summary(UUID createdBy) {
    return CongressEnrollmentSummary.builder()
        .congressId(CONGRESS_ID)
        .institutionId(INSTITUTION_ID)
        .createdBy(createdBy)
        .congressName("Congress")
        .institutionName("Institution")
        .price(new java.math.BigDecimal("100.00"))
        .build();
  }

  private Enrollment sampleEnrollment() {
    return Enrollment.builder()
        .id(UUID.randomUUID())
        .congressId(CONGRESS_ID)
        .userId(REQUESTER_ID)
        .paymentId(PAYMENT_ID)
        .enrolledAt(Instant.now())
        .paymentDate(LocalDate.of(2026, 6, 15))
        .createdBy(REQUESTER_ID)
        .build();
  }

  private EnrollmentResponse sampleResponse() {
    return EnrollmentResponse.builder()
        .id(UUID.randomUUID())
        .congressId(CONGRESS_ID)
        .userId(REQUESTER_ID)
        .paymentId(PAYMENT_ID)
        .enrolledAt(Instant.now())
        .paymentDate(LocalDate.of(2026, 6, 15))
        .build();
  }
}
