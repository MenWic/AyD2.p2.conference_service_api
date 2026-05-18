package ayd2.p2b.conference_service_api.feature.congress.application.get;

import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.application.exception.CongressExceptions;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
import ayd2.p2b.conference_service_api.feature.congress.mapper.CongressMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class GetCongressUseCase {

    private final CongressRepositoryPort congressRepositoryPort;
    private final CongressMapper congressMapper;

    public GetCongressUseCase(
            CongressRepositoryPort congressRepositoryPort,
            CongressMapper congressMapper
    ) {
        this.congressRepositoryPort = congressRepositoryPort;
        this.congressMapper = congressMapper;
    }

    public CongressResponse execute(UUID congressId) {
        return congressRepositoryPort.findPublicById(congressId)
                .map(view -> congressMapper.toResponse(view.getCongress(), view.getInstitutionName()))
                .orElseThrow(() -> CongressExceptions.notFound(congressId));
    }
}
