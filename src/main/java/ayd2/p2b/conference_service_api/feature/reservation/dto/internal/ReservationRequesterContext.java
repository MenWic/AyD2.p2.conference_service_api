package ayd2.p2b.conference_service_api.feature.reservation.dto.internal;

import ayd2.p2b.conference_service_api.core.security.Role;
import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.UUID;

@Value
@Builder
public class ReservationRequesterContext {
    UUID userId;
    Set<Role> roles;
    String accessToken;
}
