package ayd2.p2b.conference_service_api.feature.room.application.support;

import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.room.application.exception.RoomExceptions;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomCongressSummary;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomRequesterContext;

import java.util.Set;

public final class RoomAccessPolicy {

    private RoomAccessPolicy() {
    }

    public static void ensureCongressAdminWrite(RoomRequesterContext requester) {
        if (requester == null || requester.getUserId() == null) {
            throw RoomExceptions.forbidden("Authenticated congress admin is required");
        }

        Set<Role> roles = requester.getRoles();
        if (roles == null || !roles.contains(Role.CONGRESS_ADMIN) || roles.contains(Role.SYSTEM_ADMIN)) {
            throw RoomExceptions.forbidden("Congress admin role is required");
        }
    }

    public static void ensureCanManageCongress(
            RoomRequesterContext requester,
            RoomCongressSummary congress,
            boolean linkedToInstitution
    ) {
        if (requester.getUserId().equals(congress.getCreatedBy())) {
            return;
        }
        if (linkedToInstitution) {
            return;
        }
        throw RoomExceptions.forbidden("Requester is not owner and not linked to congress institution");
    }
}
