package ayd2.p2b.conference_service_api.feature.institution.application.delete;

import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionDependencyPort;
import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.domain.exception.InstitutionExceptions;
import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeleteInstitutionUseCase {

    private final InstitutionRepositoryPort institutionRepositoryPort;
    private final InstitutionDependencyPort institutionDependencyPort;
    private final InstitutionMapper institutionMapper;

    public DeleteInstitutionUseCase(
            InstitutionRepositoryPort institutionRepositoryPort,
            InstitutionDependencyPort institutionDependencyPort,
            InstitutionMapper institutionMapper
    ) {
        this.institutionRepositoryPort = institutionRepositoryPort;
        this.institutionDependencyPort = institutionDependencyPort;
        this.institutionMapper = institutionMapper;
    }

    public InstitutionResponse execute(UUID institutionId, UUID actorId) {
        Institution institution = institutionRepositoryPort.findById(institutionId)
                .orElseThrow(() -> InstitutionExceptions.notFound(institutionId));

        if (institutionDependencyPort.hasCongressesByInstitutionId(institutionId)) {
            throw InstitutionExceptions.hasCongresses(institutionId);
        }

        if (institution.isActive()) {
            institution.setActive(false);
            institution.setUpdatedBy(actorId);
            institution = institutionRepositoryPort.save(institution);
        }

        return institutionMapper.toResponse(institution);
    }
}
