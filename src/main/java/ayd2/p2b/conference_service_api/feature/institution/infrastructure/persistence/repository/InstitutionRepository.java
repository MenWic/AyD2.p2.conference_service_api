package ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InstitutionRepository extends JpaRepository<InstitutionEntity, UUID> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    Optional<InstitutionEntity> findByIdAndActiveTrue(UUID id);

    Page<InstitutionEntity> findAllByActiveTrue(Pageable pageable);

    @Query(value = "select exists(select 1 from congresses c where c.institution_id = :institutionId)", nativeQuery = true)
    boolean existsCongressesByInstitutionId(@Param("institutionId") UUID institutionId);
}
