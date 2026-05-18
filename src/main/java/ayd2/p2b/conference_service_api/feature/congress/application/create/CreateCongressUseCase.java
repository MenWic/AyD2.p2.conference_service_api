package ayd2.p2b.conference_service_api.feature.congress.application.create;

import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressInstitutionPort;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.application.support.CongressAccessPolicy;
import ayd2.p2b.conference_service_api.feature.congress.domain.exception.CongressExceptions;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressRequesterContext;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.InstitutionSummary;
import ayd2.p2b.conference_service_api.feature.congress.dto.request.CreateCongressRequest;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
import ayd2.p2b.conference_service_api.feature.congress.mapper.CongressMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static ayd2.p2b.conference_service_api.feature.congress.application.CongressInputValidator.requiredTrimmed;

@Component
@Transactional
public class CreateCongressUseCase {

    private final CongressRepositoryPort congressRepositoryPort;
    private final CongressInstitutionPort congressInstitutionPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final CongressMapper congressMapper;

    public CreateCongressUseCase(
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

    public CongressResponse execute(CreateCongressRequest request, CongressRequesterContext requester) {
        CongressAccessPolicy.ensureCongressAdminWrite(requester);

        String name = requiredTrimmed(request.getName(), "name");
        String description = requiredTrimmed(request.getDescription(), "description");
        String location = requiredTrimmed(request.getLocation(), "location");

        InstitutionSummary institution = congressInstitutionPort.findActiveInstitutionById(request.getInstitutionId())
                .orElseThrow(() -> CongressExceptions.institutionNotFound(request.getInstitutionId()));

        boolean linked = iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(),
                institution.getId(),
                requester.getAccessToken()
        );
        if (!linked) {
            throw CongressExceptions.forbidden("Requester is not linked to target institution");
        }

        Congress congressToSave = Congress.builder()
                .institutionId(institution.getId())
                .name(name)
                .description(description)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .location(location)
                .price(request.getPrice())
                .createdBy(requester.getUserId())
                .build();
        congressToSave.validateInvariants();

        Congress saved = congressRepositoryPort.save(congressToSave);
        return congressMapper.toResponse(saved, institution.getName());
    }
}
