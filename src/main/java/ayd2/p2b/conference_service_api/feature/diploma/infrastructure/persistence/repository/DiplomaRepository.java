package ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.entity.DiplomaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiplomaRepository extends JpaRepository<DiplomaEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"congress", "activity"})
    Optional<DiplomaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"congress", "activity"})
    Page<DiplomaEntity> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"congress", "activity"})
    List<DiplomaEntity> findByUserIdOrderByIssuedAtDesc(UUID userId);

    Optional<DiplomaEntity> findByUserIdAndCongressIdAndTypeAndActivityIdIsNull(
            UUID userId,
            UUID congressId,
            DiplomaType type
    );

    Optional<DiplomaEntity> findByUserIdAndCongressIdAndTypeAndActivityId(
            UUID userId,
            UUID congressId,
            DiplomaType type,
            UUID activityId
    );
}
