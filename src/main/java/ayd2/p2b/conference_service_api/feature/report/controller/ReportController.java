package ayd2.p2b.conference_service_api.feature.report.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.core.openapi.OpenApiExamples;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.report.application.attendance_summary.AttendanceByActivityReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.congresses_by_institution.CongressesByInstitutionReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.earnings.EarningsReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.earnings_by_congress.EarningsByCongressReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.exception.ReportExceptions;
import ayd2.p2b.conference_service_api.feature.report.application.participants.ParticipantsReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.WorkshopReservationsReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportFormat;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceByActivityReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressesByInstitutionReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsByCongressReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipantsReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipationTypeEnum;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationsReportResponse;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.export.HtmlReportExporter;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.export.ReportTableModel;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.export.ReportTableModelFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.Set;
import java.util.UUID;

@RestController
@Tag(name = "Reports", description = "Aggregated reports for CongressAdmin and SystemAdmin")
@RequiredArgsConstructor
public class ReportController {

    private final ParticipantsReportUseCase participantsReportUseCase;
    private final AttendanceByActivityReportUseCase attendanceByActivityReportUseCase;
    private final WorkshopReservationsReportUseCase workshopReservationsReportUseCase;
    private final CongressesByInstitutionReportUseCase congressesByInstitutionReportUseCase;
    private final EarningsByCongressReportUseCase earningsByCongressReportUseCase;
    private final EarningsReportUseCase earningsReportUseCase;
    private final HtmlReportExporter htmlReportExporter;
    private final ReportTableModelFactory reportTableModelFactory;

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
            ReportTableModel table = reportTableModelFactory.participants(response);
            String html = htmlReportExporter.export(table.getTitle(), table.getHeaders(), table.getRows());
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
            ReportTableModel table = reportTableModelFactory.attendanceByActivity(response);
            String html = htmlReportExporter.export(table.getTitle(), table.getHeaders(), table.getRows());
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/reports/workshop-reservations")
    @Operation(
            summary = "Workshop reservations report",
            description = "Returns TALLER reservation aggregates and roster entries with personalId, fullName, email and participationType. "
                    + "When format is omitted or format=json, response is ApiResponse<WorkshopReservationsReportResponse>. "
                    + "When format=html, response is text/html (not wrapped in ApiResponse) and includes workshop roster data in the exported table.",
            security = @SecurityRequirement(name = "bearerAuth"),
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
            ReportTableModel table = reportTableModelFactory.workshopReservations(response);
            String html = htmlReportExporter.export(table.getTitle(), table.getHeaders(), table.getRows());
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/reports/earnings-by-congress")
    @Operation(
            summary = "Earnings by congress report (CongressAdmin)",
            description = "CONGRESS_ADMIN only. congressId is required and must be linked to the requester scope. "
                    + "Optional dateFrom/dateTo filters are inclusive. "
                    + "When format is omitted or format=json, response is ApiResponse<EarningsByCongressReportResponse>. "
                    + "When format=html, response is text/html without ApiResponse.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Report generated",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            examples = @ExampleObject(value = OpenApiExamples.EARNINGS_BY_CONGRESS_REPORT_SUCCESS)
                                    ),
                                    @Content(mediaType = "text/html")
                            }
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Validation failed",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.VALIDATION_ERROR))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.TOKEN_INVALID_ERROR))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = "Forbidden",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.FORBIDDEN_ERROR))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Congress not found",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.NOT_FOUND_ERROR))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = "Wallet integration unavailable",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.WALLET_UNAVAILABLE_ERROR))
                    )
            }
    )
    public ResponseEntity<?> earningsByCongress(
            @Parameter(description = "Congress identifier in requester scope", required = true,
                    example = "7d899e63-481d-4df8-87f1-7a8d8f437b68")
            @RequestParam(required = false) UUID congressId,
            @Parameter(description = "Inclusive start date filter", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "Inclusive end date filter", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(
                    description = "Response format. Omit or use json for ApiResponse JSON; use html for text/html export.",
                    schema = @Schema(allowableValues = {"json", "html"}),
                    example = "json")
            @RequestParam(required = false) String format,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ReportFormat fmt = ReportFormat.parse(format);
        ReportRequesterContext requester = buildRequesterContext(authentication, authorization);
        EarningsByCongressReportResponse response = earningsByCongressReportUseCase.execute(
                congressId, dateFrom, dateTo, requester);

        if (fmt == ReportFormat.HTML) {
            ReportTableModel table = reportTableModelFactory.earningsByCongress(response);
            String html = htmlReportExporter.export(table.getTitle(), table.getHeaders(), table.getRows());
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/reports/earnings")
    @Operation(
            summary = "Platform earnings report (SystemAdmin)",
            description = "SYSTEM_ADMIN only. Optional institutionId/dateFrom/dateTo filters are forwarded to Wallet. "
                    + "When format is omitted or format=json, response is ApiResponse<EarningsReportResponse>. "
                    + "When format=html, response is text/html without ApiResponse.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Report generated",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            examples = @ExampleObject(value = OpenApiExamples.EARNINGS_REPORT_SUCCESS)
                                    ),
                                    @Content(mediaType = "text/html")
                            }
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Validation failed",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.VALIDATION_ERROR))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.TOKEN_INVALID_ERROR))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = "Forbidden",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.FORBIDDEN_ERROR))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = "Wallet integration unavailable",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.WALLET_UNAVAILABLE_ERROR))
                    )
            }
    )
    public ResponseEntity<?> earnings(
            @Parameter(description = "Optional institution filter",
                    example = "d2719de1-0409-4d2e-bf9b-a06f0ea74df7")
            @RequestParam(required = false) UUID institutionId,
            @Parameter(description = "Inclusive start date filter", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "Inclusive end date filter", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(
                    description = "Response format. Omit or use json for ApiResponse JSON; use html for text/html export.",
                    schema = @Schema(allowableValues = {"json", "html"}),
                    example = "json")
            @RequestParam(required = false) String format,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ReportFormat fmt = ReportFormat.parse(format);
        ReportRequesterContext requester = buildRequesterContext(authentication, authorization);
        EarningsReportResponse response = earningsReportUseCase.execute(institutionId, dateFrom, dateTo, requester);

        if (fmt == ReportFormat.HTML) {
            ReportTableModel table = reportTableModelFactory.earnings(response);
            String html = htmlReportExporter.export(table.getTitle(), table.getHeaders(), table.getRows());
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/reports/congresses-by-institution")
    @Operation(
            summary = "Congresses by institution report (SystemAdmin)",
            description = "SYSTEM_ADMIN only. Lists congresses grouped by institution. Optional dateFrom/dateTo "
                    + "filters are inclusive and applied to congress.startDate. "
                    + "When format is omitted or format=json, response is ApiResponse<CongressesByInstitutionReportResponse>. "
                    + "When format=html, response is text/html without ApiResponse.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Report generated",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            examples = @ExampleObject(value = OpenApiExamples.CONGRESSES_BY_INSTITUTION_REPORT_SUCCESS)
                                    ),
                                    @Content(mediaType = "text/html")
                            }
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Validation failed (invalid format)",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.VALIDATION_ERROR))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.TOKEN_INVALID_ERROR))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = "Forbidden",
                            content = @Content(mediaType = "application/problem+json",
                                    examples = @ExampleObject(value = OpenApiExamples.FORBIDDEN_ERROR))
                    )
            })
    public ResponseEntity<?> congressesByInstitution(
            @Parameter(description = "Inclusive start date filter applied to congress.startDate",
                    example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "Inclusive end date filter applied to congress.startDate",
                    example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(
                    description = "Response format. Omit or use json for ApiResponse JSON; use html for text/html export.",
                    schema = @Schema(allowableValues = {"json", "html"}),
                    example = "json")
            @RequestParam(required = false) String format,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication) {

        ReportFormat fmt = ReportFormat.parse(format);
        ReportRequesterContext requester = buildRequesterContext(authentication, authorization);
        CongressesByInstitutionReportResponse response = congressesByInstitutionReportUseCase.execute(
                dateFrom, dateTo, requester);

        if (fmt == ReportFormat.HTML) {
            ReportTableModel table = reportTableModelFactory.congressesByInstitution(response);
            String html = htmlReportExporter.export(table.getTitle(), table.getHeaders(), table.getRows());
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
}
