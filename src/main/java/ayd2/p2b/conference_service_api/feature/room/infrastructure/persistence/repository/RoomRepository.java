package ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.entity.RoomEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID> {

    @Query("""
            select r
            from RoomEntity r
            join r.congress c
            join c.institution i
            where r.id = :roomId and i.active = true
            """)
    Optional<RoomEntity> findPublicById(@Param("roomId") UUID roomId);

    @Query(
            value = """
                    select r
                    from RoomEntity r
                    join r.congress c
                    join c.institution i
                    where r.congressId = :congressId and i.active = true
                    """,
            countQuery = """
                    select count(r)
                    from RoomEntity r
                    join r.congress c
                    join c.institution i
                    where r.congressId = :congressId and i.active = true
                    """
    )
    Page<RoomEntity> findPublicByCongressId(@Param("congressId") UUID congressId, Pageable pageable);

    boolean existsByCongressIdAndName(UUID congressId, String name);

    boolean existsByCongressIdAndNameAndIdNot(UUID congressId, String name, UUID id);
}
