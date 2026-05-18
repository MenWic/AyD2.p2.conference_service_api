package ayd2.p2b.conference_service_api.feature.congress.application.port;

import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressSearchCriteria;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CongressRepositoryPort {
    Congress save(Congress congress);

    Optional<Congress> findById(UUID congressId);

    Optional<CongressView> findPublicById(UUID congressId);

    Page<CongressView> findPublicByCriteria(CongressSearchCriteria criteria, Pageable pageable);

    void deleteById(UUID congressId);
}
