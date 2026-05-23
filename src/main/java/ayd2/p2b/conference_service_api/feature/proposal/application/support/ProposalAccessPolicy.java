package ayd2.p2b.conference_service_api.feature.proposal.application.support;

import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.proposal.application.exception.ProposalExceptions;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalRequesterContext;

import java.util.Set;
import java.util.UUID;

public final class ProposalAccessPolicy {

    private ProposalAccessPolicy() {
    }

    public static void ensureAuthenticated(ProposalRequesterContext requester) {
        if (requester == null || requester.getUserId() == null) {
            throw ProposalExceptions.forbidden("Authenticated user context is required");
        }
    }

    public static void ensureParticipant(ProposalRequesterContext requester) {
        ensureAuthenticated(requester);
        Set<Role> roles = requester.getRoles();
        if (roles == null || !roles.contains(Role.PARTICIPANT)) {
            throw ProposalExceptions.forbidden("Participant role is required");
        }
    }

    public static void ensureSelf(ProposalRequesterContext requester, UUID pathUserId) {
        ensureAuthenticated(requester);
        if (pathUserId == null || !pathUserId.equals(requester.getUserId())) {
            throw ProposalExceptions.forbidden("Requester can only access their own proposals");
        }
    }

    public static boolean canUseScopedCongressAdminPath(ProposalRequesterContext requester) {
        if (requester == null || requester.getUserId() == null) {
            return false;
        }
        Set<Role> roles = requester.getRoles();
        return roles != null && roles.contains(Role.CONGRESS_ADMIN) && !roles.contains(Role.SYSTEM_ADMIN);
    }
}
