package ayd2.p2b.conference_service_api.feature.call.application.open;

import ayd2.p2b.conference_service_api.feature.call.application.exception.CallExceptions;
import ayd2.p2b.conference_service_api.feature.call.application.port.CallCongressPort;
import ayd2.p2b.conference_service_api.feature.call.application.port.CallRepositoryPort;
import ayd2.p2b.conference_service_api.feature.call.application.support.CallAccessPolicy;
import ayd2.p2b.conference_service_api.feature.call.domain.model.Call;
import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
import ayd2.p2b.conference_service_api.feature.call.dto.internal.CallCongressSummary;
import ayd2.p2b.conference_service_api.feature.call.dto.internal.CallRequesterContext;
import ayd2.p2b.conference_service_api.feature.call.dto.response.CallResponse;
import ayd2.p2b.conference_service_api.feature.call.mapper.CallMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional
public class OpenCallUseCase {

    private final CallRepositoryPort callRepositoryPort;
    private final CallCongressPort callCongressPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final CallMapper callMapper;

    public OpenCallUseCase(
            CallRepositoryPort callRepositoryPort,
            CallCongressPort callCongressPort,
            IamUserLookupPort iamUserLookupPort,
            CallMapper callMapper
    ) {
        this.callRepositoryPort = callRepositoryPort;
        this.callCongressPort = callCongressPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.callMapper = callMapper;
    }

    public CallResponse execute(UUID congressId, CallRequesterContext requester) {
        CallAccessPolicy.ensureCongressAdminWrite(requester);

        CallCongressSummary congress = callCongressPort.findManageableCongressById(congressId)
                .orElseThrow(() -> CallExceptions.congressNotFound(congressId));
        authorizeCongressAccess(requester, congress);

        if (callRepositoryPort.existsOpenCallByCongressId(congressId)) {
            throw CallExceptions.openCallAlreadyExists(congressId);
        }

        Call callToSave = Call.builder()
                .congressId(congressId)
                .status(CallStatus.OPEN)
                .createdBy(requester.getUserId())
                .build();
        callToSave.validateInvariants();

        Call saved = callRepositoryPort.save(callToSave);
        return callMapper.toResponse(saved);
    }

    private void authorizeCongressAccess(CallRequesterContext requester, CallCongressSummary congress) {
        if (requester.getUserId().equals(congress.getCreatedBy())) {
            CallAccessPolicy.ensureCanManageCongress(requester, congress, false);
            return;
        }
        boolean linkedToInstitution = iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(),
                congress.getInstitutionId(),
                requester.getAccessToken()
        );
        CallAccessPolicy.ensureCanManageCongress(requester, congress, linkedToInstitution);
    }
}
