package ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<ActivityEntity, UUID>, JpaSpecificationExecutor<ActivityEntity> {

    @Query("""
            select a
            from ActivityEntity a
            join a.congress c
            join c.institution i
            where a.id = :activityId and i.active = true
            """)
    Optional<ActivityEntity> findPublicById(@Param("activityId") UUID activityId);

    @Query("""
            select (count(a) > 0)
            from ActivityEntity a
            where a.roomId = :roomId
              and a.startTime < :endTime
              and a.endTime > :startTime
              and (:excludedActivityId is null or a.id <> :excludedActivityId)
            """)
    boolean existsRoomTimeOverlap(
            @Param("roomId") UUID roomId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("excludedActivityId") UUID excludedActivityId
    );
}
