package ayd2.p2b.conference_service_api.feature.attendance.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.feature.attendance.application.exception.AttendanceExceptions;
import ayd2.p2b.conference_service_api.feature.attendance.application.list.ListAttendanceUseCase;
import ayd2.p2b.conference_service_api.feature.attendance.application.register.RegisterAttendanceUseCase;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceRequesterContext;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceSearchCriteria;
import ayd2.p2b.conference_service_api.feature.attendance.dto.request.RegisterAttendanceRequest;
import ayd2.p2b.conference_service_api.feature.attendance.dto.response.AttendanceResponse;
import io.swagger.v3.oas.annotations.Operation;
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

    public AttendanceController(
            RegisterAttendanceUseCase registerAttendanceUseCase,
            ListAttendanceUseCase listAttendanceUseCase
    ) {
        this.registerAttendanceUseCase = registerAttendanceUseCase;
        this.listAttendanceUseCase = listAttendanceUseCase;
    }

    @PostMapping("/attendance/register")
    @Operation(
            summary = "Register attendance for an activity participant",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attendance registered"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Activity or participant not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Attendance conflict", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Domain invariant violated", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "IAM unavailable", content = @Content)
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
            @RequestParam(required = false) UUID activityId,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String personalId,
            @RequestParam(defaultValue = "0") int page,
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
