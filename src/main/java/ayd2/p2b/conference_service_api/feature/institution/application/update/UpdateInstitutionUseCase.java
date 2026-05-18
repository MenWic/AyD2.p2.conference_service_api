package ayd2.p2b.conference_service_api.feature.institution.application.update;

import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.application.exception.InstitutionExceptions;
import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import ayd2.p2b.conference_service_api.feature.institution.dto.request.UpdateInstitutionRequest;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static ayd2.p2b.conference_service_api.feature.institution.application.InstitutionInputValidator.optionalTrimmed;
import static ayd2.p2b.conference_service_api.feature.institution.application.InstitutionInputValidator.validateEmailOptional;

@Component
@Transactional
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

        Institution institutionToSave = institution.toBuilder()
                .name(nextName != null ? nextName : institution.getName())
                .description(nextDescription != null ? nextDescription : institution.getDescription())
                .contactEmail(nextContactEmail != null ? nextContactEmail : institution.getContactEmail())
                .updatedBy(actorId)
                .updatedAt(LocalDateTime.now())
                .build();

        Institution saved = institutionRepositoryPort.save(institutionToSave);
        return institutionMapper.toResponse(saved);
    }
}
