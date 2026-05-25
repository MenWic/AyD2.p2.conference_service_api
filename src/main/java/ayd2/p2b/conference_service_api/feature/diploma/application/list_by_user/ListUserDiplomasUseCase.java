package ayd2.p2b.conference_service_api.feature.diploma.application.list_by_user;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.diploma.application.materialize.DiplomaMaterializer;
import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaEligibilityPort;
import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaEnrollmentPort;
import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaRepositoryPort;
import ayd2.p2b.conference_service_api.feature.diploma.application.support.DiplomaAccessPolicy;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaEligibilityCandidate;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaRequesterContext;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class ListUserDiplomasUseCase {

    private final DiplomaRepositoryPort diplomaRepositoryPort;
    private final DiplomaEligibilityPort diplomaEligibilityPort;
    private final DiplomaEnrollmentPort diplomaEnrollmentPort;
    private final DiplomaMaterializer diplomaMaterializer;

    public ListUserDiplomasUseCase(
            DiplomaRepositoryPort diplomaRepositoryPort,
            DiplomaEligibilityPort diplomaEligibilityPort,
            DiplomaEnrollmentPort diplomaEnrollmentPort,
            DiplomaMaterializer diplomaMaterializer
    ) {
        this.diplomaRepositoryPort = diplomaRepositoryPort;
        this.diplomaEligibilityPort = diplomaEligibilityPort;
        this.diplomaEnrollmentPort = diplomaEnrollmentPort;
        this.diplomaMaterializer = diplomaMaterializer;
    }

    public PageResponse<DiplomaResponse> execute(UUID requestedUserId, Pageable pageable, DiplomaRequesterContext requester) {
        DiplomaAccessPolicy.ensureSelf(requestedUserId, requester);
        materializeEligibleDiplomas(requestedUserId, requester.getUserId());

        Page<DiplomaResponse> page = diplomaRepositoryPort.findPageResponseByUser(requestedUserId, pageable)
                .map(this::forceAvailableTrue);

        return PageResponse.<DiplomaResponse>builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private void materializeEligibleDiplomas(UUID userId, UUID actorUserId) {
        OffsetDateTime now = OffsetDateTime.now();

        for (DiplomaEligibilityCandidate candidate : diplomaEligibilityPort.findParticipationCandidates(userId)) {
            diplomaMaterializer.materializeIfEligible(candidate, now, actorUserId);
        }

        for (DiplomaEligibilityCandidate candidate : diplomaEligibilityPort.findLeadershipCandidates(userId)) {
            if (candidate.getType() != DiplomaType.LEADERSHIP) {
                continue;
            }
            if (!diplomaEnrollmentPort.existsEnrollment(candidate.getCongressId(), candidate.getUserId())) {
                continue;
            }
            diplomaMaterializer.materializeIfEligible(candidate, now, actorUserId);
        }
    }

    private DiplomaResponse forceAvailableTrue(DiplomaResponse response) {
        if (response == null || response.isAvailable()) {
            return response;
        }
        response.setAvailable(true);
        return response;
    }
}
