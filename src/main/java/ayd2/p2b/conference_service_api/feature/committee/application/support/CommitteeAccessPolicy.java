package ayd2.p2b.conference_service_api.feature.committee.application.support;

import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.committee.application.exception.CommitteeExceptions;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeCongressSummary;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeRequesterContext;

import java.util.Set;

public final class CommitteeAccessPolicy {

    private CommitteeAccessPolicy() {
    }

    public static void ensureCongressAdminWrite(CommitteeRequesterContext requester) {
        if (requester == null || requester.getUserId() == null) {
            throw CommitteeExceptions.forbidden("Authenticated congress admin is required");
        }

        Set<Role> roles = requester.getRoles();
        if (roles == null || !roles.contains(Role.CONGRESS_ADMIN) || roles.contains(Role.SYSTEM_ADMIN)) {
            throw CommitteeExceptions.forbidden("Congress admin role is required");
        }
    }

    public static void ensureReadRole(CommitteeRequesterContext requester) {
        if (requester == null || requester.getUserId() == null) {
            throw CommitteeExceptions.forbidden("Authenticated requester is required");
        }
        Set<Role> roles = requester.getRoles();
        if (roles == null || (!roles.contains(Role.CONGRESS_ADMIN) && !roles.contains(Role.SYSTEM_ADMIN))) {
            throw CommitteeExceptions.forbidden("System admin or congress admin role is required");
        }
    }

    public static boolean isSystemAdmin(CommitteeRequesterContext requester) {
        Set<Role> roles = requester.getRoles();
        return roles != null && roles.contains(Role.SYSTEM_ADMIN);
    }

    public static void ensureCanManageCongress(
            CommitteeRequesterContext requester,
            CommitteeCongressSummary congressSummary,
            boolean linkedToInstitution
    ) {
        if (isSystemAdmin(requester)) {
            return;
        }
        if (requester.getUserId().equals(congressSummary.getCreatedBy())) {
            return;
        }
        if (linkedToInstitution) {
            return;
        }
        throw CommitteeExceptions.forbidden("Requester is not owner and not linked to congress institution");
    }
}
