package ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CongressRepository extends JpaRepository<CongressEntity, UUID>, JpaSpecificationExecutor<CongressEntity> {

    @Query("select c from CongressEntity c join fetch c.institution i where c.id = :id and i.active = true")
    Optional<CongressEntity> findPublicById(@Param("id") UUID id);

    @Override
    @EntityGraph(attributePaths = "institution")
    Page<CongressEntity> findAll(Specification<CongressEntity> spec, Pageable pageable);
}
