package ayd2.p2b.conference_service_api.unit.feature.diploma.materialize;

import ayd2.p2b.conference_service_api.feature.diploma.application.materialize.DiplomaMaterializer;
import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaRepositoryPort;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.Diploma;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaEligibilityCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class DiplomaMaterializerTest {

    @Mock
    private DiplomaRepositoryPort diplomaRepositoryPort;

    private DiplomaMaterializer materializer;

    @BeforeEach
    void setUp() {
        materializer = new DiplomaMaterializer(diplomaRepositoryPort);
    }

    @Test
    void shouldNotCreateWhenDiplomaAlreadyExists() {
        UUID userId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID actor = userId;
        Diploma existing = diploma(userId, congressId, DiplomaType.PARTICIPATION, null);
        DiplomaEligibilityCandidate candidate = participationCandidate(userId, congressId);

        when(diplomaRepositoryPort.findByUniqueKey(userId, congressId, DiplomaType.PARTICIPATION, null))
                .thenReturn(Optional.of(existing));

        Diploma result = materializer.materializeIfEligible(candidate, OffsetDateTime.now(), actor);

        assertThat(result.getId()).isEqualTo(existing.getId());
        verify(diplomaRepositoryPort, never()).save(any());
    }

    @Test
    void shouldCreateParticipationWithNullActivityId() {
        UUID userId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID actor = userId;
        OffsetDateTime issuedAt = OffsetDateTime.parse("2026-10-10T10:00:00Z");
        Diploma saved = diploma(userId, congressId, DiplomaType.PARTICIPATION, null);

        when(diplomaRepositoryPort.findByUniqueKey(userId, congressId, DiplomaType.PARTICIPATION, null))
                .thenReturn(Optional.empty());
        when(diplomaRepositoryPort.save(any())).thenReturn(saved);

        Diploma result = materializer.materializeIfEligible(participationCandidate(userId, congressId), issuedAt, actor);

        assertThat(result.getType()).isEqualTo(DiplomaType.PARTICIPATION);
        assertThat(result.getActivityId()).isNull();
    }

    @Test
    void shouldCreateLeadershipWithActivityId() {
        UUID userId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID actor = userId;
        OffsetDateTime issuedAt = OffsetDateTime.parse("2026-10-10T10:00:00Z");
        Diploma saved = diploma(userId, congressId, DiplomaType.LEADERSHIP, activityId);

        when(diplomaRepositoryPort.findByUniqueKey(userId, congressId, DiplomaType.LEADERSHIP, activityId))
                .thenReturn(Optional.empty());
        when(diplomaRepositoryPort.save(any())).thenReturn(saved);

        Diploma result = materializer.materializeIfEligible(leadershipCandidate(userId, congressId, activityId), issuedAt, actor);

        assertThat(result.getType()).isEqualTo(DiplomaType.LEADERSHIP);
        assertThat(result.getActivityId()).isEqualTo(activityId);
    }

    @Test
    void shouldReloadExistingDiplomaWhenUniqueConflictOccurs() {
        UUID userId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID actor = userId;
        OffsetDateTime issuedAt = OffsetDateTime.parse("2026-10-10T10:00:00Z");
        Diploma existing = diploma(userId, congressId, DiplomaType.LEADERSHIP, activityId);

        when(diplomaRepositoryPort.findByUniqueKey(userId, congressId, DiplomaType.LEADERSHIP, activityId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(diplomaRepositoryPort.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        Diploma result = materializer.materializeIfEligible(
                leadershipCandidate(userId, congressId, activityId),
                issuedAt,
                actor
        );

        assertThat(result.getId()).isEqualTo(existing.getId());
        verify(diplomaRepositoryPort).save(any());
        verify(diplomaRepositoryPort, times(2))
                .findByUniqueKey(eq(userId), eq(congressId), eq(DiplomaType.LEADERSHIP), eq(activityId));
    }

    private DiplomaEligibilityCandidate participationCandidate(UUID userId, UUID congressId) {
        return DiplomaEligibilityCandidate.builder()
                .userId(userId)
                .congressId(congressId)
                .congressName("Congreso")
                .type(DiplomaType.PARTICIPATION)
                .build();
    }

    private DiplomaEligibilityCandidate leadershipCandidate(UUID userId, UUID congressId, UUID activityId) {
        return DiplomaEligibilityCandidate.builder()
                .userId(userId)
                .congressId(congressId)
                .congressName("Congreso")
                .type(DiplomaType.LEADERSHIP)
                .activityId(activityId)
                .activityName("Activity")
                .build();
    }

    private Diploma diploma(UUID userId, UUID congressId, DiplomaType type, UUID activityId) {
        return Diploma.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .congressId(congressId)
                .type(type)
                .activityId(activityId)
                .issuedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .createdBy(userId)
                .createdAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .build();
    }
}
