package ayd2.p2b.conference_service_api.unit.feature.activity.leader;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.activity.application.leader.ActivityLeaderResolver;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLeaderResolverTest {

    @Mock
    private IamUserLookupPort iamUserLookupPort;

    private ActivityLeaderResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ActivityLeaderResolver(iamUserLookupPort);
    }

    @Test
    void shouldRejectDuplicateLeaders() {
        UUID leaderId = UUID.randomUUID();

        assertThatThrownBy(() -> resolver.resolve(
                ActivityType.TALLER,
                List.of(leaderId, leaderId),
                "token"
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidation((ApiException) ex));

        verify(iamUserLookupPort, never()).getUsersSummary(any(), any());
    }

    @Test
    void shouldRejectMissingIamUserAsNotFound() {
        UUID leaderId = UUID.randomUUID();
        when(iamUserLookupPort.getUsersSummary(eq(Set.of(leaderId)), eq("token")))
                .thenReturn(Map.of());

        assertThatThrownBy(() -> resolver.resolve(ActivityType.PONENCIA, List.of(leaderId), "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertNotFound((ApiException) ex));
    }

    @Test
    void shouldRejectInactiveIamUserAsNotFound() {
        UUID leaderId = UUID.randomUUID();
        when(iamUserLookupPort.getUsersSummary(eq(Set.of(leaderId)), eq("token")))
                .thenReturn(Map.of(leaderId, userSummary(leaderId, false, Set.of("PARTICIPANT"))));

        assertThatThrownBy(() -> resolver.resolve(ActivityType.PONENCIA, List.of(leaderId), "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertNotFound((ApiException) ex));
    }

    @Test
    void shouldMapGuestSpeakerRoleToGuestSpeakerLeaderType() {
        UUID leaderId = UUID.randomUUID();
        when(iamUserLookupPort.getUsersSummary(eq(Set.of(leaderId)), eq("token")))
                .thenReturn(Map.of(leaderId, userSummary(leaderId, true, Set.of("GUEST_SPEAKER"))));

        List<ActivityLeader> leaders = resolver.resolve(ActivityType.PONENCIA, List.of(leaderId), "token");

        assertThat(leaders).hasSize(1);
        assertThat(leaders.getFirst().getLeaderType()).isEqualTo(ActivityLeaderType.GUEST_SPEAKER);
    }

    @Test
    void shouldMapParticipantToSpeakerForPonencia() {
        UUID leaderId = UUID.randomUUID();
        when(iamUserLookupPort.getUsersSummary(eq(Set.of(leaderId)), eq("token")))
                .thenReturn(Map.of(leaderId, userSummary(leaderId, true, Set.of("PARTICIPANT"))));

        List<ActivityLeader> leaders = resolver.resolve(ActivityType.PONENCIA, List.of(leaderId), "token");

        assertThat(leaders.getFirst().getLeaderType()).isEqualTo(ActivityLeaderType.SPEAKER);
    }

    @Test
    void shouldMapParticipantToWorkshopLeaderForTaller() {
        UUID leaderId = UUID.randomUUID();
        when(iamUserLookupPort.getUsersSummary(eq(Set.of(leaderId)), eq("token")))
                .thenReturn(Map.of(leaderId, userSummary(leaderId, true, Set.of("PARTICIPANT"))));

        List<ActivityLeader> leaders = resolver.resolve(ActivityType.TALLER, List.of(leaderId), "token");

        assertThat(leaders.getFirst().getLeaderType()).isEqualTo(ActivityLeaderType.WORKSHOP_LEADER);
    }

    @Test
    void shouldMapMixedCaseRolesWithLocaleRootNormalization() {
        UUID leaderId = UUID.randomUUID();
        when(iamUserLookupPort.getUsersSummary(eq(Set.of(leaderId)), eq("token")))
                .thenReturn(Map.of(leaderId, userSummary(leaderId, true, Set.of("gUeSt_sPeAkEr"))));

        List<ActivityLeader> leaders = resolver.resolve(ActivityType.TALLER, List.of(leaderId), "token");

        assertThat(leaders.getFirst().getLeaderType()).isEqualTo(ActivityLeaderType.GUEST_SPEAKER);
    }

    @Test
    void shouldMapRolesCorrectlyWhenDefaultLocaleIsTurkish() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            UUID leaderId = UUID.randomUUID();
            when(iamUserLookupPort.getUsersSummary(eq(Set.of(leaderId)), eq("token")))
                    .thenReturn(Map.of(leaderId, userSummary(leaderId, true, Set.of("participant"))));

            List<ActivityLeader> leaders = resolver.resolve(ActivityType.PONENCIA, List.of(leaderId), "token");

            assertThat(leaders.getFirst().getLeaderType()).isEqualTo(ActivityLeaderType.SPEAKER);
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void shouldReturnLeadersInDeterministicUuidOrder() {
        UUID high = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        UUID low = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Set<UUID> ids = Set.of(high, low);
        when(iamUserLookupPort.getUsersSummary(eq(ids), eq("token")))
                .thenReturn(Map.of(
                        low, userSummary(low, true, Set.of("PARTICIPANT")),
                        high, userSummary(high, true, Set.of("PARTICIPANT"))
                ));

        List<ActivityLeader> leaders = resolver.resolve(ActivityType.TALLER, new ArrayList<>(ids), "token");

        assertThat(leaders).extracting(ActivityLeader::getUserId).containsExactly(low, high);
    }

    @Test
    void shouldRejectInvalidLeaderRole() {
        UUID leaderId = UUID.randomUUID();
        when(iamUserLookupPort.getUsersSummary(eq(Set.of(leaderId)), eq("token")))
                .thenReturn(Map.of(leaderId, userSummary(leaderId, true, Set.of("CONGRESS_ADMIN"))));

        assertThatThrownBy(() -> resolver.resolve(ActivityType.TALLER, List.of(leaderId), "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidation((ApiException) ex));
    }

    private IamUserSummary userSummary(UUID id, boolean active, Set<String> roles) {
        return IamUserSummary.builder()
                .id(id)
                .active(active)
                .roles(roles)
                .build();
    }

    private void assertNotFound(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCode()).isEqualTo("resource.not_found");
    }

    private void assertValidation(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("validation.failed");
    }
}
