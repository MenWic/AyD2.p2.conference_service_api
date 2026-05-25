package ayd2.p2b.conference_service_api.feature.report.application.support;

import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.report.application.exception.ReportExceptions;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;

public final class ReportAccessPolicy {

    private ReportAccessPolicy() {
    }

    public static void ensureCongressAdmin(ReportRequesterContext requester) {
        if (requester.getRoles() == null || !requester.getRoles().contains(Role.CONGRESS_ADMIN)) {
            throw ReportExceptions.forbidden("Only CONGRESS_ADMIN users can access this report");
        }
    }

    public static void ensureSystemAdmin(ReportRequesterContext requester) {
        if (requester.getRoles() == null || !requester.getRoles().contains(Role.SYSTEM_ADMIN)) {
            throw ReportExceptions.forbidden("Only SYSTEM_ADMIN users can access this report");
        }
    }
}
