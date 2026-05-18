package ayd2.p2b.conference_service_api.feature.congress.application.delete;

import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressDeletionGuardPort;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressInstitutionPort;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.application.support.CongressAccessPolicy;
import ayd2.p2b.conference_service_api.feature.congress.domain.exception.CongressExceptions;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressRequesterContext;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.InstitutionSummary;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
import ayd2.p2b.conference_service_api.feature.congress.mapper.CongressMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Transactional
public class DeleteCongressUseCase {

    private final CongressRepositoryPort congressRepositoryPort;
    private final CongressInstitutionPort congressInstitutionPort;
    private final CongressDeletionGuardPort congressDeletionGuardPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final CongressMapper congressMapper;

    public DeleteCongressUseCase(
            CongressRepositoryPort congressRepositoryPort,
            CongressInstitutionPort congressInstitutionPort,
            CongressDeletionGuardPort congressDeletionGuardPort,
            IamUserLookupPort iamUserLookupPort,
            CongressMapper congressMapper
    ) {
        this.congressRepositoryPort = congressRepositoryPort;
        this.congressInstitutionPort = congressInstitutionPort;
        this.congressDeletionGuardPort = congressDeletionGuardPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.congressMapper = congressMapper;
    }

    public CongressResponse execute(UUID congressId, CongressRequesterContext requester) {
        CongressAccessPolicy.ensureCongressAdminWrite(requester);

        Congress current = congressRepositoryPort.findById(congressId)
                .orElseThrow(() -> CongressExceptions.notFound(congressId));

        boolean linkedToCurrentInstitution = iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(),
                current.getInstitutionId(),
                requester.getAccessToken()
        );
        CongressAccessPolicy.ensureCanModifyExisting(requester, current, linkedToCurrentInstitution);

        List<String> dependencies = congressDeletionGuardPort.findBlockingDependencies(congressId);
        if (!dependencies.isEmpty()) {
            throw CongressExceptions.dependencyConflict(congressId, dependencies);
        }

        InstitutionSummary institutionSummary = congressInstitutionPort.findActiveInstitutionById(current.getInstitutionId())
                .orElseThrow(() -> CongressExceptions.institutionNotFound(current.getInstitutionId()));

        congressRepositoryPort.deleteById(congressId);

        Congress deleted = current.toBuilder()
                .updatedBy(requester.getUserId())
                .updatedAt(LocalDateTime.now())
                .build();
        return congressMapper.toResponse(deleted, institutionSummary.getName());
    }
}
