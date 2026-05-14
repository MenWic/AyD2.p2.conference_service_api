package ayd2.p2b.conference_service_api.feature.institution.application.update;

import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.domain.exception.InstitutionExceptions;
import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import ayd2.p2b.conference_service_api.feature.institution.dto.request.UpdateInstitutionRequest;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static ayd2.p2b.conference_service_api.feature.institution.application.InstitutionInputValidator.optionalTrimmed;
import static ayd2.p2b.conference_service_api.feature.institution.application.InstitutionInputValidator.validateEmailOptional;

@Component
public class UpdateInstitutionUseCase {

    private final InstitutionRepositoryPort institutionRepositoryPort;
    private final InstitutionMapper institutionMapper;

    public UpdateInstitutionUseCase(
            InstitutionRepositoryPort institutionRepositoryPort,
            InstitutionMapper institutionMapper
    ) {
        this.institutionRepositoryPort = institutionRepositoryPort;
        this.institutionMapper = institutionMapper;
    }

    public InstitutionResponse execute(UUID institutionId, UpdateInstitutionRequest request, UUID actorId) {
        Institution institution = institutionRepositoryPort.findById(institutionId)
                .orElseThrow(() -> InstitutionExceptions.notFound(institutionId));

        String nextName = optionalTrimmed(request.getName(), "name");
        String nextDescription = optionalTrimmed(request.getDescription(), "description");
        String nextContactEmail = validateEmailOptional(request.getContactEmail());

        if (nextName != null && institutionRepositoryPort.existsByNameAndIdNot(nextName, institutionId)) {
            throw InstitutionExceptions.nameConflict(nextName);
        }

        if (nextName != null) {
            institution.setName(nextName);
        }
        if (nextDescription != null) {
            institution.setDescription(nextDescription);
        }
        if (nextContactEmail != null) {
            institution.setContactEmail(nextContactEmail);
        }
        institution.setUpdatedBy(actorId);

        Institution saved = institutionRepositoryPort.save(institution);
        return institutionMapper.toResponse(saved);
    }
}
