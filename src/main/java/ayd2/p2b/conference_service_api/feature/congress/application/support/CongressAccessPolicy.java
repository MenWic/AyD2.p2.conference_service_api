package ayd2.p2b.conference_service_api.feature.congress.application.support;

import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.congress.domain.exception.CongressExceptions;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressRequesterContext;

import java.util.Set;

public final class CongressAccessPolicy {

    private CongressAccessPolicy() {
    }

    public static void ensureCongressAdminWrite(CongressRequesterContext requester) {
        if (requester == null || requester.getUserId() == null) {
            throw CongressExceptions.forbidden("Authenticated congress admin is required");
        }

        Set<Role> roles = requester.getRoles();
        if (roles == null || !roles.contains(Role.CONGRESS_ADMIN) || roles.contains(Role.SYSTEM_ADMIN)) {
            throw CongressExceptions.forbidden("Congress admin role is required");
        }
    }

    public static void ensureCanModifyExisting(
            CongressRequesterContext requester,
            Congress congress,
            boolean linkedToCurrentInstitution
    ) {
        if (requester.getUserId().equals(congress.getCreatedBy())) {
            return;
        }
        if (linkedToCurrentInstitution) {
            return;
        }
        throw CongressExceptions.forbidden("Requester is not owner and not linked to congress institution");
    }
}
