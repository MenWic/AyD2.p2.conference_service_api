package ayd2.p2b.conference_service_api.feature.call.application.support;

import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.call.application.exception.CallExceptions;
import ayd2.p2b.conference_service_api.feature.call.dto.internal.CallCongressSummary;
import ayd2.p2b.conference_service_api.feature.call.dto.internal.CallRequesterContext;

import java.util.Set;

public final class CallAccessPolicy {

    private CallAccessPolicy() {
    }

    public static void ensureCongressAdminWrite(CallRequesterContext requester) {
        if (requester == null || requester.getUserId() == null) {
            throw CallExceptions.forbidden("Authenticated congress admin is required");
        }

        Set<Role> roles = requester.getRoles();
        if (roles == null || !roles.contains(Role.CONGRESS_ADMIN) || roles.contains(Role.SYSTEM_ADMIN)) {
            throw CallExceptions.forbidden("Congress admin role is required");
        }
    }

    public static void ensureCanManageCongress(
            CallRequesterContext requester,
            CallCongressSummary congressSummary,
            boolean linkedToInstitution
    ) {
        if (requester.getUserId().equals(congressSummary.getCreatedBy())) {
            return;
        }
        if (linkedToInstitution) {
            return;
        }
        throw CallExceptions.forbidden("Requester is not owner and not linked to congress institution");
    }
}
