package ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations;

import ayd2.p2b.conference_service_api.feature.report.application.exception.ReportExceptions;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsCongressScopePort;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsReportQueryPort;
import ayd2.p2b.conference_service_api.feature.report.application.support.ReportAccessPolicy;
import ayd2.p2b.conference_service_api.feature.report.application.support.ReportParticipationTypeResolver;
import ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.port.WorkshopReservationsQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ParticipantRow;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.WorkshopReservationRow;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipationTypeEnum;
import ayd2.p2b.conference_service_api.feature.report.dto.response.RosterEntry;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationsReportResponse;
import ayd2.p2b.conference_service_api.integration.dto.IamUserDetailSummary;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class WorkshopReservationsReportUseCase {

    private final ParticipantsCongressScopePort congressScopePort;
    private final WorkshopReservationsQueryPort workshopReservationsQueryPort;
    private final ParticipantsReportQueryPort participantsReportQueryPort;
    private final ReportParticipationTypeResolver participationTypeResolver;
    private final IamUserLookupPort iamUserLookupPort;

    public WorkshopReservationsReportUseCase(
            ParticipantsCongressScopePort congressScopePort,
            WorkshopReservationsQueryPort workshopReservationsQueryPort,
            ParticipantsReportQueryPort participantsReportQueryPort,
            ReportParticipationTypeResolver participationTypeResolver,
            IamUserLookupPort iamUserLookupPort) {
        this.congressScopePort = congressScopePort;
        this.workshopReservationsQueryPort = workshopReservationsQueryPort;
        this.participantsReportQueryPort = participantsReportQueryPort;
        this.participationTypeResolver = participationTypeResolver;
        this.iamUserLookupPort = iamUserLookupPort;
    }

    public WorkshopReservationsReportResponse execute(UUID congressId, UUID activityIdFilter,
                                                      ReportRequesterContext requester) {
        ReportAccessPolicy.ensureCongressAdmin(requester);

        if (congressId == null) {
            throw ReportExceptions.missingCongressId();
        }

        CongressInstitutionSummary summary = congressScopePort.findCongressSummary(congressId)
                .orElseThrow(() -> ReportExceptions.congressNotFound(congressId));

        if (!iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(), summary.getInstitutionId(), requester.getAccessToken())) {
            throw ReportExceptions.congressAccessDenied(congressId);
        }

        List<WorkshopReservationRow> rows = workshopReservationsQueryPort.query(congressId, activityIdFilter);
        if (rows.isEmpty()) {
            return WorkshopReservationsReportResponse.builder()
                    .items(List.of())
                    .totalItems(0)
                    .build();
        }

        Set<UUID> reservedUserIds = rows.stream()
                .flatMap(row -> row.getReservedUserIds().stream())
                .filter(userId -> userId != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (reservedUserIds.isEmpty()) {
            List<WorkshopReservationItem> items = rows.stream()
                    .map(row -> WorkshopReservationItem.builder()
                            .activityId(row.getActivityId())
                            .activityName(row.getActivityName())
                            .workshopCapacity(row.getWorkshopCapacity())
                            .reservationCount(0)
                            .availableSeats(Math.max(0, row.getWorkshopCapacity()))
                            .roster(List.of())
                            .build())
                    .toList();
            return WorkshopReservationsReportResponse.builder()
                    .items(items)
                    .totalItems(items.size())
                    .build();
        }

        Map<UUID, IamUserDetailSummary> iamDetails = iamUserLookupPort.getUserDetailsSummary(
                reservedUserIds, requester.getAccessToken());

        Map<UUID, ParticipationTypeEnum> participationTypeByUser = participantsReportQueryPort.findParticipants(congressId)
                .stream()
                .filter(participant -> participant.getUserId() != null && reservedUserIds.contains(participant.getUserId()))
                .collect(Collectors.toMap(
                        ParticipantRow::getUserId,
                        participant -> participationTypeResolver.resolvePrimaryType(participant.getParticipationTypes()),
                        (left, right) -> left
                ));

        List<WorkshopReservationItem> items = rows.stream()
                .map(row -> mapItem(row, iamDetails, participationTypeByUser))
                .toList();

        return WorkshopReservationsReportResponse.builder()
                .items(items)
                .totalItems(items.size())
                .build();
    }

    private WorkshopReservationItem mapItem(
            WorkshopReservationRow row,
            Map<UUID, IamUserDetailSummary> iamDetails,
            Map<UUID, ParticipationTypeEnum> participationTypeByUser
    ) {
        List<UUID> reservedUserIds = row.getReservedUserIds() == null ? List.of() : row.getReservedUserIds();
        int reservationCount = reservedUserIds.size();
        int availableSeats = Math.max(0, row.getWorkshopCapacity() - reservationCount);

        List<RosterEntry> roster = reservedUserIds.stream()
                .map(userId -> mapRosterEntry(userId, iamDetails, participationTypeByUser))
                .toList();

        return WorkshopReservationItem.builder()
                .activityId(row.getActivityId())
                .activityName(row.getActivityName())
                .workshopCapacity(row.getWorkshopCapacity())
                .reservationCount(reservationCount)
                .availableSeats(availableSeats)
                .roster(roster)
                .build();
    }

    private RosterEntry mapRosterEntry(
            UUID userId,
            Map<UUID, IamUserDetailSummary> iamDetails,
            Map<UUID, ParticipationTypeEnum> participationTypeByUser
    ) {
        IamUserDetailSummary profile = iamDetails.get(userId);
        if (profile == null || profile.getId() == null) {
            throw IntegrationExceptions.iamUnavailable("IAM profile not found for reserved user: " + userId);
        }

        String personalId = normalizeOrFail(profile.getPersonalId(), userId, "personalId");
        String fullName = normalizeOrFail(profile.getFullName(), userId, "fullName");
        String email = normalizeOrFail(profile.getEmail(), userId, "email");
        ParticipationTypeEnum participationType = participationTypeByUser.getOrDefault(userId, ParticipationTypeEnum.ENROLLED);

        return RosterEntry.builder()
                .personalId(personalId)
                .fullName(fullName)
                .email(email)
                .participationType(participationType)
                .build();
    }

    private String normalizeOrFail(String value, UUID userId, String fieldName) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw IntegrationExceptions.iamUnavailable(
                    "IAM profile field '" + fieldName + "' is missing for reserved user: " + userId);
        }
        return normalized;
    }
}
