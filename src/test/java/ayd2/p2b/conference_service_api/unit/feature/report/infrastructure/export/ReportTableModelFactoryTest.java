package ayd2.p2b.conference_service_api.unit.feature.report.infrastructure.export;

import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceActivityItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceByActivityReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressByInstitutionItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressesByInstitutionReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsByCongressItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsByCongressReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsCongressItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsInstitutionItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsReportResponse;
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

        assertThat(table.getHeaders()).containsExactly("Institucion", "Congreso", "Inicio", "Fin", "Lugar", "Precio");
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

    @Test
    void congresses_with_null_response_items_returns_empty_rows() {
        CongressesByInstitutionReportResponse response = CongressesByInstitutionReportResponse.builder()
                .items(null)
                .totalItems(0)
                .build();

        ReportTableModel table = factory.congressesByInstitution(response);

        assertThat(table.getRows()).isEmpty();
    }

    @Test
    void earnings_by_congress_headers_and_values_are_mapped() {
        EarningsByCongressReportResponse response = EarningsByCongressReportResponse.builder()
                .items(List.of(EarningsByCongressItem.builder()
                        .congressName("AydConf 2026")
                        .totalAmount(new BigDecimal("1234.50"))
                        .commissionAmount(new BigDecimal("123.45"))
                        .netAmount(new BigDecimal("1111.05"))
                        .paymentCount(20)
                        .build()))
                .totalItems(1)
                .build();

        ReportTableModel table = factory.earningsByCongress(response);

        assertThat(table.getHeaders()).containsExactly("Congreso", "Total", "Comisión", "Neto", "Pagos");
        assertThat(table.getRows()).hasSize(1);
        assertThat(table.getRows().get(0)).containsExactly("AydConf 2026", "1234.50", "123.45", "1111.05", "20");
    }

    @Test
    void platform_earnings_nested_rows_render_one_row_per_institution_and_congress() {
        EarningsReportResponse response = EarningsReportResponse.builder()
                .items(List.of(EarningsInstitutionItem.builder()
                        .institutionName("USAC")
                        .congresses(List.of(
                                EarningsCongressItem.builder()
                                        .congressName("AydConf 2026")
                                        .totalAmount(new BigDecimal("1000.00"))
                                        .commissionAmount(new BigDecimal("100.00"))
                                        .netAmount(new BigDecimal("900.00"))
                                        .paymentCount(10)
                                        .build(),
                                EarningsCongressItem.builder()
                                        .congressName("AydConf 2027")
                                        .totalAmount(new BigDecimal("2000.00"))
                                        .commissionAmount(new BigDecimal("200.00"))
                                        .netAmount(new BigDecimal("1800.00"))
                                        .paymentCount(20)
                                        .build()
                        ))
                        .build()))
                .totalItems(1)
                .build();

        ReportTableModel table = factory.earnings(response);

        assertThat(table.getHeaders()).containsExactly("Institución", "Congreso", "Total", "Comisión", "Neto", "Pagos");
        assertThat(table.getRows()).hasSize(2);
        assertThat(table.getRows().get(0)).containsExactly("USAC", "AydConf 2026", "1000.00", "100.00", "900.00", "10");
        assertThat(table.getRows().get(1)).containsExactly("USAC", "AydConf 2027", "2000.00", "200.00", "1800.00", "20");
    }

    @Test
    void platform_earnings_institution_without_congresses_renders_single_totals_row() {
        EarningsReportResponse response = EarningsReportResponse.builder()
                .items(List.of(EarningsInstitutionItem.builder()
                        .institutionName("Landivar")
                        .congresses(List.of())
                        .institutionTotalAmount(new BigDecimal("300.00"))
                        .institutionTotalCommission(new BigDecimal("30.00"))
                        .institutionTotalNet(new BigDecimal("270.00"))
                        .paymentCount(3)
                        .build()))
                .totalItems(1)
                .build();

        ReportTableModel table = factory.earnings(response);

        assertThat(table.getRows()).hasSize(1);
        assertThat(table.getRows().get(0)).containsExactly("Landivar", "", "300.00", "30.00", "270.00", "3");
    }
}
