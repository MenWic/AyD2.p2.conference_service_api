package ayd2.p2b.conference_service_api.feature.reservation.application.port;

import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationActivitySummary;

import java.util.Optional;
import java.util.UUID;

public interface ReservationActivityPort {

    Optional<ReservationActivitySummary> findActivityById(UUID activityId);
}
