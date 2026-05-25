package ayd2.p2b.conference_service_api.feature.attendance.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.openapi.OpenApiExamples;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.feature.attendance.application.exception.AttendanceExceptions;
import ayd2.p2b.conference_service_api.feature.attendance.application.list.ListAttendanceUseCase;
import ayd2.p2b.conference_service_api.feature.attendance.application.register.RegisterAttendanceUseCase;
import ayd2.p2b.conference_service_api.feature.attendance.application.user.GetUserAttendanceUseCase;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceRequesterContext;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceSearchCriteria;
import ayd2.p2b.conference_service_api.feature.attendance.dto.request.RegisterAttendanceRequest;
import ayd2.p2b.conference_service_api.feature.attendance.dto.response.AttendanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Attendance", description = "Attendance registration and listing endpoints")
public class AttendanceController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final RegisterAttendanceUseCase registerAttendanceUseCase;
    private final ListAttendanceUseCase listAttendanceUseCase;
    private final GetUserAttendanceUseCase getUserAttendanceUseCase;

    public AttendanceController(
            RegisterAttendanceUseCase registerAttendanceUseCase,
            ListAttendanceUseCase listAttendanceUseCase,
            GetUserAttendanceUseCase getUserAttendanceUseCase
    ) {
        this.registerAttendanceUseCase = registerAttendanceUseCase;
        this.listAttendanceUseCase = listAttendanceUseCase;
        this.getUserAttendanceUseCase = getUserAttendanceUseCase;
    }

    @PostMapping("/attendance/register")
    @Operation(
            summary = "Register attendance for an activity participant",
            description = "Registers immutable attendance using activityId + personalId. personalId is resolved through IAM. TALLER requires reservation; PONENCIA does not.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attendance registered"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.VALIDATION_ERROR))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Activity or participant not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Attendance conflict", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Domain invariant violated", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "IAM unavailable", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.IAM_UNAVAILABLE_ERROR)))
            }
    )
    public ResponseEntity<ApiResponse<AttendanceResponse>> registerAttendance(
            @Valid @RequestBody RegisterAttendanceRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        AttendanceRequesterContext requester = buildRequesterContext(authentication, authorization);
        AttendanceResponse response = registerAttendanceUseCase.execute(request, requester);
        return ResponseEntity.ok(ApiResponse.of(response, "Attendance registered"));
    }

    @GetMapping("/attendance")
    @Operation(
            summary = "List attendance records with filters",
            description = "Returns immutable attendance records scoped to CONGRESS_ADMIN owner/scoped access. Supports filters by activity, room, date range, and personalId.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attendances listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "IAM unavailable", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<AttendanceResponse>>> listAttendance(
            @Parameter(description = "Activity filter (UUID)", example = "3a6d8f8e-a077-4a66-a7d3-c8dd90666fc2")
            @RequestParam(required = false) UUID activityId,
            @Parameter(description = "Room filter (UUID)", example = "f39f1f7f-e2d2-4f2e-8cb8-95630f1a9f8e")
            @RequestParam(required = false) UUID roomId,
            @Parameter(description = "Date range start (yyyy-MM-dd, inclusive)", example = "2026-10-10")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "Date range end (yyyy-MM-dd, inclusive)", example = "2026-10-12")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Exact personalId filter (trimmed, case-insensitive)", example = "A1234567")
            @RequestParam(required = false) String personalId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw AttendanceExceptions.validationFailed("dateFrom must be <= dateTo");
        }

        AttendanceRequesterContext requester = buildRequesterContext(authentication, authorization);
        AttendanceSearchCriteria criteria = AttendanceSearchCriteria.builder()
                .activityId(activityId)
                .roomId(roomId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .personalId(personalId == null ? null : personalId.trim())
                .build();
        Pageable pageable = normalizePageable(page, size);
        PageResponse<AttendanceResponse> response = listAttendanceUseCase.execute(criteria, pageable, requester);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/users/{userId}/attendance")
    @Operation(
            summary = "List attendance records for a participant (self only)",
            description = "Returns all attendance records owned by the authenticated user. The path userId must match the authenticated userId.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attendance records returned"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> listUserAttendance(
            @org.springframework.web.bind.annotation.PathVariable UUID userId,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw AttendanceExceptions.forbidden("Authentication required");
        }
        if (!userId.equals(user.getUserId())) {
            throw AttendanceExceptions.forbidden("Cannot access another user's attendance");
        }
        List<AttendanceResponse> items = getUserAttendanceUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.of(items));
    }

    private AttendanceRequesterContext buildRequesterContext(Authentication authentication, String authorization) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw AttendanceExceptions.forbidden("Authenticated congress admin context is required");
        }
        if (user.getUserId() == null) {
            throw AttendanceExceptions.forbidden("Authenticated congress admin context is required");
        }
        return AttendanceRequesterContext.builder()
                .userId(user.getUserId())
                .roles(user.getRoles() == null ? Set.of() : user.getRoles())
                .accessToken(extractBearerToken(authorization))
                .build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw AttendanceExceptions.forbidden("Bearer token is required");
        }
        String token = authorization.substring(7).trim();
        if (token.isBlank()) {
            throw AttendanceExceptions.forbidden("Bearer token is required");
        }
        return token;
    }

    private Pageable normalizePageable(int page, int size) {
        int normalizedPage = Math.max(DEFAULT_PAGE, page);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "registeredAt"));
    }
}
