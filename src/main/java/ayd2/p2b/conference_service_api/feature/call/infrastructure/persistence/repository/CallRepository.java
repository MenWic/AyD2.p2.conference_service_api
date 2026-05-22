package ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.entity.CallEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CallRepository extends JpaRepository<CallEntity, UUID> {

    @Query(
            value = """
                    select c
                    from CallEntity c
                    join c.congress cg
                    join cg.institution i
                    where c.congressId = :congressId and i.active = true
                    """,
            countQuery = """
                    select count(c)
                    from CallEntity c
                    join c.congress cg
                    join cg.institution i
                    where c.congressId = :congressId and i.active = true
                    """
    )
    Page<CallEntity> findPublicByCongressId(@Param("congressId") UUID congressId, Pageable pageable);

    boolean existsByCongressIdAndStatus(UUID congressId, CallStatus status);
}
