package ayd2.p2b.conference_service_api.feature.diploma.application.port;

import ayd2.p2b.conference_service_api.feature.diploma.domain.model.Diploma;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiplomaRepositoryPort {
    Optional<Diploma> findById(UUID diplomaId);

    List<Diploma> findExistingForUser(UUID userId);

    Page<Diploma> findPageByUser(UUID userId, Pageable pageable);

    Page<DiplomaResponse> findPageResponseByUser(UUID userId, Pageable pageable);

    Optional<DiplomaResponse> findResponseById(UUID diplomaId);

    Diploma save(Diploma diploma);

    Optional<Diploma> findByUniqueKey(UUID userId, UUID congressId, DiplomaType type, UUID activityId);
}
