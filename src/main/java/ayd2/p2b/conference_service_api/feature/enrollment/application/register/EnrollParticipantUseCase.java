package ayd2.p2b.conference_service_api.feature.enrollment.application.register;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.enrollment.application.exception.EnrollmentExceptions;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentCongressPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentIdempotencyRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.support.EnrollmentAccessPolicy;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Component
public class EnrollParticipantUseCase {

  private final EnrollmentRepositoryPort enrollmentRepositoryPort;
  private final EnrollmentIdempotencyRepositoryPort idempotencyRepositoryPort;
  private final EnrollmentCongressPort enrollmentCongressPort;
  private final WalletPaymentPort walletPaymentPort;
  private final EnrollmentMapper enrollmentMapper;
  private final EnrollmentRegistrationTransactions enrollmentRegistrationTransactions;

  public EnrollParticipantUseCase(
      EnrollmentRepositoryPort enrollmentRepositoryPort,
      EnrollmentIdempotencyRepositoryPort idempotencyRepositoryPort,
      EnrollmentCongressPort enrollmentCongressPort,
      WalletPaymentPort walletPaymentPort,
      EnrollmentMapper enrollmentMapper,
      EnrollmentRegistrationTransactions enrollmentRegistrationTransactions) {
    this.enrollmentRepositoryPort = enrollmentRepositoryPort;
    this.idempotencyRepositoryPort = idempotencyRepositoryPort;
    this.enrollmentCongressPort = enrollmentCongressPort;
    this.walletPaymentPort = walletPaymentPort;
    this.enrollmentMapper = enrollmentMapper;
    this.enrollmentRegistrationTransactions = enrollmentRegistrationTransactions;
  }

  public EnrollParticipantResult execute(
      UUID congressId,
      CreateEnrollmentRequest request,
      String idempotencyKey,
      EnrollmentRequesterContext requester) {
    EnrollmentAccessPolicy.ensureParticipant(requester);
    LocalDate paymentDate = requirePaymentDate(request);

    Optional<EnrollmentIdempotencyRecord> existingRecord = idempotencyRepositoryPort.findByKey(idempotencyKey);
    if (existingRecord.isPresent()) {
      return handleExistingRecord(existingRecord.get(), congressId, paymentDate, requester);
    }

    CongressEnrollmentSummary congressSummary = enrollmentCongressPort.findPublicEnrollmentCongressById(congressId)
        .orElseThrow(() -> EnrollmentExceptions.congressNotFound(congressId));

    String congressNameSnapshot = normalizeSnapshotName(congressSummary.getCongressName(), "congressNameSnapshot");
    String institutionNameSnapshot = normalizeSnapshotName(
        congressSummary.getInstitutionName(),
        "institutionNameSnapshot");
    BigDecimal amount = normalizeAmount(congressSummary.getPrice());

    String requestHash = computeWalletPayloadHash(
        congressSummary.getCongressId(),
        requester.getUserId(),
        congressSummary.getInstitutionId(),
        amount,
        paymentDate,
        congressNameSnapshot,
        institutionNameSnapshot);

    idempotencyRepositoryPort.findActiveByCongressIdAndUserId(congressId, requester.getUserId())
        .ifPresent(active -> {
          throw EnrollmentExceptions.alreadyEnrolled(congressId, requester.getUserId());
        });

    EnrollmentIdempotencyRecord processingRecord = EnrollmentIdempotencyRecord.builder()
        .idempotencyKey(idempotencyKey)
        .congressId(congressId)
        .userId(requester.getUserId())
        .paymentDate(paymentDate)
        .requestHash(requestHash)
        .status(EnrollmentIdempotencyStatus.PROCESSING)
        .institutionId(congressSummary.getInstitutionId())
        .congressNameSnapshot(congressNameSnapshot)
        .institutionNameSnapshot(institutionNameSnapshot)
        .amount(amount)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    EnrollmentIdempotencyRecord savedProcessing;
    try {
      savedProcessing = enrollmentRegistrationTransactions.createProcessingRecord(processingRecord);
    } catch (ApiException conflict) {
      if (conflict.getStatus().isSameCodeAs(HttpStatus.CONFLICT)) {
        return recoverAfterConcurrentProcessingInsert(congressId, paymentDate, idempotencyKey, requester);
      }
      throw conflict;
    }

    return recoverProcessingWithoutPayment(savedProcessing, requester, false);
  }

  private EnrollParticipantResult handleExistingRecord(
      EnrollmentIdempotencyRecord record,
      UUID requestedCongressId,
      LocalDate requestedPaymentDate,
      EnrollmentRequesterContext requester) {

    ensureIdentityMatches(record, requestedCongressId, requester.getUserId(), requestedPaymentDate);

    return switch (record.getStatus()) {
      case FAILED -> throw EnrollmentExceptions.failedKeyNotReusable(record.getIdempotencyKey());
      case PROCESSING -> {
        if (record.getPaymentId() != null) {
          Enrollment enrollment = enrollmentRegistrationTransactions.completeSuccessfulEnrollment(
              record,
              buildEnrollmentDraft(record, record.getPaymentId()),
              record.getPaymentId());
          yield EnrollParticipantResult.builder()
              .enrollment(enrollmentMapper.toResponse(enrollment))
              .replay(true)
              .build();
        }
        yield recoverProcessingWithoutPayment(record, requester, true);
      }
      case SUCCEEDED -> {
        Enrollment existing = enrollmentRepositoryPort.findByCongressIdAndUserId(record.getCongressId(), record.getUserId())
            .orElseGet(() -> {
              if (record.getPaymentId() == null) {
                throw EnrollmentExceptions.idempotencyConflict(record.getIdempotencyKey());
              }
              return enrollmentRegistrationTransactions.completeSuccessfulEnrollment(
                  record,
                  buildEnrollmentDraft(record, record.getPaymentId()),
                  record.getPaymentId());
            });
        if (!existing.getPaymentId().equals(record.getPaymentId())) {
          throw EnrollmentExceptions.idempotencyConflict(record.getIdempotencyKey());
        }
        yield EnrollParticipantResult.builder()
            .enrollment(enrollmentMapper.toResponse(existing))
            .replay(true)
            .build();
      }
    };
  }

