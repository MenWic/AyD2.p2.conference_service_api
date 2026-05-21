package ayd2.p2b.conference_service_api.feature.activity.application.support;

import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.activity.application.exception.ActivityExceptions;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityCongressRoomSummary;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityRequesterContext;

import java.util.Set;

public final class ActivityAccessPolicy {

    private ActivityAccessPolicy() {
    }

    public static void ensureCongressAdminWrite(ActivityRequesterContext requester) {
        if (requester == null || requester.getUserId() == null) {
            throw ActivityExceptions.forbidden("Authenticated congress admin is required");
        }

        Set<Role> roles = requester.getRoles();
        if (roles == null || !roles.contains(Role.CONGRESS_ADMIN) || roles.contains(Role.SYSTEM_ADMIN)) {
            throw ActivityExceptions.forbidden("Congress admin role is required");
        }
    }

    public static void ensureCanManageActivity(
            ActivityRequesterContext requester,
            ActivityCongressRoomSummary congressSummary,
            boolean linkedToInstitution
    ) {
        if (requester.getUserId().equals(congressSummary.getCreatedBy())) {
            return;
        }
        if (linkedToInstitution) {
            return;
        }
        throw ActivityExceptions.forbidden("Requester is not owner and not linked to congress institution");
    }
}
