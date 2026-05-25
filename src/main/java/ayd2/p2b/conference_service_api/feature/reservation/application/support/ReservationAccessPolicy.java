package ayd2.p2b.conference_service_api.feature.reservation.application.support;

import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.reservation.application.exception.ReservationExceptions;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationRequesterContext;

import java.util.Set;
import java.util.UUID;

public final class ReservationAccessPolicy {

    private ReservationAccessPolicy() {
    }

    public static void ensureAuthenticated(ReservationRequesterContext requester) {
        if (requester == null || requester.getUserId() == null) {
            throw ReservationExceptions.forbidden("Authenticated user context is required");
        }
    }

    public static void ensureParticipant(ReservationRequesterContext requester) {
        ensureAuthenticated(requester);
        Set<Role> roles = requester.getRoles();
        if (roles == null || !roles.contains(Role.PARTICIPANT)) {
            throw ReservationExceptions.forbidden("Participant role is required");
        }
    }

    public static void ensureCongressAdminScopedWrite(ReservationRequesterContext requester) {
        ensureAuthenticated(requester);
        Set<Role> roles = requester.getRoles();
        if (roles == null || !roles.contains(Role.CONGRESS_ADMIN) || roles.contains(Role.SYSTEM_ADMIN)) {
            throw ReservationExceptions.forbidden("Congress admin role is required");
        }
    }

    public static void ensureSelfAccess(UUID requestedUserId, ReservationRequesterContext requester) {
        ensureAuthenticated(requester);
        if (!requester.getUserId().equals(requestedUserId)) {
            throw ReservationExceptions.forbidden("Requester can only access their own reservations");
        }
    }
}
