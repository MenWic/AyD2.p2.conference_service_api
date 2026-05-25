package ayd2.p2b.conference_service_api.feature.attendance.application.support;

import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.attendance.application.exception.AttendanceExceptions;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceRequesterContext;

import java.util.Set;

public final class AttendanceAccessPolicy {

    private AttendanceAccessPolicy() {
    }

    public static void ensureCongressAdminScoped(AttendanceRequesterContext requester) {
        if (requester == null || requester.getUserId() == null) {
            throw AttendanceExceptions.forbidden("Authenticated congress admin context is required");
        }
        Set<Role> roles = requester.getRoles();
        if (roles == null || !roles.contains(Role.CONGRESS_ADMIN) || roles.contains(Role.SYSTEM_ADMIN)) {
            throw AttendanceExceptions.forbidden("Congress admin role is required");
        }
    }
}
