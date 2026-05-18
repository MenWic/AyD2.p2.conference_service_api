package ayd2.p2b.conference_service_api.feature.institution.application.get;

import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.domain.exception.InstitutionExceptions;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class GetInstitutionUseCase {

    private final InstitutionRepositoryPort institutionRepositoryPort;
    private final InstitutionMapper institutionMapper;

    public GetInstitutionUseCase(
            InstitutionRepositoryPort institutionRepositoryPort,
            InstitutionMapper institutionMapper
    ) {
        this.institutionRepositoryPort = institutionRepositoryPort;
        this.institutionMapper = institutionMapper;
    }

    public InstitutionResponse execute(UUID institutionId) {
        return institutionRepositoryPort.findActiveById(institutionId)
                .map(institutionMapper::toResponse)
                .orElseThrow(() -> InstitutionExceptions.notFound(institutionId));
    }
}
