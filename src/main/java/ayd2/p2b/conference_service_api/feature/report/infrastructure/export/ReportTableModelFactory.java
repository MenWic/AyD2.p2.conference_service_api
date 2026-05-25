package ayd2.p2b.conference_service_api.feature.report.infrastructure.export;

import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceActivityItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceByActivityReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressByInstitutionItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressesByInstitutionReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipantItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipantsReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.RosterEntry;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationsReportResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReportTableModelFactory {

    public ReportTableModel participants(ParticipantsReportResponse response) {
        List<List<String>> rows = safeList(response == null ? null : response.getItems())
                .stream()
                .map(this::participantToRow)
                .toList();
        return ReportTableModel.builder()
                .title("Reporte de Participantes")
                .headers(List.of("Personal ID", "Nombre", "Organizacion", "Email", "Telefono", "Tipos"))
                .rows(rows)
                .build();
    }

    public ReportTableModel attendanceByActivity(AttendanceByActivityReportResponse response) {
        List<List<String>> rows = safeList(response == null ? null : response.getItems())
                .stream()
                .map(this::attendanceItemToRow)
                .toList();
        return ReportTableModel.builder()
                .title("Asistencia por Actividad")
                .headers(List.of("Actividad", "Sala", "Inicio", "Fin", "Asistentes"))
                .rows(rows)
                .build();
    }

    public ReportTableModel workshopReservations(WorkshopReservationsReportResponse response) {
        List<List<String>> rows = new ArrayList<>();
        for (WorkshopReservationItem item : safeList(response == null ? null : response.getItems())) {
            List<RosterEntry> roster = safeList(item.getRoster());
            if (roster.isEmpty()) {
                rows.add(workshopItemWithRosterToRow(item, null));
                continue;
            }
            for (RosterEntry rosterEntry : roster) {
                rows.add(workshopItemWithRosterToRow(item, rosterEntry));
            }
        }
        return ReportTableModel.builder()
                .title("Reservas de Talleres")
                .headers(List.of("Taller", "Capacidad", "Reservas", "Disponibles",
                        "Personal ID", "Nombre", "Email", "Tipo participación"))
                .rows(rows)
                .build();
    }

    public ReportTableModel congressesByInstitution(CongressesByInstitutionReportResponse response) {
        List<List<String>> rows = safeList(response == null ? null : response.getItems())
                .stream()
                .map(this::congressItemToRow)
                .toList();
        return ReportTableModel.builder()
                .title("Congresos por Institucion")
                .headers(List.of("Institucion", "Congreso", "Inicio", "Fin", "Lugar", "Precio"))
                .rows(rows)
                .build();
    }

    private List<String> participantToRow(ParticipantItem item) {
        String types = item.getParticipationTypes() == null
                ? ""
                : item.getParticipationTypes().stream().map(Enum::name).collect(Collectors.joining(", "));
        return List.of(
                orEmpty(item.getPersonalId()),
                orEmpty(item.getFullName()),
                orEmpty(item.getOrganization()),
                orEmpty(item.getEmail()),
                orEmpty(item.getPhone()),
                types
        );
    }

    private List<String> attendanceItemToRow(AttendanceActivityItem item) {
        return List.of(
                orEmpty(item.getActivityName()),
                orEmpty(item.getRoomName()),
                item.getStartTime() != null ? item.getStartTime().toString() : "",
                item.getEndTime() != null ? item.getEndTime().toString() : "",
                String.valueOf(item.getAttendanceCount())
        );
    }

    private List<String> workshopItemWithRosterToRow(WorkshopReservationItem item, RosterEntry rosterEntry) {
        return List.of(
                orEmpty(item.getActivityName()),
                String.valueOf(item.getWorkshopCapacity()),
                String.valueOf(item.getReservationCount()),
                String.valueOf(item.getAvailableSeats()),
                orEmpty(rosterEntry == null ? null : rosterEntry.getPersonalId()),
                orEmpty(rosterEntry == null ? null : rosterEntry.getFullName()),
                orEmpty(rosterEntry == null ? null : rosterEntry.getEmail()),
                rosterEntry != null && rosterEntry.getParticipationType() != null
                        ? rosterEntry.getParticipationType().name()
                        : ""
        );
    }

    private List<String> congressItemToRow(CongressByInstitutionItem item) {
        return List.of(
                orEmpty(item.getInstitutionName()),
                orEmpty(item.getCongressName()),
                item.getStartDate() != null ? item.getStartDate().toString() : "",
                item.getEndDate() != null ? item.getEndDate().toString() : "",
                orEmpty(item.getLocation()),
                item.getPrice() != null ? item.getPrice().toPlainString() : ""
        );
    }

    private String orEmpty(String value) {
        return value != null ? value : "";
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
