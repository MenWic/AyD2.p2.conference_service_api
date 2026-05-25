package ayd2.p2b.conference_service_api.unit.feature.report.infrastructure.export;

import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceActivityItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceByActivityReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressByInstitutionItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressesByInstitutionReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipantItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipantsReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipationTypeEnum;
import ayd2.p2b.conference_service_api.feature.report.dto.response.RosterEntry;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationsReportResponse;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.export.ReportTableModel;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.export.ReportTableModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportTableModelFactoryTest {

    private ReportTableModelFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ReportTableModelFactory();
    }

    @Test
    void participants_maps_all_columns_and_types() {
        ParticipantsReportResponse response = ParticipantsReportResponse.builder()
                .items(List.of(ParticipantItem.builder()
                        .personalId("A-001")
                        .fullName("Ana Lopez")
                        .organization("USAC")
                        .email("ana@test.com")
                        .phone("555-1000")
                        .participationTypes(List.of(ParticipationTypeEnum.ENROLLED, ParticipationTypeEnum.SPEAKER))
                        .build()))
                .totalItems(1)
                .build();

        ReportTableModel table = factory.participants(response);

        assertThat(table.getTitle()).isEqualTo("Reporte de Participantes");
        assertThat(table.getHeaders()).containsExactly("Personal ID", "Nombre", "Organizacion", "Email", "Telefono", "Tipos");
        assertThat(table.getRows()).hasSize(1);
        assertThat(table.getRows().get(0)).containsExactly("A-001", "Ana Lopez", "USAC", "ana@test.com", "555-1000", "ENROLLED, SPEAKER");
    }

    @Test
    void attendance_handles_null_timestamps_and_strings() {
        AttendanceByActivityReportResponse response = AttendanceByActivityReportResponse.builder()
                .items(List.of(AttendanceActivityItem.builder()
                        .activityId(UUID.randomUUID())
                        .activityName(null)
                        .roomName(null)
                        .startTime(null)
                        .endTime(null)
                        .attendanceCount(0)
                        .build()))
                .totalItems(1)
                .build();

        ReportTableModel table = factory.attendanceByActivity(response);

        assertThat(table.getRows().get(0)).containsExactly("", "", "", "", "0");
    }

    @Test
    void workshop_maps_capacity_roster_and_participation_type() {
        WorkshopReservationsReportResponse response = WorkshopReservationsReportResponse.builder()
                .items(List.of(WorkshopReservationItem.builder()
                        .activityId(UUID.randomUUID())
                        .activityName("Java Workshop")
                        .workshopCapacity(30)
                        .reservationCount(12)
                        .availableSeats(18)
                        .roster(List.of(RosterEntry.builder()
                                .personalId("A-1001")
                                .fullName("Ana Lopez")
                                .email("ana@test.com")
                                .participationType(ParticipationTypeEnum.SPEAKER)
                                .build()))
                        .build()))
                .totalItems(1)
                .build();

        ReportTableModel table = factory.workshopReservations(response);

        assertThat(table.getHeaders()).containsExactly("Taller", "Capacidad", "Reservas", "Disponibles",
                "Personal ID", "Nombre", "Email", "Tipo participación");
        assertThat(table.getRows()).hasSize(1);
        assertThat(table.getRows().get(0)).containsExactly(
                "Java Workshop", "30", "12", "18", "A-1001", "Ana Lopez", "ana@test.com", "SPEAKER"
        );
    }

    @Test
    void workshop_without_roster_creates_one_row_with_blank_roster_columns() {
        WorkshopReservationsReportResponse response = WorkshopReservationsReportResponse.builder()
                .items(List.of(WorkshopReservationItem.builder()
                        .activityId(UUID.randomUUID())
                        .activityName("Java Workshop")
                        .workshopCapacity(30)
                        .reservationCount(0)
                        .availableSeats(30)
                        .roster(List.of())
                        .build()))
                .totalItems(1)
                .build();

        ReportTableModel table = factory.workshopReservations(response);

        assertThat(table.getRows()).hasSize(1);
        assertThat(table.getRows().get(0)).containsExactly(
                "Java Workshop", "30", "0", "30", "", "", "", ""
        );
    }

    @Test
    void workshop_with_multiple_roster_entries_creates_one_row_per_roster_entry() {
        WorkshopReservationsReportResponse response = WorkshopReservationsReportResponse.builder()
                .items(List.of(WorkshopReservationItem.builder()
                        .activityId(UUID.randomUUID())
                        .activityName("Java Workshop")
                        .workshopCapacity(30)
                        .reservationCount(2)
                        .availableSeats(28)
                        .roster(List.of(
                                RosterEntry.builder()
                                        .personalId("A-1001")
                                        .fullName("Ana Lopez")
                                        .email("ana@test.com")
                                        .participationType(ParticipationTypeEnum.SPEAKER)
                                        .build(),
                                RosterEntry.builder()
                                        .personalId("B-2002")
                                        .fullName("Luis Perez")
                                        .email("luis@test.com")
                                        .participationType(ParticipationTypeEnum.ENROLLED)
                                        .build()
                        ))
                        .build()))
                .totalItems(1)
                .build();

        ReportTableModel table = factory.workshopReservations(response);

        assertThat(table.getRows()).hasSize(2);
        assertThat(table.getRows().get(0)).containsExactly(
                "Java Workshop", "30", "2", "28", "A-1001", "Ana Lopez", "ana@test.com", "SPEAKER"
        );
        assertThat(table.getRows().get(1)).containsExactly(
                "Java Workshop", "30", "2", "28", "B-2002", "Luis Perez", "luis@test.com", "ENROLLED"
        );
    }

    @Test
    void congresses_maps_dates_location_and_price() {
        CongressesByInstitutionReportResponse response = CongressesByInstitutionReportResponse.builder()
                .items(List.of(CongressByInstitutionItem.builder()
                        .institutionName("USAC")
                        .congressName("AydConf")
                        .startDate(LocalDate.of(2026, 6, 1))
                        .endDate(LocalDate.of(2026, 6, 5))
                        .location("Guatemala")
                        .price(new BigDecimal("100.00"))
                        .build()))
                .totalItems(1)
                .build();

        ReportTableModel table = factory.congressesByInstitution(response);

        assertThat(table.getRows().get(0)).containsExactly("USAC", "AydConf", "2026-06-01", "2026-06-05", "Guatemala", "100.00");
    }

    @Test
    void participants_with_null_response_items_returns_empty_rows() {
        ParticipantsReportResponse response = ParticipantsReportResponse.builder()
                .items(null)
                .totalItems(0)
                .build();

        ReportTableModel table = factory.participants(response);

        assertThat(table.getRows()).isEmpty();
    }
}
