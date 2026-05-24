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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class EnrollParticipantUseCase {

  private final EnrollmentRepositoryPort enrollmentRepositoryPort;
  private final EnrollmentIdempotencyRepositoryPort idempotencyRepositoryPort;
  private final EnrollmentCongressPort enrollmentCongressPort;
  private final WalletPaymentPort walletPaymentPort;
  private final EnrollmentMapper enrollmentMapper;

  public EnrollParticipantUseCase(
      EnrollmentRepositoryPort enrollmentRepositoryPort,
      EnrollmentIdempotencyRepositoryPort idempotencyRepositoryPort,
      EnrollmentCongressPort enrollmentCongressPort,
      WalletPaymentPort walletPaymentPort,
      EnrollmentMapper enrollmentMapper) {
    this.enrollmentRepositoryPort = enrollmentRepositoryPort;
    this.idempotencyRepositoryPort = idempotencyRepositoryPort;
    this.enrollmentCongressPort = enrollmentCongressPort;
    this.walletPaymentPort = walletPaymentPort;
    this.enrollmentMapper = enrollmentMapper;
  }

  public EnrollParticipantResult execute(
      UUID congressId,
      CreateEnrollmentRequest request,
      String idempotencyKey,
      EnrollmentRequesterContext requester) {
    EnrollmentAccessPolicy.ensureParticipant(requester);

    String requestHash = computeHash(congressId, requester.getUserId(), request.getPaymentDate());

    Optional<EnrollmentIdempotencyRecord> existingRecord = idempotencyRepositoryPort.findByKey(idempotencyKey);
    if (existingRecord.isPresent()) {
      return handleExistingRecord(existingRecord.get(), requestHash, requester);
    }

    // Pre-check: reject if there's already an active enrollment for this
    // user/congress
    idempotencyRepositoryPort.findActiveByCongressIdAndUserId(congressId, requester.getUserId())
        .ifPresent(active -> {
          throw EnrollmentExceptions.alreadyEnrolled(congressId, requester.getUserId());
        });

    // Insert PROCESSING record — DB constraint is the final race condition guard
    EnrollmentIdempotencyRecord processingRecord = EnrollmentIdempotencyRecord.builder()
        .idempotencyKey(idempotencyKey)
        .congressId(congressId)
        .userId(requester.getUserId())
        .paymentDate(request.getPaymentDate())
        .requestHash(requestHash)
        .status(EnrollmentIdempotencyStatus.PROCESSING)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    EnrollmentIdempotencyRecord savedProcessing = idempotencyRepositoryPort.insert(processingRecord);

    CongressEnrollmentSummary congressSummary = enrollmentCongressPort.findCongressSummaryById(congressId)
        .orElseThrow(() -> {
          markFailed(savedProcessing);
          return EnrollmentExceptions.congressNotFound(congressId);
        });

    UUID paymentId;
    try {
      WalletPaymentRegisterRequest walletRequest = WalletPaymentRegisterRequest.builder()
          .userId(requester.getUserId())
          .congressId(congressSummary.getCongressId())
          .institutionId(congressSummary.getInstitutionId())
          .congressNameSnapshot(congressSummary.getCongressName())
          .institutionNameSnapshot(congressSummary.getInstitutionName())
          .amount(congressSummary.getPrice())
          .paymentDate(request.getPaymentDate())
          .build();
      paymentId = walletPaymentPort.registerPayment(walletRequest, idempotencyKey, requester.getAccessToken());
    } catch (ApiException ex) {
      markFailed(savedProcessing);
      throw ex;
    }

    Enrollment enrollment = Enrollment.builder()
        .congressId(congressId)
        .userId(requester.getUserId())
        .paymentId(paymentId)
        .enrolledAt(Instant.now())
        .paymentDate(request.getPaymentDate())
        .createdBy(requester.getUserId())
        .build();
    enrollment.validateInvariants();

    Enrollment saved = enrollmentRepositoryPort.save(enrollment);

    idempotencyRepositoryPort.update(savedProcessing.toBuilder()
        .status(EnrollmentIdempotencyStatus.SUCCEEDED)
        .enrollmentId(saved.getId())
        .paymentId(paymentId)
        .updatedAt(Instant.now())
        .build());

    EnrollmentResponse response = enrollmentMapper.toResponse(saved);
    return EnrollParticipantResult.builder()
        .enrollment(response)
        .replay(false)
        .build();
  }

  private EnrollParticipantResult handleExistingRecord(
      EnrollmentIdempotencyRecord record, String requestHash, EnrollmentRequesterContext requester) {

    return switch (record.getStatus()) {
      case FAILED -> throw EnrollmentExceptions.failedKeyNotReusable(record.getIdempotencyKey());
      case PROCESSING -> throw EnrollmentExceptions.alreadyEnrolled(record.getCongressId(), record.getUserId());
      case SUCCEEDED -> {
        if (!record.getRequestHash().equals(requestHash)) {
          throw EnrollmentExceptions.idempotencyConflict(record.getIdempotencyKey());
        }
        Enrollment existing = enrollmentRepositoryPort
            .findByCongressIdAndUserId(record.getCongressId(), record.getUserId())
            .orElseThrow(() -> EnrollmentExceptions.notFound(record.getEnrollmentId()));
        EnrollmentResponse response = enrollmentMapper.toResponse(existing);
        yield EnrollParticipantResult.builder()
            .enrollment(response)
            .replay(true)
            .build();
      }
    };
  }

  private void markFailed(EnrollmentIdempotencyRecord record) {
    idempotencyRepositoryPort.update(record.toBuilder()
        .status(EnrollmentIdempotencyStatus.FAILED)
        .updatedAt(Instant.now())
        .build());
  }

  private static String computeHash(UUID congressId, UUID userId, LocalDate paymentDate) {
    String raw = congressId.toString() + "|" + userId.toString() + "|" + paymentDate.toString();
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
