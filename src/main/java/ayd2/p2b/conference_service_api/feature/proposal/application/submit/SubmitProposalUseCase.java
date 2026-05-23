package ayd2.p2b.conference_service_api.feature.proposal.application.submit;

import ayd2.p2b.conference_service_api.feature.proposal.application.ProposalInputValidator;
import ayd2.p2b.conference_service_api.feature.proposal.application.exception.ProposalExceptions;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCallPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalRepositoryPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.support.ProposalAccessPolicy;
import ayd2.p2b.conference_service_api.feature.proposal.domain.exception.ProposalDomainException;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallStatus;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallSummary;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalRequesterContext;
import ayd2.p2b.conference_service_api.feature.proposal.dto.request.CreateProposalRequest;
import ayd2.p2b.conference_service_api.feature.proposal.dto.response.ProposalResponse;
import ayd2.p2b.conference_service_api.feature.proposal.mapper.ProposalMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional
public class SubmitProposalUseCase {

    private final ProposalRepositoryPort proposalRepositoryPort;
    private final ProposalCallPort proposalCallPort;
    private final ProposalMapper proposalMapper;

    public SubmitProposalUseCase(
            ProposalRepositoryPort proposalRepositoryPort,
            ProposalCallPort proposalCallPort,
            ProposalMapper proposalMapper
    ) {
        this.proposalRepositoryPort = proposalRepositoryPort;
        this.proposalCallPort = proposalCallPort;
        this.proposalMapper = proposalMapper;
    }

    public ProposalResponse execute(UUID callId, CreateProposalRequest request, ProposalRequesterContext requester) {
        ProposalAccessPolicy.ensureParticipant(requester);
        CreateProposalRequest normalizedRequest = ProposalInputValidator.requireValidCreateRequest(request);

        ProposalCallSummary callSummary = proposalCallPort.findVisibleCallById(callId)
                .orElseThrow(() -> ProposalExceptions.callNotFound(callId));

        if (callSummary.getStatus() != ProposalCallStatus.OPEN) {
            throw ProposalExceptions.submitRequiresOpenCall(callId);
        }

        Proposal proposalToSave = Proposal.builder()
                .callId(callId)
                .authorUserId(requester.getUserId())
                .title(normalizedRequest.getTitle())
                .description(normalizedRequest.getDescription())
                .type(normalizedRequest.getType())
                .status(ProposalStatus.PENDING)
                .createdBy(requester.getUserId())
                .build();

        try {
            proposalToSave.validateInvariants();
        } catch (ProposalDomainException ex) {
            throw ProposalExceptions.validationFailed(ex.getMessage());
        }

        Proposal saved = proposalRepositoryPort.save(proposalToSave);
        return proposalMapper.toResponse(saved);
    }
}
