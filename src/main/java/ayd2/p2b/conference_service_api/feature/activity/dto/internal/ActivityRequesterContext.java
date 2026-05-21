package ayd2.p2b.conference_service_api.feature.activity.dto.internal;

import ayd2.p2b.conference_service_api.core.security.Role;
import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.UUID;

@Value
@Builder
public class ActivityRequesterContext {
    UUID userId;
    Set<Role> roles;
    String accessToken;
}
