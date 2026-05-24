package ayd2.p2b.conference_service_api.unit.feature.enrollment;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentCongressPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentIdempotencyRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.register.EnrollParticipantResult;
import ayd2.p2b.conference_service_api.feature.enrollment.application.register.EnrollParticipantUseCase;
import ayd2.p2b.conference_service_api.feature.enrollment.application.register.EnrollmentRegistrationTransactions;
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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
import static org.mockito.Mockito.verifyNoInteractions;
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
  @Mock
  private EnrollmentRegistrationTransactions enrollmentRegistrationTransactions;

  private EnrollParticipantUseCase useCase;

  private static final UUID CONGRESS_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID INSTITUTION_ID = UUID.randomUUID();
  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final UUID ENROLLMENT_ID = UUID.randomUUID();
  private static final String IDEMPOTENCY_KEY = "idem-key-123";
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2026, 6, 15);
  private static final BigDecimal AMOUNT = new BigDecimal("150.00");

  @BeforeEach
  void setUp() {
    useCase = new EnrollParticipantUseCase(
        enrollmentRepositoryPort,
        idempotencyRepositoryPort,
        enrollmentCongressPort,
        walletPaymentPort,
        enrollmentMapper,
        enrollmentRegistrationTransactions);
  }

  @Test
  void nonParticipantReturns403() {
    assertThatThrownBy(() -> useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        requester(Set.of(Role.CONGRESS_ADMIN))))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(apiEx.getCode()).isEqualTo("auth.forbidden");
        });
  }

  @Test
  void newEnrollmentCreatesProcessingSnapshotAndUsesWalletPayloadForHash() {
    EnrollmentIdempotencyRecord processing = processingRecord(IDEMPOTENCY_KEY, null);
    EnrollmentIdempotencyRecord processingWithPayment = processing.toBuilder().paymentId(PAYMENT_ID).build();
    Enrollment enrolled = enrollment();
    EnrollmentResponse response = enrollmentResponse();

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(enrollmentCongressPort.findPublicEnrollmentCongressById(CONGRESS_ID)).thenReturn(Optional.of(summary()));
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID)).thenReturn(Optional.empty());
    when(enrollmentRegistrationTransactions.createProcessingRecord(any())).thenReturn(processing);
    when(walletPaymentPort.registerPayment(any(), eq(IDEMPOTENCY_KEY), eq("token"))).thenReturn(PAYMENT_ID);
    when(enrollmentRegistrationTransactions.recordWalletPayment(processing, PAYMENT_ID)).thenReturn(processingWithPayment);
    when(enrollmentRegistrationTransactions.completeSuccessfulEnrollment(any(), any(), eq(PAYMENT_ID)))
        .thenReturn(enrolled);
    when(enrollmentMapper.toResponse(enrolled)).thenReturn(response);

    EnrollParticipantResult result = useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        participantRequester());

    assertThat(result.isReplay()).isFalse();
    assertThat(result.getEnrollment()).isEqualTo(response);

    ArgumentCaptor<EnrollmentIdempotencyRecord> recordCaptor = ArgumentCaptor.forClass(EnrollmentIdempotencyRecord.class);
    verify(enrollmentRegistrationTransactions).createProcessingRecord(recordCaptor.capture());
    EnrollmentIdempotencyRecord created = recordCaptor.getValue();
    assertThat(created.getInstitutionId()).isEqualTo(INSTITUTION_ID);
    assertThat(created.getCongressNameSnapshot()).isEqualTo("Congress Name");
    assertThat(created.getInstitutionNameSnapshot()).isEqualTo("Institution Name");
    assertThat(created.getAmount()).isEqualByComparingTo(AMOUNT);
    assertThat(created.getRequestHash()).isEqualTo(expectedHashFromSnapshot(created));

    ArgumentCaptor<WalletPaymentRegisterRequest> walletCaptor = ArgumentCaptor.forClass(WalletPaymentRegisterRequest.class);
    verify(walletPaymentPort).registerPayment(walletCaptor.capture(), eq(IDEMPOTENCY_KEY), eq("token"));
    WalletPaymentRegisterRequest walletRequest = walletCaptor.getValue();
    assertThat(walletRequest.getCongressId()).isEqualTo(CONGRESS_ID);
    assertThat(walletRequest.getUserId()).isEqualTo(USER_ID);
    assertThat(walletRequest.getInstitutionId()).isEqualTo(INSTITUTION_ID);
    assertThat(walletRequest.getAmount()).isEqualByComparingTo(AMOUNT);
    assertThat(walletRequest.getCongressNameSnapshot()).isEqualTo("Congress Name");
    assertThat(walletRequest.getInstitutionNameSnapshot()).isEqualTo("Institution Name");
  }

  @Test
  void succeededReplayReturnsWithoutWalletCall() {
    EnrollmentIdempotencyRecord succeeded = succeededRecord(IDEMPOTENCY_KEY, PAYMENT_ID);
    Enrollment existing = enrollment();
    EnrollmentResponse response = enrollmentResponse();
    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(succeeded));
    when(enrollmentRepositoryPort.findByCongressIdAndUserId(CONGRESS_ID, USER_ID)).thenReturn(Optional.of(existing));
    when(enrollmentMapper.toResponse(existing)).thenReturn(response);

    EnrollParticipantResult result = useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        participantRequester());

    assertThat(result.isReplay()).isTrue();
    assertThat(result.getEnrollment()).isEqualTo(response);
    verifyNoInteractions(walletPaymentPort);
  }

  @Test
  void failedRecordSameRequestReturns422AndNoWalletCall() {
    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(failedRecord(IDEMPOTENCY_KEY)));

    assertThatThrownBy(() -> useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        participantRequester()))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
          assertThat(apiEx.getCode()).isEqualTo("idempotency.failed_key_not_reusable");
        });

    verifyNoInteractions(walletPaymentPort);
  }

  @Test
  void sameKeyDifferentCongressOrUserOrPaymentDateReturns409() {
    EnrollmentIdempotencyRecord record = processingRecord(IDEMPOTENCY_KEY, null);
    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(record));

    assertThatThrownBy(() -> useCase.execute(
        UUID.randomUUID(),
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        participantRequester()))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

    EnrollmentRequesterContext otherUser = EnrollmentRequesterContext.builder()
        .userId(UUID.randomUUID())
        .roles(Set.of(Role.PARTICIPANT))
        .accessToken("token")
        .build();
    assertThatThrownBy(() -> useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        otherUser))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

    assertThatThrownBy(() -> useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(LocalDate.of(2027, 1, 1)),
        IDEMPOTENCY_KEY,
        participantRequester()))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  void processingWithPaymentRecoversLocallyWithoutWalletCall() {
    EnrollmentIdempotencyRecord record = processingRecord(IDEMPOTENCY_KEY, PAYMENT_ID);
    Enrollment enrolled = enrollment();
    EnrollmentResponse response = enrollmentResponse();
    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(record));
    when(enrollmentRegistrationTransactions.completeSuccessfulEnrollment(any(), any(), eq(PAYMENT_ID)))
        .thenReturn(enrolled);
    when(enrollmentMapper.toResponse(enrolled)).thenReturn(response);

    EnrollParticipantResult result = useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        participantRequester());

    assertThat(result.isReplay()).isTrue();
    assertThat(result.getEnrollment()).isEqualTo(response);
    verifyNoInteractions(walletPaymentPort);
  }

  @Test
  void processingWithoutPaymentRetriesWalletUsingPersistedSnapshot() {
    EnrollmentIdempotencyRecord record = processingRecord(IDEMPOTENCY_KEY, null);
    EnrollmentIdempotencyRecord recordWithPayment = record.toBuilder().paymentId(PAYMENT_ID).build();
    Enrollment enrolled = enrollment();
    EnrollmentResponse response = enrollmentResponse();

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(record));
    when(walletPaymentPort.registerPayment(any(), eq(IDEMPOTENCY_KEY), eq("token"))).thenReturn(PAYMENT_ID);
    when(enrollmentRegistrationTransactions.recordWalletPayment(record, PAYMENT_ID)).thenReturn(recordWithPayment);
    when(enrollmentRegistrationTransactions.completeSuccessfulEnrollment(any(), any(), eq(PAYMENT_ID)))
        .thenReturn(enrolled);
    when(enrollmentMapper.toResponse(enrolled)).thenReturn(response);

    EnrollParticipantResult result = useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        participantRequester());

    assertThat(result.isReplay()).isTrue();
    assertThat(result.getEnrollment()).isEqualTo(response);
    verifyNoInteractions(enrollmentCongressPort);
  }

  @Test
  void insufficientFundsMarksFailed() {
    EnrollmentIdempotencyRecord processing = processingRecord(IDEMPOTENCY_KEY, null);
    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(enrollmentCongressPort.findPublicEnrollmentCongressById(CONGRESS_ID)).thenReturn(Optional.of(summary()));
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID)).thenReturn(Optional.empty());
    when(enrollmentRegistrationTransactions.createProcessingRecord(any())).thenReturn(processing);
    when(walletPaymentPort.registerPayment(any(), eq(IDEMPOTENCY_KEY), eq("token")))
        .thenThrow(new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "wallet.insufficient_funds", "Insufficient funds"));

    assertThatThrownBy(() -> useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        participantRequester()))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("wallet.insufficient_funds"));

    verify(enrollmentRegistrationTransactions).markFailed(processing);
  }

  @Test
  void walletConflictKeepsProcessingWithoutFailedMark() {
    EnrollmentIdempotencyRecord processing = processingRecord(IDEMPOTENCY_KEY, null);
    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(enrollmentCongressPort.findPublicEnrollmentCongressById(CONGRESS_ID)).thenReturn(Optional.of(summary()));
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID)).thenReturn(Optional.empty());
    when(enrollmentRegistrationTransactions.createProcessingRecord(any())).thenReturn(processing);
    when(walletPaymentPort.registerPayment(any(), eq(IDEMPOTENCY_KEY), eq("token")))
        .thenThrow(new ApiException(HttpStatus.CONFLICT, "resource.conflict", "conflict"));

    assertThatThrownBy(() -> useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        participantRequester()))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

    verify(enrollmentRegistrationTransactions, never()).markFailed(any());
  }

  @Test
  void ambiguousWalletErrorKeepsProcessingWithoutFailedMark() {
    EnrollmentIdempotencyRecord processing = processingRecord(IDEMPOTENCY_KEY, null);
    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(enrollmentCongressPort.findPublicEnrollmentCongressById(CONGRESS_ID)).thenReturn(Optional.of(summary()));
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID)).thenReturn(Optional.empty());
    when(enrollmentRegistrationTransactions.createProcessingRecord(any())).thenReturn(processing);
    when(walletPaymentPort.registerPayment(any(), eq(IDEMPOTENCY_KEY), eq("token")))
        .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "system.integration_error", "down"));

    assertThatThrownBy(() -> useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        participantRequester()))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

    verify(enrollmentRegistrationTransactions, never()).markFailed(any());
  }

  @Test
  void concurrentProcessingInsertRecoversByExistingKeyAndReturnsReplay() {
    EnrollmentIdempotencyRecord succeeded = succeededRecord(IDEMPOTENCY_KEY, PAYMENT_ID);
    Enrollment existing = enrollment();
    EnrollmentResponse response = enrollmentResponse();

    when(idempotencyRepositoryPort.findByKey(IDEMPOTENCY_KEY))
        .thenReturn(Optional.empty(), Optional.of(succeeded));
    when(enrollmentCongressPort.findPublicEnrollmentCongressById(CONGRESS_ID)).thenReturn(Optional.of(summary()));
    when(idempotencyRepositoryPort.findActiveByCongressIdAndUserId(CONGRESS_ID, USER_ID)).thenReturn(Optional.empty());
    when(enrollmentRegistrationTransactions.createProcessingRecord(any()))
        .thenThrow(new ApiException(HttpStatus.CONFLICT, "resource.conflict", "duplicate active key"));
    when(enrollmentRepositoryPort.findByCongressIdAndUserId(CONGRESS_ID, USER_ID)).thenReturn(Optional.of(existing));
    when(enrollmentMapper.toResponse(existing)).thenReturn(response);

    EnrollParticipantResult result = useCase.execute(
        CONGRESS_ID,
        new CreateEnrollmentRequest(PAYMENT_DATE),
        IDEMPOTENCY_KEY,
        participantRequester());

    assertThat(result.isReplay()).isTrue();
    assertThat(result.getEnrollment()).isEqualTo(response);
    verifyNoInteractions(walletPaymentPort);
  }

  private EnrollmentRequesterContext participantRequester() {
    return requester(Set.of(Role.PARTICIPANT));
  }

  private EnrollmentRequesterContext requester(Set<Role> roles) {
    return EnrollmentRequesterContext.builder()
        .userId(USER_ID)
        .roles(roles)
        .accessToken("token")
        .build();
  }

  private CongressEnrollmentSummary summary() {
    return CongressEnrollmentSummary.builder()
        .congressId(CONGRESS_ID)
        .institutionId(INSTITUTION_ID)
        .createdBy(UUID.randomUUID())
        .congressName("  Congress Name  ")
        .institutionName(" Institution Name ")
        .price(AMOUNT)
        .build();
  }

  private EnrollmentIdempotencyRecord processingRecord(String key, UUID paymentId) {
    return EnrollmentIdempotencyRecord.builder()
        .idempotencyKey(key)
        .congressId(CONGRESS_ID)
        .userId(USER_ID)
        .paymentDate(PAYMENT_DATE)
        .requestHash("hash")
        .status(EnrollmentIdempotencyStatus.PROCESSING)
        .paymentId(paymentId)
        .institutionId(INSTITUTION_ID)
        .congressNameSnapshot("Congress Name")
        .institutionNameSnapshot("Institution Name")
        .amount(AMOUNT)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  private EnrollmentIdempotencyRecord succeededRecord(String key, UUID paymentId) {
    return processingRecord(key, paymentId).toBuilder()
        .status(EnrollmentIdempotencyStatus.SUCCEEDED)
        .enrollmentId(ENROLLMENT_ID)
        .build();
  }

  private EnrollmentIdempotencyRecord failedRecord(String key) {
    return processingRecord(key, null).toBuilder()
        .status(EnrollmentIdempotencyStatus.FAILED)
        .build();
  }

  private Enrollment enrollment() {
    return Enrollment.builder()
        .id(ENROLLMENT_ID)
        .congressId(CONGRESS_ID)
        .userId(USER_ID)
        .paymentId(PAYMENT_ID)
        .paymentDate(PAYMENT_DATE)
        .enrolledAt(Instant.now())
        .createdBy(USER_ID)
        .build();
  }

  private EnrollmentResponse enrollmentResponse() {
    return EnrollmentResponse.builder()
        .id(ENROLLMENT_ID)
        .congressId(CONGRESS_ID)
        .userId(USER_ID)
        .paymentId(PAYMENT_ID)
        .paymentDate(PAYMENT_DATE)
        .enrolledAt(Instant.now())
        .build();
  }

  private String expectedHashFromSnapshot(EnrollmentIdempotencyRecord record) {
    String raw = record.getCongressId() + "|"
        + record.getUserId() + "|"
        + record.getInstitutionId() + "|"
        + record.getAmount().setScale(2, RoundingMode.UNNECESSARY).toPlainString() + "|"
        + record.getPaymentDate() + "|"
        + record.getCongressNameSnapshot().trim() + "|"
        + record.getInstitutionNameSnapshot().trim();

    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
