package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.feature.report.application.attendance_summary.AttendanceByActivityReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.congresses_by_institution.CongressesByInstitutionReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.participants.ParticipantsReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.WorkshopReservationsReportUseCase;
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
import ayd2.p2b.conference_service_api.feature.report.infrastructure.export.HtmlReportExporter;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.export.ReportTableModel;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.export.ReportTableModelFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
        "security.jwt.secret=test_secret_key_with_at_least_32_chars",
        "spring.main.lazy-initialization=true"
})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParticipantsReportUseCase participantsReportUseCase;
    @MockitoBean
    private AttendanceByActivityReportUseCase attendanceByActivityReportUseCase;
    @MockitoBean
    private WorkshopReservationsReportUseCase workshopReservationsReportUseCase;
    @MockitoBean
    private CongressesByInstitutionReportUseCase congressesByInstitutionReportUseCase;
    @MockitoBean
    private HtmlReportExporter htmlReportExporter;
    @MockitoBean
    private ReportTableModelFactory reportTableModelFactory;

    private static final String SECRET = "test_secret_key_with_at_least_32_chars";
    private static final UUID CONGRESS_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void participants_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/reports/participants")
                        .param("congressId", CONGRESS_ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void participants_with_participant_token_returns_403() throws Exception {
        String token = buildToken(USER_ID, "PARTICIPANT");
        when(participantsReportUseCase.execute(any(), any(), any()))
                .thenThrow(new ayd2.p2b.conference_service_api.common.exception.ApiException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "auth.forbidden", "Only CONGRESS_ADMIN"));

        mockMvc.perform(get("/reports/participants")
                        .param("congressId", CONGRESS_ID.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void participants_with_congress_admin_token_returns_200_json() throws Exception {
        String token = buildToken(USER_ID, "CONGRESS_ADMIN");
        when(participantsReportUseCase.execute(any(), any(), any()))
                .thenReturn(ParticipantsReportResponse.builder().items(List.of()).totalItems(0).build());

        mockMvc.perform(get("/reports/participants")
                        .param("congressId", CONGRESS_ID.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void participants_with_html_format_returns_html_content_type() throws Exception {
        String token = buildToken(USER_ID, "CONGRESS_ADMIN");
        when(participantsReportUseCase.execute(any(), any(), any()))
                .thenReturn(ParticipantsReportResponse.builder().items(List.of()).totalItems(0).build());
        when(reportTableModelFactory.participants(any())).thenReturn(sampleTableModel());
        when(htmlReportExporter.export(any(), any(), any())).thenReturn("<html><body>test</body></html>");

        mockMvc.perform(get("/reports/participants")
                        .param("congressId", CONGRESS_ID.toString())
                        .param("format", "html")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void participants_with_invalid_format_returns_400() throws Exception {
        String token = buildToken(USER_ID, "CONGRESS_ADMIN");

        mockMvc.perform(get("/reports/participants")
                        .param("congressId", CONGRESS_ID.toString())
                        .param("format", "pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    @Test
    void attendance_by_activity_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/reports/attendance-by-activity")
                        .param("congressId", CONGRESS_ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void attendance_by_activity_with_congress_admin_token_returns_200() throws Exception {
        String token = buildToken(USER_ID, "CONGRESS_ADMIN");
        when(attendanceByActivityReportUseCase.execute(any(), any(), any(), any(), any(), any()))
                .thenReturn(AttendanceByActivityReportResponse.builder().items(List.of()).totalItems(0).build());

        mockMvc.perform(get("/reports/attendance-by-activity")
                        .param("congressId", CONGRESS_ID.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void workshop_reservations_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/reports/workshop-reservations")
                        .param("congressId", CONGRESS_ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void workshop_reservations_with_congress_admin_token_returns_200() throws Exception {
        String token = buildToken(USER_ID, "CONGRESS_ADMIN");
        when(workshopReservationsReportUseCase.execute(any(), any(), any()))
                .thenReturn(WorkshopReservationsReportResponse.builder().items(List.of()).totalItems(0).build());

        mockMvc.perform(get("/reports/workshop-reservations")
                        .param("congressId", CONGRESS_ID.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void congresses_by_institution_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/reports/congresses-by-institution"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void congresses_by_institution_with_system_admin_token_returns_200() throws Exception {
        String token = buildToken(USER_ID, "SYSTEM_ADMIN");
        when(congressesByInstitutionReportUseCase.execute(any(), any(), any()))
                .thenReturn(CongressesByInstitutionReportResponse.builder().items(List.of()).totalItems(0).build());

        mockMvc.perform(get("/reports/congresses-by-institution")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void attendance_by_activity_with_html_format_returns_html() throws Exception {
        String token = buildToken(USER_ID, "CONGRESS_ADMIN");
        OffsetDateTime start = OffsetDateTime.of(2026, 5, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 5, 1, 11, 0, 0, 0, ZoneOffset.UTC);
        AttendanceActivityItem item = AttendanceActivityItem.builder()
                .activityId(UUID.randomUUID())
                .activityName("Keynote")
                .roomName("Room A")
                .startTime(start)
                .endTime(end)
                .attendanceCount(42L)
                .build();
        when(attendanceByActivityReportUseCase.execute(any(), any(), any(), any(), any(), any()))
                .thenReturn(AttendanceByActivityReportResponse.builder()
                        .items(List.of(item)).totalItems(1).build());
        when(reportTableModelFactory.attendanceByActivity(any())).thenReturn(sampleTableModel());
        when(htmlReportExporter.export(any(), any(), any())).thenReturn("<html>attendance</html>");

        mockMvc.perform(get("/reports/attendance-by-activity")
                        .param("congressId", CONGRESS_ID.toString())
                        .param("format", "html")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void workshop_reservations_with_html_format_returns_html() throws Exception {
        String token = buildToken(USER_ID, "CONGRESS_ADMIN");
        WorkshopReservationItem item = WorkshopReservationItem.builder()
                .activityId(UUID.randomUUID())
                .activityName("Java Workshop")
                .workshopCapacity(30)
                .reservationCount(12)
                .availableSeats(18)
                .roster(List.of(RosterEntry.builder().personalId("A001").build()))
                .build();
        when(workshopReservationsReportUseCase.execute(any(), any(), any()))
                .thenReturn(WorkshopReservationsReportResponse.builder()
                        .items(List.of(item)).totalItems(1).build());
        when(reportTableModelFactory.workshopReservations(any())).thenReturn(sampleTableModel());
        when(htmlReportExporter.export(any(), any(), any())).thenReturn("<html>workshops</html>");

        mockMvc.perform(get("/reports/workshop-reservations")
                        .param("congressId", CONGRESS_ID.toString())
                        .param("format", "html")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void congresses_by_institution_with_html_format_returns_html() throws Exception {
        String token = buildToken(USER_ID, "SYSTEM_ADMIN");
        CongressByInstitutionItem item = CongressByInstitutionItem.builder()
                .institutionId(UUID.randomUUID())
                .institutionName("USAC")
                .congressId(UUID.randomUUID())
                .congressName("AydConf 2026")
                .startDate(java.time.LocalDate.of(2026, 6, 1))
                .endDate(java.time.LocalDate.of(2026, 6, 5))
                .location("Guatemala City")
                .price(new BigDecimal("100.00"))
                .build();
        when(congressesByInstitutionReportUseCase.execute(any(), any(), any()))
                .thenReturn(CongressesByInstitutionReportResponse.builder()
                        .items(List.of(item)).totalItems(1).build());
        when(reportTableModelFactory.congressesByInstitution(any())).thenReturn(sampleTableModel());
        when(htmlReportExporter.export(any(), any(), any())).thenReturn("<html>congresses</html>");

        mockMvc.perform(get("/reports/congresses-by-institution")
                        .param("format", "html")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void participants_with_data_items_exercises_row_mapping() throws Exception {
        String token = buildToken(USER_ID, "CONGRESS_ADMIN");
        ParticipantItem item = ParticipantItem.builder()
                .personalId("ABC123")
                .fullName("Ana Lopez")
                .organization("USAC")
                .email("ana@usac.edu")
                .phone("5550001")
                .participationTypes(List.of(ParticipationTypeEnum.ENROLLED, ParticipationTypeEnum.SPEAKER))
                .build();
        when(participantsReportUseCase.execute(any(), any(), any()))
                .thenReturn(ParticipantsReportResponse.builder()
                        .items(List.of(item)).totalItems(1).build());
        when(reportTableModelFactory.participants(any())).thenReturn(sampleTableModel());
        when(htmlReportExporter.export(any(), any(), any())).thenReturn("<html>participants</html>");

        mockMvc.perform(get("/reports/participants")
                        .param("congressId", CONGRESS_ID.toString())
                        .param("format", "html")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void participants_with_null_fields_in_item_does_not_throw() throws Exception {
        String token = buildToken(USER_ID, "CONGRESS_ADMIN");
        ParticipantItem item = ParticipantItem.builder()
                .participationTypes(null)
                .build();
        when(participantsReportUseCase.execute(any(), any(), any()))
                .thenReturn(ParticipantsReportResponse.builder()
                        .items(List.of(item)).totalItems(1).build());
        when(reportTableModelFactory.participants(any())).thenReturn(sampleTableModel());
        when(htmlReportExporter.export(any(), any(), any())).thenReturn("<html>ok</html>");

        mockMvc.perform(get("/reports/participants")
                        .param("congressId", CONGRESS_ID.toString())
                        .param("format", "html")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void attendance_item_with_null_times_does_not_throw() throws Exception {
        String token = buildToken(USER_ID, "CONGRESS_ADMIN");
        AttendanceActivityItem item = AttendanceActivityItem.builder()
                .activityId(UUID.randomUUID())
                .activityName(null)
                .roomName(null)
                .startTime(null)
                .endTime(null)
                .attendanceCount(0L)
                .build();
        when(attendanceByActivityReportUseCase.execute(any(), any(), any(), any(), any(), any()))
                .thenReturn(AttendanceByActivityReportResponse.builder()
                        .items(List.of(item)).totalItems(1).build());
        when(reportTableModelFactory.attendanceByActivity(any())).thenReturn(sampleTableModel());
        when(htmlReportExporter.export(any(), any(), any())).thenReturn("<html>ok</html>");

        mockMvc.perform(get("/reports/attendance-by-activity")
                        .param("congressId", CONGRESS_ID.toString())
                        .param("format", "html")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void congress_item_with_null_fields_does_not_throw() throws Exception {
        String token = buildToken(USER_ID, "SYSTEM_ADMIN");
        CongressByInstitutionItem item = CongressByInstitutionItem.builder()
                .institutionId(UUID.randomUUID())
                .institutionName(null)
                .congressId(UUID.randomUUID())
                .congressName(null)
                .startDate(null)
                .endDate(null)
                .location(null)
                .price(null)
                .build();
        when(congressesByInstitutionReportUseCase.execute(any(), any(), any()))
                .thenReturn(CongressesByInstitutionReportResponse.builder()
                        .items(List.of(item)).totalItems(1).build());
        when(reportTableModelFactory.congressesByInstitution(any())).thenReturn(sampleTableModel());
        when(htmlReportExporter.export(any(), any(), any())).thenReturn("<html>ok</html>");

        mockMvc.perform(get("/reports/congresses-by-institution")
                        .param("format", "html")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void missing_bearer_token_returns_401() throws Exception {
        mockMvc.perform(get("/reports/participants")
                        .param("congressId", CONGRESS_ID.toString())
                        .header("Authorization", "NotBearer token"))
                .andExpect(status().isUnauthorized());
    }

    private String buildToken(UUID userId, String... roles) {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("email", "test@example.com")
                .claim("roles", List.of(roles))
                .claim("tokenType", "ACCESS")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();
    }

    private ReportTableModel sampleTableModel() {
        return ReportTableModel.builder()
                .title("Reporte")
                .headers(List.of("Columna"))
                .rows(List.of(List.of("Valor")))
                .build();
    }
}
