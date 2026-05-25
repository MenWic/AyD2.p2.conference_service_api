package ayd2.p2b.conference_service_api.feature.diploma.application.materialize;

import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaRepositoryPort;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.Diploma;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaEligibilityCandidate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class DiplomaMaterializer {

    private final DiplomaRepositoryPort diplomaRepositoryPort;

    public DiplomaMaterializer(DiplomaRepositoryPort diplomaRepositoryPort) {
        this.diplomaRepositoryPort = diplomaRepositoryPort;
    }

    @Transactional
    public Diploma materializeIfEligible(DiplomaEligibilityCandidate candidate, OffsetDateTime issuedAt, java.util.UUID actorUserId) {
        return diplomaRepositoryPort.findByUniqueKey(
                        candidate.getUserId(),
                        candidate.getCongressId(),
                        candidate.getType(),
                        candidate.getActivityId())
                .orElseGet(() -> createDiploma(candidate, issuedAt, actorUserId));
    }

    private Diploma createDiploma(DiplomaEligibilityCandidate candidate, OffsetDateTime issuedAt, java.util.UUID actorUserId) {
        Diploma draft = Diploma.builder()
                .userId(candidate.getUserId())
                .congressId(candidate.getCongressId())
                .type(candidate.getType())
                .activityId(candidate.getActivityId())
                .issuedAt(issuedAt)
                .createdBy(actorUserId)
                .createdAt(issuedAt)
                .build();
        draft.validateInvariants();

        try {
            return diplomaRepositoryPort.save(draft);
        } catch (DataIntegrityViolationException ex) {
            return diplomaRepositoryPort.findByUniqueKey(
                            candidate.getUserId(),
                            candidate.getCongressId(),
                            candidate.getType(),
                            candidate.getActivityId())
                    .orElseThrow(() -> ex);
        }
    }
}
