package ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaInstitutionRepositoryAdapter implements InstitutionRepositoryPort {

    private final InstitutionRepository institutionRepository;
    private final InstitutionMapper institutionMapper;

    public JpaInstitutionRepositoryAdapter(
            InstitutionRepository institutionRepository,
            InstitutionMapper institutionMapper
    ) {
        this.institutionRepository = institutionRepository;
        this.institutionMapper = institutionMapper;
    }

    @Override
    public boolean existsByName(String name) {
        return institutionRepository.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, UUID institutionId) {
        return institutionRepository.existsByNameAndIdNot(name, institutionId);
    }

    @Override
    public Institution save(Institution institution) {
        return institutionMapper.toDomain(
                institutionRepository.save(institutionMapper.toEntity(institution))
        );
    }

    @Override
    public Optional<Institution> findById(UUID institutionId) {
        return institutionRepository.findById(institutionId)
                .map(institutionMapper::toDomain);
    }

    @Override
    public Optional<Institution> findActiveById(UUID institutionId) {
        return institutionRepository.findByIdAndActiveTrue(institutionId)
                .map(institutionMapper::toDomain);
    }

    @Override
    public Page<Institution> findAllActive(Pageable pageable) {
        return institutionRepository.findAllByActiveTrue(pageable)
                .map(institutionMapper::toDomain);
    }
}
