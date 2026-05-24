package ayd2.p2b.conference_service_api.feature.enrollment.application.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.support.EnrollmentAccessPolicy;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.EnrollmentRequesterContext;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.response.EnrollmentResponse;
import ayd2.p2b.conference_service_api.feature.enrollment.mapper.EnrollmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListUserEnrollmentsUseCase {

  private final EnrollmentRepositoryPort enrollmentRepositoryPort;
  private final EnrollmentMapper enrollmentMapper;

  public PageResponse<EnrollmentResponse> execute(
      UUID userId, Pageable pageable, EnrollmentRequesterContext requester) {
    EnrollmentAccessPolicy.ensureSelfAccess(userId, requester);

    Page<EnrollmentResponse> page = enrollmentRepositoryPort
        .findByUserId(userId, pageable)
        .map(enrollmentMapper::toResponse);

    return PageResponse.<EnrollmentResponse>builder()
        .items(page.getContent())
        .page(page.getNumber())
        .size(page.getSize())
        .totalItems(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .build();
  }
}
