package ayd2.p2b.conference_service_api.feature.activity.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ActivityLeader {
    UUID userId;
    ActivityLeaderType leaderType;
}