  private EnrollParticipantResult recoverProcessingWithoutPayment(
      EnrollmentIdempotencyRecord record,
      EnrollmentRequesterContext requester,
      boolean replay) {

    WalletPaymentRegisterRequest walletRequest = walletRequestFromRecord(record);
    UUID paymentId;
    try {
      paymentId = walletPaymentPort.registerPayment(walletRequest, record.getIdempotencyKey(), requester.getAccessToken());
    } catch (ApiException ex) {
      if (isDeterministicFailure(ex)) {
        enrollmentRegistrationTransactions.markFailed(record);
      }
      throw ex;
    }

    EnrollmentIdempotencyRecord updatedRecord = enrollmentRegistrationTransactions.recordWalletPayment(record, paymentId);
    Enrollment enrollment = enrollmentRegistrationTransactions.completeSuccessfulEnrollment(
        updatedRecord,
        buildEnrollmentDraft(updatedRecord, paymentId),
        paymentId);

    return EnrollParticipantResult.builder()
        .enrollment(enrollmentMapper.toResponse(enrollment))
        .replay(replay)
        .build();
  }

  private EnrollParticipantResult recoverAfterConcurrentProcessingInsert(
      UUID congressId,
      LocalDate paymentDate,
      String idempotencyKey,
      EnrollmentRequesterContext requester) {
    Optional<EnrollmentIdempotencyRecord> recordByKey = idempotencyRepositoryPort.findByKey(idempotencyKey);
    if (recordByKey.isPresent()) {
      return handleExistingRecord(recordByKey.get(), congressId, paymentDate, requester);
    }

    Optional<EnrollmentIdempotencyRecord> activeRecord = idempotencyRepositoryPort
        .findActiveByCongressIdAndUserId(congressId, requester.getUserId());
    if (activeRecord.isPresent()) {
      throw EnrollmentExceptions.alreadyEnrolled(congressId, requester.getUserId());
    }

    throw EnrollmentExceptions.idempotencyConflict(idempotencyKey);
  }

  private void ensureIdentityMatches(
      EnrollmentIdempotencyRecord record,
      UUID requestedCongressId,
      UUID requesterUserId,
      LocalDate requestedPaymentDate) {
    if (!record.getCongressId().equals(requestedCongressId)
        || !record.getUserId().equals(requesterUserId)
        || !record.getPaymentDate().equals(requestedPaymentDate)) {
      throw EnrollmentExceptions.idempotencyConflict(record.getIdempotencyKey());
    }
  }

  private LocalDate requirePaymentDate(CreateEnrollmentRequest request) {
    if (request == null || request.getPaymentDate() == null) {
      throw EnrollmentExceptions.validationFailed("paymentDate is required");
    }
    return request.getPaymentDate();
  }

  private BigDecimal normalizeAmount(BigDecimal amount) {
    if (amount == null) {
      throw EnrollmentExceptions.validationFailed("Congress price is required for enrollment");
    }
    try {
      return amount.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException ex) {
      throw EnrollmentExceptions.validationFailed("Congress price must use monetary scale 2");
    }
  }

  private String normalizeSnapshotName(String value, String field) {
    if (value == null) {
      throw EnrollmentExceptions.validationFailed(field + " is required");
    }
    String normalized = value.trim();
    if (normalized.isEmpty()) {
      throw EnrollmentExceptions.validationFailed(field + " is required");
    }
    return normalized;
  }

  private WalletPaymentRegisterRequest walletRequestFromRecord(EnrollmentIdempotencyRecord record) {
    return WalletPaymentRegisterRequest.builder()
        .userId(record.getUserId())
        .congressId(record.getCongressId())
        .institutionId(record.getInstitutionId())
        .congressNameSnapshot(record.getCongressNameSnapshot())
        .institutionNameSnapshot(record.getInstitutionNameSnapshot())
        .amount(record.getAmount())
        .paymentDate(record.getPaymentDate())
        .build();
  }

  private Enrollment buildEnrollmentDraft(EnrollmentIdempotencyRecord record, UUID paymentId) {
    Enrollment draft = Enrollment.builder()
        .congressId(record.getCongressId())
        .userId(record.getUserId())
        .paymentId(paymentId)
        .paymentDate(record.getPaymentDate())
        .enrolledAt(Instant.now())
        .createdBy(record.getUserId())
        .build();
    draft.validateInvariants();
    return draft;
  }

  private boolean isDeterministicFailure(ApiException ex) {
    return ex.getStatus().isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY)
        && "wallet.insufficient_funds".equals(ex.getCode());
  }

  private static String computeWalletPayloadHash(
      UUID congressId,
      UUID userId,
      UUID institutionId,
      BigDecimal amount,
      LocalDate paymentDate,
      String congressNameSnapshot,
      String institutionNameSnapshot) {
    String raw = congressId + "|" + userId + "|" + institutionId + "|"
        + amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString() + "|"
        + paymentDate + "|" + congressNameSnapshot.trim() + "|" + institutionNameSnapshot.trim();
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
