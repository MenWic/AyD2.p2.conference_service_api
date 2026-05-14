package ayd2.p2b.conference_service_api.feature.institution.application.port;

import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface InstitutionRepositoryPort {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID institutionId);

    Institution save(Institution institution);

    Optional<Institution> findById(UUID institutionId);

    Optional<Institution> findActiveById(UUID institutionId);

    Page<Institution> findAllActive(Pageable pageable);
}
