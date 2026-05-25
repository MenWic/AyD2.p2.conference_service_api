package ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.port;

import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationItem;

import java.util.List;
import java.util.UUID;

public interface WorkshopReservationsQueryPort {
    List<WorkshopReservationItem> query(UUID congressId, UUID activityIdFilter);
}
