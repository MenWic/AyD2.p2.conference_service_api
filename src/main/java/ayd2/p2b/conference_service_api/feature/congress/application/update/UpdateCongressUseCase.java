package ayd2.p2b.conference_service_api.feature.congress.application.update;

import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressInstitutionPort;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.application.support.CongressAccessPolicy;
import ayd2.p2b.conference_service_api.feature.congress.application.exception.CongressExceptions;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressRequesterContext;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.InstitutionSummary;
import ayd2.p2b.conference_service_api.feature.congress.dto.request.UpdateCongressRequest;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
import ayd2.p2b.conference_service_api.feature.congress.mapper.CongressMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static ayd2.p2b.conference_service_api.feature.congress.application.CongressInputValidator.optionalTrimmed;

@Component
@Transactional
public class UpdateCongressUseCase {

    private final CongressRepositoryPort congressRepositoryPort;
    private final CongressInstitutionPort congressInstitutionPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final CongressMapper congressMapper;

    public UpdateCongressUseCase(
            CongressRepositoryPort congressRepositoryPort,
            CongressInstitutionPort congressInstitutionPort,
            IamUserLookupPort iamUserLookupPort,
            CongressMapper congressMapper
    ) {
        this.congressRepositoryPort = congressRepositoryPort;
        this.congressInstitutionPort = congressInstitutionPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.congressMapper = congressMapper;
    }

    public CongressResponse execute(UUID congressId, UpdateCongressRequest request, CongressRequesterContext requester) {
        CongressAccessPolicy.ensureCongressAdminWrite(requester);

        Congress current = congressRepositoryPort.findById(congressId)
                .orElseThrow(() -> CongressExceptions.notFound(congressId));

        boolean linkedToCurrentInstitution = iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(),
                current.getInstitutionId(),
                requester.getAccessToken()
        );
        CongressAccessPolicy.ensureCanModifyExisting(requester, current, linkedToCurrentInstitution);

        UUID targetInstitutionId = request.getInstitutionId() != null ? request.getInstitutionId() : current.getInstitutionId();
        InstitutionSummary institutionSummary = congressInstitutionPort.findActiveInstitutionById(targetInstitutionId)
                .orElseThrow(() -> CongressExceptions.institutionNotFound(targetInstitutionId));

        if (!targetInstitutionId.equals(current.getInstitutionId())) {
            boolean linkedToNewInstitution = iamUserLookupPort.isCongressAdminLinkedToInstitution(
                    requester.getUserId(),
                    targetInstitutionId,
                    requester.getAccessToken()
            );
            if (!linkedToNewInstitution) {
                throw CongressExceptions.forbidden("Requester is not linked to new institution");
            }
        }

        String nextName = optionalTrimmed(request.getName(), "name");
        String nextDescription = optionalTrimmed(request.getDescription(), "description");
        String nextLocation = optionalTrimmed(request.getLocation(), "location");

        LocalDate nextStartDate = request.getStartDate() != null ? request.getStartDate() : current.getStartDate();
        LocalDate nextEndDate = request.getEndDate() != null ? request.getEndDate() : current.getEndDate();

        Congress congressToSave = current.toBuilder()
                .institutionId(targetInstitutionId)
                .name(nextName != null ? nextName : current.getName())
                .description(nextDescription != null ? nextDescription : current.getDescription())
                .startDate(nextStartDate)
                .endDate(nextEndDate)
                .location(nextLocation != null ? nextLocation : current.getLocation())
                .price(request.getPrice() != null ? request.getPrice() : current.getPrice())
                .updatedBy(requester.getUserId())
                .updatedAt(LocalDateTime.now())
                .build();
        congressToSave.validateInvariants();

        Congress saved = congressRepositoryPort.save(congressToSave);
        return congressMapper.toResponse(saved, institutionSummary.getName());
    }
}
