package ayd2.p2b.conference_service_api.feature.attendance.application.register;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.attendance.application.exception.AttendanceExceptions;
import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceActivityPort;
import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceEnrollmentPort;
import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceRepositoryPort;
import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceReservationPort;
import ayd2.p2b.conference_service_api.feature.attendance.application.support.AttendanceAccessPolicy;
import ayd2.p2b.conference_service_api.feature.attendance.domain.model.Attendance;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceActivitySummary;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceRequesterContext;
import ayd2.p2b.conference_service_api.feature.attendance.dto.request.RegisterAttendanceRequest;
import ayd2.p2b.conference_service_api.feature.attendance.dto.response.AttendanceResponse;
import ayd2.p2b.conference_service_api.feature.attendance.mapper.AttendanceMapper;
import ayd2.p2b.conference_service_api.integration.dto.IamPersonalIdUserSummary;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RegisterAttendanceUseCase {

    private final AttendanceRepositoryPort attendanceRepositoryPort;
    private final AttendanceActivityPort attendanceActivityPort;
    private final AttendanceEnrollmentPort attendanceEnrollmentPort;
    private final AttendanceReservationPort attendanceReservationPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final AttendanceMapper attendanceMapper;

    public RegisterAttendanceUseCase(
            AttendanceRepositoryPort attendanceRepositoryPort,
            AttendanceActivityPort attendanceActivityPort,
            AttendanceEnrollmentPort attendanceEnrollmentPort,
            AttendanceReservationPort attendanceReservationPort,
            IamUserLookupPort iamUserLookupPort,
            AttendanceMapper attendanceMapper
    ) {
        this.attendanceRepositoryPort = attendanceRepositoryPort;
        this.attendanceActivityPort = attendanceActivityPort;
        this.attendanceEnrollmentPort = attendanceEnrollmentPort;
        this.attendanceReservationPort = attendanceReservationPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.attendanceMapper = attendanceMapper;
    }

    public AttendanceResponse execute(RegisterAttendanceRequest request, AttendanceRequesterContext requester) {
        AttendanceAccessPolicy.ensureCongressAdminScoped(requester);
        String normalizedPersonalId = normalizePersonalId(request.getPersonalId());

        IamPersonalIdUserSummary attendee = iamUserLookupPort.findUserByPersonalId(normalizedPersonalId)
                .orElseThrow(() -> AttendanceExceptions.participantNotFoundByPersonalId(normalizedPersonalId));

        UUID userId = attendee.getUserId();
        AttendanceActivitySummary activity = attendanceActivityPort.findActivityById(request.getActivityId())
                .orElseThrow(() -> AttendanceExceptions.activityNotFound(request.getActivityId()));

        authorizeManageAccess(requester, activity);

        if (!attendanceEnrollmentPort.existsEnrollment(activity.getCongressId(), userId)) {
            throw AttendanceExceptions.enrollmentRequired(activity.getActivityId(), userId);
        }

        if (activity.getType() == ActivityType.TALLER
                && !attendanceReservationPort.existsReservation(activity.getActivityId(), userId)) {
            throw AttendanceExceptions.reservationRequiredForWorkshop(activity.getActivityId(), userId);
        }

        if (attendanceRepositoryPort.existsByActivityIdAndUserId(activity.getActivityId(), userId)) {
            throw AttendanceExceptions.duplicateAttendance(activity.getActivityId(), userId);
        }

        Attendance attendance = Attendance.builder()
                .activityId(activity.getActivityId())
                .userId(userId)
                .personalIdSnapshot(attendee.getPersonalId())
                .registeredBy(requester.getUserId())
                .createdBy(requester.getUserId())
                .build();
        attendance.validateInvariants();

        try {
            return attendanceMapper.toResponse(attendanceRepositoryPort.save(attendance));
        } catch (DataIntegrityViolationException ex) {
            throw AttendanceExceptions.duplicateAttendance(activity.getActivityId(), userId);
        }
    }

    private void authorizeManageAccess(AttendanceRequesterContext requester, AttendanceActivitySummary activity) {
        if (requester.getUserId().equals(activity.getCongressCreatedBy())) {
            return;
        }

        boolean linked = iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(),
                activity.getInstitutionId(),
                requester.getAccessToken());
        if (!linked) {
            throw AttendanceExceptions.forbidden("Requester is not owner and not linked to congress institution");
        }
    }

    private String normalizePersonalId(String personalId) {
        if (personalId == null || personalId.trim().isBlank()) {
            throw AttendanceExceptions.validationFailed("personalId is required");
        }
        return personalId.trim();
    }
}
