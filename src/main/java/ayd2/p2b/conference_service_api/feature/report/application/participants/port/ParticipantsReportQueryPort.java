package ayd2.p2b.conference_service_api.feature.report.application.participants.port;

import ayd2.p2b.conference_service_api.feature.report.dto.internal.ParticipantRow;

import java.util.List;
import java.util.UUID;

public interface ParticipantsReportQueryPort {
    List<ParticipantRow> findParticipants(UUID congressId);
}
