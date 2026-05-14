package ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionDependencyPort;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JpaInstitutionDependencyAdapter implements InstitutionDependencyPort {

    private final InstitutionRepository institutionRepository;

    public JpaInstitutionDependencyAdapter(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @Override
    public boolean hasCongressesByInstitutionId(UUID institutionId) {
        return institutionRepository.existsCongressesByInstitutionId(institutionId);
    }
}
