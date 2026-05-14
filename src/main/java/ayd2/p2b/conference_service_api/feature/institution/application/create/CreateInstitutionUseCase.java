package ayd2.p2b.conference_service_api.feature.institution.application.create;

import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.domain.exception.InstitutionExceptions;
import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import ayd2.p2b.conference_service_api.feature.institution.dto.request.CreateInstitutionRequest;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static ayd2.p2b.conference_service_api.feature.institution.application.InstitutionInputValidator.requiredTrimmed;
import static ayd2.p2b.conference_service_api.feature.institution.application.InstitutionInputValidator.validateEmailRequired;

@Component
public class CreateInstitutionUseCase {

    private final InstitutionRepositoryPort institutionRepositoryPort;
    private final InstitutionMapper institutionMapper;

    public CreateInstitutionUseCase(
            InstitutionRepositoryPort institutionRepositoryPort,
            InstitutionMapper institutionMapper
    ) {
        this.institutionRepositoryPort = institutionRepositoryPort;
        this.institutionMapper = institutionMapper;
    }

    public InstitutionResponse execute(CreateInstitutionRequest request, UUID actorId) {
        String name = requiredTrimmed(request.getName(), "name");
        String description = requiredTrimmed(request.getDescription(), "description");
        String contactEmail = validateEmailRequired(request.getContactEmail());

        if (institutionRepositoryPort.existsByName(name)) {
            throw InstitutionExceptions.nameConflict(name);
        }

        Institution saved = institutionRepositoryPort.save(Institution.builder()
                .name(name)
                .description(description)
                .contactEmail(contactEmail)
                .active(true)
                .createdBy(actorId)
                .build());

        return institutionMapper.toResponse(saved);
    }
}
