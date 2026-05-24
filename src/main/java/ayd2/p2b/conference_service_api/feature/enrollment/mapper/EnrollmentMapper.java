package ayd2.p2b.conference_service_api.feature.enrollment.mapper;

import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.Enrollment;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyRecord;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyStatus;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.response.EnrollmentResponse;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentEntity;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentIdempotencyRecordEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

  Enrollment toDomain(EnrollmentEntity entity);

  EnrollmentEntity toEntity(Enrollment enrollment);

  EnrollmentResponse toResponse(Enrollment enrollment);

  default EnrollmentIdempotencyRecord toIdempotencyDomain(EnrollmentIdempotencyRecordEntity entity) {
    if (entity == null) {
      return null;
    }
    return EnrollmentIdempotencyRecord.builder()
        .idempotencyKey(entity.getIdempotencyKey())
        .congressId(entity.getCongressId())
        .userId(entity.getUserId())
        .paymentDate(entity.getPaymentDate())
        .requestHash(entity.getRequestHash())
        .status(EnrollmentIdempotencyStatus.valueOf(entity.getStatus()))
        .enrollmentId(entity.getEnrollmentId())
        .paymentId(entity.getPaymentId())
        .institutionId(entity.getInstitutionId())
        .congressNameSnapshot(entity.getCongressNameSnapshot())
        .institutionNameSnapshot(entity.getInstitutionNameSnapshot())
        .amount(entity.getAmount())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  default EnrollmentIdempotencyRecordEntity toIdempotencyEntity(EnrollmentIdempotencyRecord record) {
    if (record == null) {
      return null;
    }
    EnrollmentIdempotencyRecordEntity entity = new EnrollmentIdempotencyRecordEntity();
    entity.setIdempotencyKey(record.getIdempotencyKey());
    entity.setCongressId(record.getCongressId());
    entity.setUserId(record.getUserId());
    entity.setPaymentDate(record.getPaymentDate());
    entity.setRequestHash(record.getRequestHash());
    entity.setStatus(record.getStatus().name());
    entity.setEnrollmentId(record.getEnrollmentId());
    entity.setPaymentId(record.getPaymentId());
    entity.setInstitutionId(record.getInstitutionId());
    entity.setCongressNameSnapshot(record.getCongressNameSnapshot());
    entity.setInstitutionNameSnapshot(record.getInstitutionNameSnapshot());
    entity.setAmount(record.getAmount());
    entity.setCreatedAt(record.getCreatedAt());
    entity.setUpdatedAt(record.getUpdatedAt());
    return entity;
  }
}
