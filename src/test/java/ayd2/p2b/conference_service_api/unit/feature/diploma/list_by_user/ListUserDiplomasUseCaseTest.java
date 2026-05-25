package ayd2.p2b.conference_service_api.unit.feature.diploma.list_by_user;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.diploma.application.list_by_user.ListUserDiplomasUseCase;
import ayd2.p2b.conference_service_api.feature.diploma.application.materialize.DiplomaMaterializer;
import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaEligibilityPort;
import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaEnrollmentPort;
import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaRepositoryPort;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.Diploma;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaEligibilityCandidate;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaRequesterContext;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUserDiplomasUseCaseTest {

    @Mock
    private DiplomaRepositoryPort diplomaRepositoryPort;
    @Mock
    private DiplomaEligibilityPort diplomaEligibilityPort;
    @Mock
    private DiplomaEnrollmentPort diplomaEnrollmentPort;
    @Mock
    private DiplomaMaterializer diplomaMaterializer;

    private ListUserDiplomasUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUserDiplomasUseCase(
                diplomaRepositoryPort,
                diplomaEligibilityPort,
                diplomaEnrollmentPort,
                diplomaMaterializer
        );
    }

    @Test
    void shouldRejectWhenRequesterIsNotSelf() {
        UUID requestedUserId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> useCase.execute(requestedUserId, pageable, requester(UUID.randomUUID())))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void fewerThanThreeAttendancesShouldNotCreateParticipationDiploma() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        when(diplomaEligibilityPort.findParticipationCandidates(userId)).thenReturn(List.of());
        when(diplomaEligibilityPort.findLeadershipCandidates(userId)).thenReturn(List.of());
        when(diplomaRepositoryPort.findPageResponseByUser(eq(userId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0L));

        PageResponse<DiplomaResponse> result = useCase.execute(userId, pageable, requester(userId));

        assertThat(result.getItems()).isEmpty();
        verify(diplomaMaterializer, never()).materializeIfEligible(any(), any(), any());
    }

    @Test
    void exactlyThreeAttendancesShouldCreateParticipationDiploma() {
        UUID userId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        DiplomaEligibilityCandidate candidate = participationCandidate(userId, congressId);
        Diploma persisted = persistedDiploma(userId, congressId, DiplomaType.PARTICIPATION, null);

        when(diplomaEligibilityPort.findParticipationCandidates(userId)).thenReturn(List.of(candidate));
        when(diplomaEligibilityPort.findLeadershipCandidates(userId)).thenReturn(List.of());
        when(diplomaMaterializer.materializeIfEligible(eq(candidate), any(), eq(userId))).thenReturn(persisted);
        when(diplomaRepositoryPort.findPageResponseByUser(eq(userId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(responseFrom(persisted, "Congreso", null, true)), pageable, 1L));

        PageResponse<DiplomaResponse> result = useCase.execute(userId, pageable, requester(userId));

        assertThat(result.getItems()).hasSize(1);
        verify(diplomaMaterializer).materializeIfEligible(eq(candidate), any(), eq(userId));
    }

    @Test
    void leadershipCandidateWithEnrollmentShouldCreateLeadershipDiploma() {
        UUID userId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        DiplomaEligibilityCandidate candidate = leadershipCandidate(userId, congressId, activityId);
        Diploma persisted = persistedDiploma(userId, congressId, DiplomaType.LEADERSHIP, activityId);

        when(diplomaEligibilityPort.findParticipationCandidates(userId)).thenReturn(List.of());
        when(diplomaEligibilityPort.findLeadershipCandidates(userId)).thenReturn(List.of(candidate));
        when(diplomaEnrollmentPort.existsEnrollment(congressId, userId)).thenReturn(true);
        when(diplomaMaterializer.materializeIfEligible(eq(candidate), any(), eq(userId))).thenReturn(persisted);
        when(diplomaRepositoryPort.findPageResponseByUser(eq(userId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(responseFrom(persisted, "Congreso", "Actividad", true)), pageable, 1L));

        PageResponse<DiplomaResponse> result = useCase.execute(userId, pageable, requester(userId));

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getType()).isEqualTo(DiplomaType.LEADERSHIP);
        verify(diplomaMaterializer).materializeIfEligible(eq(candidate), any(), eq(userId));
    }

    @Test
    void leadershipCandidateWithoutEnrollmentShouldNotCreateDiploma() {
        UUID userId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        DiplomaEligibilityCandidate candidate = leadershipCandidate(userId, congressId, activityId);

        when(diplomaEligibilityPort.findParticipationCandidates(userId)).thenReturn(List.of());
        when(diplomaEligibilityPort.findLeadershipCandidates(userId)).thenReturn(List.of(candidate));
        when(diplomaEnrollmentPort.existsEnrollment(congressId, userId)).thenReturn(false);
        when(diplomaRepositoryPort.findPageResponseByUser(eq(userId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0L));

        PageResponse<DiplomaResponse> result = useCase.execute(userId, pageable, requester(userId));

        assertThat(result.getItems()).isEmpty();
        verify(diplomaMaterializer, never()).materializeIfEligible(eq(candidate), any(), eq(userId));
    }

    @Test
    void repeatedListCallsShouldNotDuplicateDiplomas() {
        UUID userId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        DiplomaEligibilityCandidate candidate = participationCandidate(userId, congressId);
        Diploma persisted = persistedDiploma(userId, congressId, DiplomaType.PARTICIPATION, null);
        DiplomaResponse response = responseFrom(persisted, "Congreso", null, true);

        when(diplomaEligibilityPort.findParticipationCandidates(userId)).thenReturn(List.of(candidate));
        when(diplomaEligibilityPort.findLeadershipCandidates(userId)).thenReturn(List.of());
        when(diplomaMaterializer.materializeIfEligible(eq(candidate), any(), eq(userId))).thenReturn(persisted);
        when(diplomaRepositoryPort.findPageResponseByUser(eq(userId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 1L));

        PageResponse<DiplomaResponse> first = useCase.execute(userId, pageable, requester(userId));
        PageResponse<DiplomaResponse> second = useCase.execute(userId, pageable, requester(userId));

        assertThat(first.getTotalItems()).isEqualTo(1);
        assertThat(second.getTotalItems()).isEqualTo(1);
        verify(diplomaMaterializer, times(2)).materializeIfEligible(eq(candidate), any(), eq(userId));
    }

    @Test
    void returnedDiplomasShouldAlwaysBeAvailable() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        DiplomaResponse unavailable = DiplomaResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .congressId(UUID.randomUUID())
                .type(DiplomaType.PARTICIPATION)
                .issuedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .congressName("Congreso")
                .available(false)
                .build();

        when(diplomaEligibilityPort.findParticipationCandidates(userId)).thenReturn(List.of());
        when(diplomaEligibilityPort.findLeadershipCandidates(userId)).thenReturn(List.of());
        when(diplomaRepositoryPort.findPageResponseByUser(eq(userId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(unavailable), pageable, 1L));

        PageResponse<DiplomaResponse> result = useCase.execute(userId, pageable, requester(userId));

        assertThat(result.getItems().getFirst().isAvailable()).isTrue();
    }

    private DiplomaRequesterContext requester(UUID userId) {
        return DiplomaRequesterContext.builder()
                .userId(userId)
                .roles(Set.of(Role.PARTICIPANT))
                .build();
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
                .activityName("Actividad")
                .build();
    }

    private Diploma persistedDiploma(UUID userId, UUID congressId, DiplomaType type, UUID activityId) {
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

    private DiplomaResponse responseFrom(Diploma diploma, String congressName, String activityName, boolean available) {
        return DiplomaResponse.builder()
                .id(diploma.getId())
                .userId(diploma.getUserId())
                .congressId(diploma.getCongressId())
                .type(diploma.getType())
                .activityId(diploma.getActivityId())
                .issuedAt(diploma.getIssuedAt())
                .congressName(congressName)
                .activityName(activityName)
                .available(available)
                .build();
    }
}
