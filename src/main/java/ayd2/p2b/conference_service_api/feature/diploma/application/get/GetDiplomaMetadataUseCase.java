package ayd2.p2b.conference_service_api.feature.diploma.application.get;

import ayd2.p2b.conference_service_api.feature.diploma.application.exception.DiplomaExceptions;
import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaRepositoryPort;
import ayd2.p2b.conference_service_api.feature.diploma.application.support.DiplomaAccessPolicy;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaRequesterContext;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class GetDiplomaMetadataUseCase {

    private final DiplomaRepositoryPort diplomaRepositoryPort;

    public GetDiplomaMetadataUseCase(DiplomaRepositoryPort diplomaRepositoryPort) {
        this.diplomaRepositoryPort = diplomaRepositoryPort;
    }

    public DiplomaResponse execute(UUID diplomaId, DiplomaRequesterContext requester) {
        DiplomaAccessPolicy.ensureParticipant(requester);
        DiplomaResponse response = diplomaRepositoryPort.findResponseById(diplomaId)
                .orElseThrow(() -> DiplomaExceptions.diplomaNotFound(diplomaId));
        DiplomaAccessPolicy.ensureOwner(response.getUserId(), requester);
        response.setAvailable(true);
        return response;
    }
}
