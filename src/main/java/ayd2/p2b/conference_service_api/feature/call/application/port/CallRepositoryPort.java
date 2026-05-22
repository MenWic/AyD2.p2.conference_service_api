package ayd2.p2b.conference_service_api.feature.call.application.port;

import ayd2.p2b.conference_service_api.feature.call.domain.model.Call;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CallRepositoryPort {
    Call save(Call call);

    Optional<Call> findById(UUID callId);

    Page<Call> findPublicByCongressId(UUID congressId, Pageable pageable);

    boolean existsOpenCallByCongressId(UUID congressId);
}
