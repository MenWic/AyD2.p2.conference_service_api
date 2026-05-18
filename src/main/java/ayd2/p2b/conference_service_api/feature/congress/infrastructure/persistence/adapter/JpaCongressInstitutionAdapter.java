package ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressInstitutionPort;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.InstitutionSummary;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaCongressInstitutionAdapter implements CongressInstitutionPort {

    private final InstitutionRepository institutionRepository;

    public JpaCongressInstitutionAdapter(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @Override
    public Optional<InstitutionSummary> findActiveInstitutionById(UUID institutionId) {
        return institutionRepository.findByIdAndActiveTrue(institutionId)
                .map(entity -> InstitutionSummary.builder()
                        .id(entity.getId())
                        .name(entity.getName())
                        .build());
    }
}
