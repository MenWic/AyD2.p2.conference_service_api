package ayd2.p2b.conference_service_api.feature.room.application.port;

import ayd2.p2b.conference_service_api.feature.room.domain.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface RoomRepositoryPort {
    Room save(Room room);

    Optional<Room> findById(UUID roomId);

    Optional<Room> findPublicById(UUID roomId);

    Page<Room> findPublicByCongressId(UUID congressId, Pageable pageable);

    boolean existsByCongressIdAndName(UUID congressId, String name);

    boolean existsByCongressIdAndNameAndIdNot(UUID congressId, String name, UUID roomId);

    void deleteById(UUID roomId);
}
