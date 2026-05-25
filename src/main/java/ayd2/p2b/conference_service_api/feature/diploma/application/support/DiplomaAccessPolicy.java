package ayd2.p2b.conference_service_api.feature.diploma.application.support;

import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.diploma.application.exception.DiplomaExceptions;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaRequesterContext;

import java.util.Set;
import java.util.UUID;

public final class DiplomaAccessPolicy {

    private DiplomaAccessPolicy() {
    }

    public static void ensureAuthenticated(DiplomaRequesterContext requester) {
        if (requester == null || requester.getUserId() == null) {
            throw DiplomaExceptions.forbidden("Authenticated participant context is required");
        }
    }

    public static void ensureParticipant(DiplomaRequesterContext requester) {
        ensureAuthenticated(requester);
        Set<Role> roles = requester.getRoles();
        if (roles == null || !roles.contains(Role.PARTICIPANT)) {
            throw DiplomaExceptions.forbidden("Participant role is required");
        }
    }

    public static void ensureSelf(UUID requestedUserId, DiplomaRequesterContext requester) {
        ensureParticipant(requester);
        if (!requester.getUserId().equals(requestedUserId)) {
            throw DiplomaExceptions.forbidden("Requester can only access their own diplomas");
        }
    }

    public static void ensureOwner(UUID diplomaUserId, DiplomaRequesterContext requester) {
        ensureParticipant(requester);
        if (!requester.getUserId().equals(diplomaUserId)) {
            throw DiplomaExceptions.forbidden("Requester is not owner of this diploma");
        }
    }
}
