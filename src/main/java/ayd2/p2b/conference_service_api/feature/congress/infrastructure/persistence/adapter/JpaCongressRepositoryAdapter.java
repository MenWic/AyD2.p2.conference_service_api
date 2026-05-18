package ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressSearchCriteria;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressView;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.specification.CongressSpecification;
import ayd2.p2b.conference_service_api.feature.congress.mapper.CongressMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaCongressRepositoryAdapter implements CongressRepositoryPort {

    private final CongressRepository congressRepository;
    private final CongressMapper congressMapper;

    public JpaCongressRepositoryAdapter(
            CongressRepository congressRepository,
            CongressMapper congressMapper
    ) {
        this.congressRepository = congressRepository;
        this.congressMapper = congressMapper;
    }

    @Override
    public Congress save(Congress congress) {
        return congressMapper.toDomain(congressRepository.save(congressMapper.toEntity(congress)));
    }

    @Override
    public Optional<Congress> findById(UUID congressId) {
        return congressRepository.findById(congressId)
                .map(congressMapper::toDomain);
    }

    @Override
    public Optional<CongressView> findPublicById(UUID congressId) {
        return congressRepository.findPublicById(congressId)
                .map(this::toView);
    }

    @Override
    public Page<CongressView> findPublicByCriteria(CongressSearchCriteria criteria, Pageable pageable) {
        return congressRepository.findAll(CongressSpecification.publicCriteria(criteria), pageable)
                .map(this::toView);
    }

    @Override
    public void deleteById(UUID congressId) {
        congressRepository.deleteById(congressId);
    }

    private CongressView toView(CongressEntity entity) {
        return CongressView.builder()
                .congress(congressMapper.toDomain(entity))
                .institutionName(entity.getInstitution().getName())
                .build();
    }
}
