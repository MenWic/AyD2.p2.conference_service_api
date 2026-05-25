package ayd2.p2b.conference_service_api.feature.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopReservationItem {
    private UUID activityId;
    private String activityName;
    private int workshopCapacity;
    private int reservationCount;
    private int availableSeats;
    private List<RosterEntry> roster;
}
