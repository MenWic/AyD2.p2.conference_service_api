package ayd2.p2b.conference_service_api.unit.feature.enrollment;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentIdempotencyRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.register.EnrollmentRegistrationTransactions;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.Enrollment;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyRecord;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentRegistrationTransactionsTest {

  @Mock
  private EnrollmentIdempotencyRepositoryPort idempotencyRepositoryPort;
  @Mock
  private EnrollmentRepositoryPort enrollmentRepositoryPort;

  private EnrollmentRegistrationTransactions transactions;

  private static final UUID CONGRESS_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final UUID ENROLLMENT_ID = UUID.randomUUID();
  private static final String IDEMPOTENCY_KEY = "idem-key-123";

  @BeforeEach
  void setUp() {
    transactions = new EnrollmentRegistrationTransactions(idempotencyRepositoryPort, enrollmentRepositoryPort);
  }

  @Test
  void completeSuccessfulEnrollmentReusesExistingWithSamePaymentId() {
    EnrollmentIdempotencyRecord record = record();
    Enrollment existing = enrollment(PAYMENT_ID);

    when(enrollmentRepositoryPort.findByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.of(existing));
    when(idempotencyRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

    Enrollment result = transactions.completeSuccessfulEnrollment(record, enrollment(PAYMENT_ID), PAYMENT_ID);

    assertThat(result).isEqualTo(existing);
    verify(idempotencyRepositoryPort).update(any());
  }

  @Test
  void completeSuccessfulEnrollmentRejectsExistingWithDifferentPaymentId() {
    EnrollmentIdempotencyRecord record = record();
    Enrollment existing = enrollment(UUID.randomUUID());

    when(enrollmentRepositoryPort.findByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> transactions.completeSuccessfulEnrollment(record, enrollment(PAYMENT_ID), PAYMENT_ID))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
          assertThat(apiEx.getCode()).isEqualTo("resource.conflict");
        });
  }

  @Test
  void completeSuccessfulEnrollmentRecoversFromConcurrentInsertWithSamePaymentId() {
    EnrollmentIdempotencyRecord record = record();
    Enrollment existing = enrollment(PAYMENT_ID);

    when(enrollmentRepositoryPort.findByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.empty(), Optional.of(existing));
    when(enrollmentRepositoryPort.save(any())).thenThrow(new DataIntegrityViolationException("duplicate enrollment"));
    when(idempotencyRepositoryPort.update(any())).thenAnswer(inv -> inv.getArgument(0));

    Enrollment result = transactions.completeSuccessfulEnrollment(record, enrollment(PAYMENT_ID), PAYMENT_ID);

    assertThat(result).isEqualTo(existing);
    verify(idempotencyRepositoryPort).update(any());
  }

  @Test
  void completeSuccessfulEnrollmentRejectsConcurrentInsertWhenExistingPaymentDiffers() {
    EnrollmentIdempotencyRecord record = record();
    Enrollment existing = enrollment(UUID.randomUUID());

    when(enrollmentRepositoryPort.findByCongressIdAndUserId(CONGRESS_ID, USER_ID))
        .thenReturn(Optional.empty(), Optional.of(existing));
    when(enrollmentRepositoryPort.save(any())).thenThrow(new DataIntegrityViolationException("duplicate enrollment"));

    assertThatThrownBy(() -> transactions.completeSuccessfulEnrollment(record, enrollment(PAYMENT_ID), PAYMENT_ID))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
  }

  private EnrollmentIdempotencyRecord record() {
    return EnrollmentIdempotencyRecord.builder()
        .idempotencyKey(IDEMPOTENCY_KEY)
        .congressId(CONGRESS_ID)
        .userId(USER_ID)
        .paymentDate(LocalDate.of(2026, 6, 15))
        .requestHash("hash")
        .status(EnrollmentIdempotencyStatus.PROCESSING)
        .institutionId(UUID.randomUUID())
        .congressNameSnapshot("Congress")
        .institutionNameSnapshot("Institution")
        .amount(new BigDecimal("100.00"))
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  private Enrollment enrollment(UUID paymentId) {
    return Enrollment.builder()
        .id(ENROLLMENT_ID)
        .congressId(CONGRESS_ID)
        .userId(USER_ID)
        .paymentId(paymentId)
        .paymentDate(LocalDate.of(2026, 6, 15))
        .enrolledAt(Instant.now())
        .createdBy(USER_ID)
        .build();
  }
}
