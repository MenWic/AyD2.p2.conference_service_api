package ayd2.p2b.conference_service_api.feature.report.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.report.application.attendance_summary.AttendanceByActivityReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.congresses_by_institution.CongressesByInstitutionReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.exception.ReportExceptions;
import ayd2.p2b.conference_service_api.feature.report.application.participants.ParticipantsReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.WorkshopReservationsReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportFormat;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceActivityItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceByActivityReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressByInstitutionItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressesByInstitutionReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipantItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipantsReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipationTypeEnum;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationsReportResponse;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.export.HtmlReportExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Reports", description = "Aggregated reports for CongressAdmin and SystemAdmin")
@RequiredArgsConstructor
public class ReportController {

    private final ParticipantsReportUseCase participantsReportUseCase;
    private final AttendanceByActivityReportUseCase attendanceByActivityReportUseCase;
    private final WorkshopReservationsReportUseCase workshopReservationsReportUseCase;
    private final CongressesByInstitutionReportUseCase congressesByInstitutionReportUseCase;
    private final HtmlReportExporter htmlReportExporter;

    @GetMapping("/reports/participants")
    @Operation(summary = "Participant listing report", security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report generated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress not found")
            })
    public ResponseEntity<?> participants(
            @RequestParam UUID congressId,
            @RequestParam(required = false) ParticipationTypeEnum type,
            @RequestParam(required = false) String format,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication) {

        ReportFormat fmt = ReportFormat.parse(format);
        ReportRequesterContext requester = buildRequesterContext(authentication, authorization);
        ParticipantsReportResponse response = participantsReportUseCase.execute(congressId, type, requester);

        if (fmt == ReportFormat.HTML) {
            List<String> headers = List.of("Personal ID", "Nombre", "Organizacion", "Email", "Telefono", "Tipos");
            List<List<String>> rows = response.getItems().stream().map(this::participantToRow).toList();
            String html = htmlReportExporter.export("Reporte de Participantes", headers, rows);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/reports/attendance-by-activity")
    @Operation(summary = "Attendance by activity report", security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report generated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress not found")
            })
    public ResponseEntity<?> attendanceByActivity(
            @RequestParam UUID congressId,
            @RequestParam(required = false) UUID activityId,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(required = false) String format,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication) {

        ReportFormat fmt = ReportFormat.parse(format);
        ReportRequesterContext requester = buildRequesterContext(authentication, authorization);
        AttendanceByActivityReportResponse response = attendanceByActivityReportUseCase.execute(
                congressId, activityId, roomId, dateFrom, dateTo, requester);

        if (fmt == ReportFormat.HTML) {
            List<String> headers = List.of("Actividad", "Sala", "Inicio", "Fin", "Asistentes");
            List<List<String>> rows = response.getItems().stream().map(this::attendanceItemToRow).toList();
            String html = htmlReportExporter.export("Asistencia por Actividad", headers, rows);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/reports/workshop-reservations")
    @Operation(summary = "Workshop reservations report", security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report generated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress not found")
            })
    public ResponseEntity<?> workshopReservations(
            @RequestParam UUID congressId,
            @RequestParam(required = false) UUID activityId,
            @RequestParam(required = false) String format,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication) {

        ReportFormat fmt = ReportFormat.parse(format);
        ReportRequesterContext requester = buildRequesterContext(authentication, authorization);
        WorkshopReservationsReportResponse response = workshopReservationsReportUseCase.execute(
                congressId, activityId, requester);

        if (fmt == ReportFormat.HTML) {
            List<String> headers = List.of("Taller", "Capacidad", "Reservas", "Disponibles");
            List<List<String>> rows = response.getItems().stream().map(this::workshopItemToRow).toList();
            String html = htmlReportExporter.export("Reservas de Talleres", headers, rows);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/reports/congresses-by-institution")
    @Operation(summary = "Congresses by institution report (SystemAdmin)", security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report generated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
            })
    public ResponseEntity<?> congressesByInstitution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String format,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication) {

        ReportFormat fmt = ReportFormat.parse(format);
        ReportRequesterContext requester = buildRequesterContext(authentication, authorization);
        CongressesByInstitutionReportResponse response = congressesByInstitutionReportUseCase.execute(
                dateFrom, dateTo, requester);

        if (fmt == ReportFormat.HTML) {
            List<String> headers = List.of("Institucion", "Congreso", "Inicio", "Fin", "Lugar", "Precio");
            List<List<String>> rows = response.getItems().stream().map(this::congressItemToRow).toList();
            String html = htmlReportExporter.export("Congresos por Institucion", headers, rows);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    private ReportRequesterContext buildRequesterContext(Authentication authentication, String authorization) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw ReportExceptions.forbidden("Authentication is required");
        }
        String token = extractBearerToken(authorization);
        Set<Role> roles = user.getRoles() == null ? Set.of() : user.getRoles();
        return ReportRequesterContext.builder()
                .userId(user.getUserId())
                .roles(roles)
                .accessToken(token)
                .build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw ReportExceptions.forbidden("Bearer token is required");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw ReportExceptions.forbidden("Bearer token is required");
        }
        return token;
    }

    private List<String> participantToRow(ParticipantItem item) {
        String types = item.getParticipationTypes() == null ? ""
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

    private List<String> workshopItemToRow(WorkshopReservationItem item) {
        return List.of(
                orEmpty(item.getActivityName()),
                String.valueOf(item.getWorkshopCapacity()),
                String.valueOf(item.getReservationCount()),
                String.valueOf(item.getAvailableSeats())
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
}
