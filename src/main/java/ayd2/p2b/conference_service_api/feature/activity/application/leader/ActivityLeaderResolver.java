package ayd2.p2b.conference_service_api.feature.activity.application.leader;

import ayd2.p2b.conference_service_api.feature.activity.application.exception.ActivityExceptions;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ayd2.p2b.conference_service_api.feature.activity.application.ActivityInputValidator.normalizeLeaders;

@Component
public class ActivityLeaderResolver {

    private static final String ROLE_GUEST_SPEAKER = "GUEST_SPEAKER";
    private static final String ROLE_PARTICIPANT = "PARTICIPANT";

    private final IamUserLookupPort iamUserLookupPort;

    public ActivityLeaderResolver(IamUserLookupPort iamUserLookupPort) {
        this.iamUserLookupPort = iamUserLookupPort;
    }

    public List<ActivityLeader> resolve(ActivityType activityType, List<UUID> leaders, String accessToken) {
        Set<UUID> normalizedLeaderIds = normalizeLeaders(leaders);
        if (normalizedLeaderIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, IamUserSummary> usersSummary = iamUserLookupPort.getUsersSummary(normalizedLeaderIds, accessToken);

        return normalizedLeaderIds.stream()
                .sorted(Comparator.comparing(UUID::toString))
                .map(leaderId -> toActivityLeader(activityType, leaderId, usersSummary.get(leaderId)))
                .toList();
    }

    private ActivityLeader toActivityLeader(ActivityType activityType, UUID leaderId, IamUserSummary summary) {
        if (summary == null || summary.getId() == null || !summary.isActive()) {
            throw ActivityExceptions.activityLeaderNotFound(leaderId);
        }

        Set<String> roles = summary.getRoles() == null
                ? Set.of()
                : summary.getRoles().stream()
                .map(role -> role.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        ActivityLeaderType leaderType = inferLeaderType(activityType, leaderId, roles);
        return ActivityLeader.builder()
                .userId(leaderId)
                .leaderType(leaderType)
                .build();
    }

    private ActivityLeaderType inferLeaderType(ActivityType activityType, UUID leaderId, Set<String> roles) {
        if (roles.contains(ROLE_GUEST_SPEAKER)) {
            return ActivityLeaderType.GUEST_SPEAKER;
        }

        boolean participant = roles.contains(ROLE_PARTICIPANT);
        if (participant && activityType == ActivityType.PONENCIA) {
            return ActivityLeaderType.SPEAKER;
        }
        if (participant && activityType == ActivityType.TALLER) {
            return ActivityLeaderType.WORKSHOP_LEADER;
        }

        throw ActivityExceptions.validationFailed(
                "leader " + leaderId + " has no valid role mapping for activity type " + activityType
        );
    }
}
