package ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsCongressScopePort;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaParticipantsCongressScopeAdapter implements ParticipantsCongressScopePort {

    private final CongressRepository congressRepository;

    @Override
    public Optional<CongressInstitutionSummary> findCongressSummary(UUID congressId) {
        return congressRepository.findById(congressId)
                .map(entity -> CongressInstitutionSummary.builder()
                        .congressId(entity.getId())
                        .institutionId(entity.getInstitutionId())
                        .congressName(entity.getName())
                        .build());
    }
}
