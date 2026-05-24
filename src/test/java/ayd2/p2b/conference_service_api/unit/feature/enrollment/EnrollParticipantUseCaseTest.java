package ayd2.p2b.conference_service_api.unit.feature.enrollment;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.enrollment.application.exception.EnrollmentExceptions;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentCongressPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentIdempotencyRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.register.EnrollParticipantResult;
import ayd2.p2b.conference_service_api.feature.enrollment.application.register.EnrollParticipantUseCase;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.Enrollment;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyRecord;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyStatus;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.CongressEnrollmentSummary;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.EnrollmentRequesterContext;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.request.CreateEnrollmentRequest;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.response.EnrollmentResponse;
import ayd2.p2b.conference_service_api.feature.enrollment.mapper.EnrollmentMapper;
import ayd2.p2b.conference_service_api.integration.dto.WalletPaymentRegisterRequest;
import ayd2.p2b.conference_service_api.integration.port.WalletPaymentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollParticipantUseCaseTest {

  @Mock
  private EnrollmentRepositoryPort enrollmentRepositoryPort;
  @Mock
  private EnrollmentIdempotencyRepositoryPort idempotencyRepositoryPort;
  @Mock
  private EnrollmentCongressPort enrollmentCongressPort;
  @Mock
  private WalletPaymentPort walletPaymentPort;
  @Mock
  private EnrollmentMapper enrollmentMapper;

  private EnrollParticipantUseCase useCase;

  private static final UUID CONGRESS_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final UUID ENROLLMENT_ID = UUID.randomUUID();
  private static final String IDEMPOTENCY_KEY = "test-idempotency-key-123";
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2026, 6, 15);
  private static final BigDecimal PRICE = new BigDecimal("150.00");

  @BeforeEach
  void setUp() {
    useCase = new EnrollParticipantUseCase(
        enrollmentRepositoryPort,
        idempotencyRepositoryPort,
        enrollmentCongressPort,
        walletPaymentPort,
        enrollmentMapper);
  }

  @Test
  void non_participant_role_returns_403() {
    EnrollmentRequesterContext requester = requester(Set.of(Role.CONGRESS_ADMIN));
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(PAYMENT_DATE);

    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, request, IDEMPOTENCY_KEY, requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(apiEx.getCode()).isEqualTo("auth.forbidden");
        });
  }

  @Test
  void congress_not_found_returns_404() {
    EnrollmentRequesterContext requester = participantRequester();
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(PAYMENT_DATE);

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.insert(any())).thenReturn(processingRecord());
    when(enrollmentCongressPort.findCongressSummaryById(CONGRESS_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, request, IDEMPOTENCY_KEY, requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
          assertThat(apiEx.getCode()).isEqualTo("resource.not_found");
        });
  }

  @Test
  void wallet_insufficient_funds_marks_idempotency_failed_and_returns_422() {
    EnrollmentRequesterContext requester = participantRequester();
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(PAYMENT_DATE);

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.insert(any())).thenReturn(processingRecord());
    when(enrollmentCongressPort.findCongressSummaryById(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
    when(walletPaymentPort.registerPayment(any(), eq(IDEMPOTENCY_KEY), any()))
        .thenThrow(new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "wallet.insufficient_funds",
            "Insufficient funds"));

    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, request, IDEMPOTENCY_KEY, requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
          assertThat(apiEx.getCode()).isEqualTo("wallet.insufficient_funds");
        });

    verify(idempotencyRepositoryPort).update(
        argThatStatus(EnrollmentIdempotencyStatus.FAILED));
  }

  @Test
  void wallet_integration_error_marks_idempotency_failed_and_returns_503() {
    EnrollmentRequesterContext requester = participantRequester();
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(PAYMENT_DATE);

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.insert(any())).thenReturn(processingRecord());
    when(enrollmentCongressPort.findCongressSummaryById(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
    when(walletPaymentPort.registerPayment(any(), eq(IDEMPOTENCY_KEY), any()))
        .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "system.integration_error",
            "Wallet unavailable"));

    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, request, IDEMPOTENCY_KEY, requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        });

    verify(idempotencyRepositoryPort).update(
        argThatStatus(EnrollmentIdempotencyStatus.FAILED));
  }

  @Test
  void successful_enrollment_persists_enrollment_with_correct_payment_id() {
    EnrollmentRequesterContext requester = participantRequester();
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(PAYMENT_DATE);
    Enrollment savedEnrollment = savedEnrollment();
    EnrollmentResponse expectedResponse = enrollmentResponse();

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.insert(any())).thenReturn(processingRecord());
    when(enrollmentCongressPort.findCongressSummaryById(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
    when(walletPaymentPort.registerPayment(any(), eq(IDEMPOTENCY_KEY), any())).thenReturn(PAYMENT_ID);
    when(enrollmentRepositoryPort.save(any())).thenReturn(savedEnrollment);
    when(enrollmentMapper.toResponse(savedEnrollment)).thenReturn(expectedResponse);
    when(idempotencyRepositoryPort.update(any())).thenReturn(succeededRecord());

    EnrollParticipantResult result = useCase.execute(CONGRESS_ID, request, IDEMPOTENCY_KEY, requester);

    assertThat(result.isReplay()).isFalse();
    assertThat(result.getEnrollment()).isEqualTo(expectedResponse);

    ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
    verify(enrollmentRepositoryPort).save(enrollmentCaptor.capture());
    assertThat(enrollmentCaptor.getValue().getPaymentId()).isEqualTo(PAYMENT_ID);
    assertThat(enrollmentCaptor.getValue().getCreatedBy()).isEqualTo(USER_ID);
  }

  @Test
  void amount_sent_to_wallet_equals_congress_price_not_request_body() {
    EnrollmentRequesterContext requester = participantRequester();
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(PAYMENT_DATE);
    Enrollment savedEnrollment = savedEnrollment();

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.insert(any())).thenReturn(processingRecord());
    when(enrollmentCongressPort.findCongressSummaryById(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
    when(walletPaymentPort.registerPayment(any(), any(), any())).thenReturn(PAYMENT_ID);
    when(enrollmentRepositoryPort.save(any())).thenReturn(savedEnrollment);
    when(enrollmentMapper.toResponse(savedEnrollment)).thenReturn(enrollmentResponse());
    when(idempotencyRepositoryPort.update(any())).thenReturn(succeededRecord());

    useCase.execute(CONGRESS_ID, request, IDEMPOTENCY_KEY, requester);

    ArgumentCaptor<WalletPaymentRegisterRequest> walletCaptor = ArgumentCaptor
        .forClass(WalletPaymentRegisterRequest.class);
    verify(walletPaymentPort).registerPayment(walletCaptor.capture(), any(), any());
    assertThat(walletCaptor.getValue().getAmount()).isEqualByComparingTo(PRICE);
  }

  @Test
  void same_key_same_request_after_success_returns_existing_enrollment_200() {
    EnrollmentRequesterContext requester = participantRequester();
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(PAYMENT_DATE);
    EnrollmentIdempotencyRecord succeededRecord = succeededRecord();
    Enrollment existingEnrollment = savedEnrollment();
    EnrollmentResponse existingResponse = enrollmentResponse();

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(succeededRecord));
    when(enrollmentRepositoryPort.findByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.of(existingEnrollment));
    when(enrollmentMapper.toResponse(existingEnrollment)).thenReturn(existingResponse);

    EnrollParticipantResult result = useCase.execute(CONGRESS_ID, request, IDEMPOTENCY_KEY, requester);

    assertThat(result.isReplay()).isTrue();
    assertThat(result.getEnrollment()).isEqualTo(existingResponse);
    verify(walletPaymentPort, never()).registerPayment(any(), any(), any());
  }

  @Test
  void same_key_different_request_returns_409() {
    EnrollmentRequesterContext requester = participantRequester();
    // Different payment date — hash won't match
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(LocalDate.of(2026, 12, 31));

    // Succeeded record was created with PAYMENT_DATE (different hash)
    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(succeededRecord()));

    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, request, IDEMPOTENCY_KEY, requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
          assertThat(apiEx.getCode()).isEqualTo("resource.conflict");
        });

    verify(walletPaymentPort, never()).registerPayment(any(), any(), any());
  }

  @Test
  void different_key_for_already_enrolled_user_returns_409_before_wallet_call() {
    EnrollmentRequesterContext requester = participantRequester();
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(PAYMENT_DATE);
    String differentKey = "different-key-456";

    when(idempotencyRepositoryPort.findByKey(differentKey)).thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.of(succeededRecord()));

    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, request, differentKey, requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
          assertThat(apiEx.getCode()).isEqualTo("resource.conflict");
        });

    verify(walletPaymentPort, never()).registerPayment(any(), any(), any());
  }

  @Test
  void failed_idempotency_record_with_same_key_returns_422() {
    EnrollmentRequesterContext requester = participantRequester();
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(PAYMENT_DATE);
    EnrollmentIdempotencyRecord failedRecord = failedRecord();

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(failedRecord));

    assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, request, IDEMPOTENCY_KEY, requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
          assertThat(apiEx.getCode()).isEqualTo("idempotency.failed_key_not_reusable");
        });
  }

  @Test
  void created_by_is_set_to_requester_user_id() {
    EnrollmentRequesterContext requester = participantRequester();
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(PAYMENT_DATE);
    Enrollment savedEnrollment = savedEnrollment();

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.insert(any())).thenReturn(processingRecord());
    when(enrollmentCongressPort.findCongressSummaryById(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
    when(walletPaymentPort.registerPayment(any(), any(), any())).thenReturn(PAYMENT_ID);
    when(enrollmentRepositoryPort.save(any())).thenReturn(savedEnrollment);
    when(enrollmentMapper.toResponse(savedEnrollment)).thenReturn(enrollmentResponse());
    when(idempotencyRepositoryPort.update(any())).thenReturn(succeededRecord());

    useCase.execute(CONGRESS_ID, request, IDEMPOTENCY_KEY, requester);

    ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
    verify(enrollmentRepositoryPort).save(captor.capture());
    assertThat(captor.getValue().getCreatedBy()).isEqualTo(USER_ID);
  }

  @Test
  void successful_enrollment_marks_idempotency_succeeded() {
    EnrollmentRequesterContext requester = participantRequester();
    CreateEnrollmentRequest request = new CreateEnrollmentRequest(PAYMENT_DATE);
    Enrollment savedEnrollment = savedEnrollment();

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.empty());
    when(idempotencyRepositoryPort.insert(any())).thenReturn(processingRecord());
    when(enrollmentCongressPort.findCongressSummaryById(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
    when(walletPaymentPort.registerPayment(any(), any(), any())).thenReturn(PAYMENT_ID);
    when(enrollmentRepositoryPort.save(any())).thenReturn(savedEnrollment);
    when(enrollmentMapper.toResponse(savedEnrollment)).thenReturn(enrollmentResponse());
    when(idempotencyRepositoryPort.update(any())).thenReturn(succeededRecord());

    useCase.execute(CONGRESS_ID, request, IDEMPOTENCY_KEY, requester);

    verify(idempotencyRepositoryPort).update(argThatStatus(EnrollmentIdempotencyStatus.SUCCEEDED));
  }

  // --- Helpers ---

  private EnrollmentRequesterContext participantRequester() {
    return requester(Set.of(Role.PARTICIPANT));
  }

  private EnrollmentRequesterContext requester(Set<Role> roles) {
    return EnrollmentRequesterContext.builder()
        .userId(USER_ID)
        .roles(roles)
        .accessToken("test-token")
        .build();
  }

  private CongressEnrollmentSummary congressSummary() {
    return CongressEnrollmentSummary.builder()
        .congressId(CONGRESS_ID)
        .institutionId(UUID.randomUUID())
        .congressName("Test Congress")
        .institutionName("Test Institution")
        .price(PRICE)
        .build();
  }

  private EnrollmentIdempotencyRecord processingRecord() {
    return buildRecord(EnrollmentIdempotencyStatus.PROCESSING, null, null);
  }

  private EnrollmentIdempotencyRecord succeededRecord() {
    return buildRecord(EnrollmentIdempotencyStatus.SUCCEEDED, ENROLLMENT_ID, PAYMENT_ID);
  }

  private EnrollmentIdempotencyRecord failedRecord() {
    return buildRecord(EnrollmentIdempotencyStatus.FAILED, null, null);
  }

  private EnrollmentIdempotencyRecord buildRecord(
      EnrollmentIdempotencyStatus status, UUID enrollmentId, UUID paymentId) {
    return EnrollmentIdempotencyRecord.builder()
        .idempotencyKey(IDEMPOTENCY_KEY)
        .congressId(CONGRESS_ID)
        .userId(USER_ID)
        .paymentDate(PAYMENT_DATE)
        .requestHash(computeTestHash(CONGRESS_ID, USER_ID, PAYMENT_DATE))
        .status(status)
        .enrollmentId(enrollmentId)
        .paymentId(paymentId)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  private String computeTestHash(UUID congressId, UUID userId, LocalDate paymentDate) {
    String raw = congressId.toString() + "|" + userId.toString() + "|" + paymentDate.toString();
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private Enrollment savedEnrollment() {
    return Enrollment.builder()
        .id(ENROLLMENT_ID)
        .congressId(CONGRESS_ID)
        .userId(USER_ID)
        .paymentId(PAYMENT_ID)
        .enrolledAt(Instant.now())
        .paymentDate(PAYMENT_DATE)
        .createdBy(USER_ID)
        .build();
  }

  private EnrollmentResponse enrollmentResponse() {
    return EnrollmentResponse.builder()
        .id(ENROLLMENT_ID)
        .congressId(CONGRESS_ID)
        .userId(USER_ID)
        .paymentId(PAYMENT_ID)
        .enrolledAt(Instant.now())
        .paymentDate(PAYMENT_DATE)
        .build();
  }

  private EnrollmentIdempotencyRecord argThatStatus(EnrollmentIdempotencyStatus expectedStatus) {
    return org.mockito.ArgumentMatchers.argThat(
        record -> record != null && record.getStatus() == expectedStatus);
  }
}
