package ayd2.p2b.conference_service_api.feature.call.application.close;

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

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@Transactional
public class CloseCallUseCase {

    private final CallRepositoryPort callRepositoryPort;
    private final CallCongressPort callCongressPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final CallMapper callMapper;

    public CloseCallUseCase(
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

    public CallResponse execute(UUID callId, CallRequesterContext requester) {
        CallAccessPolicy.ensureCongressAdminWrite(requester);

        Call existing = callRepositoryPort.findById(callId)
                .orElseThrow(() -> CallExceptions.callNotFound(callId));
        CallCongressSummary congress = callCongressPort.findManageableCongressById(existing.getCongressId())
                .orElseThrow(() -> CallExceptions.congressNotFound(existing.getCongressId()));
        authorizeCongressAccess(requester, congress);

        if (existing.getStatus() == CallStatus.CLOSED) {
            throw CallExceptions.callAlreadyClosed(callId);
        }

        Call closed = existing.close(requester.getUserId(), OffsetDateTime.now());
        closed.validateInvariants();

        Call saved = callRepositoryPort.save(closed);
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
