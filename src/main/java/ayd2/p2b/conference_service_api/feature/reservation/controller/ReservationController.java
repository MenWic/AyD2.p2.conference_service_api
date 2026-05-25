package ayd2.p2b.conference_service_api.feature.reservation.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.openapi.OpenApiExamples;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.feature.reservation.application.cancel.CancelReservationUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.application.exception.ReservationExceptions;
import ayd2.p2b.conference_service_api.feature.reservation.application.list_by_activity.ListActivityReservationsUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.application.list_by_user.ListUserReservationsUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.application.reserve.ReserveActivityUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationRequesterContext;
import ayd2.p2b.conference_service_api.feature.reservation.dto.response.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Reservations", description = "Workshop reservation endpoints")
public class ReservationController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ReserveActivityUseCase reserveActivityUseCase;
    private final ListActivityReservationsUseCase listActivityReservationsUseCase;
    private final ListUserReservationsUseCase listUserReservationsUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    public ReservationController(
            ReserveActivityUseCase reserveActivityUseCase,
            ListActivityReservationsUseCase listActivityReservationsUseCase,
            ListUserReservationsUseCase listUserReservationsUseCase,
            CancelReservationUseCase cancelReservationUseCase
    ) {
        this.reserveActivityUseCase = reserveActivityUseCase;
        this.listActivityReservationsUseCase = listActivityReservationsUseCase;
        this.listUserReservationsUseCase = listUserReservationsUseCase;
        this.cancelReservationUseCase = cancelReservationUseCase;
    }

    @PostMapping("/activities/{id}/reservations")
    @Operation(
            summary = "Reserve a workshop seat",
            description = "Creates a reservation for a TALLER activity. Requires participant enrollment and available workshop capacity.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Reservation created"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Activity not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Reservation conflict", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.CONFLICT_ERROR))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Domain invariant violated", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.DOMAIN_INVARIANT_ERROR)))
            }
    )
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveActivity(
            @PathVariable("id") UUID activityId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ReservationRequesterContext requester = buildRequesterContext(authentication, authorization);
        ReservationResponse response = reserveActivityUseCase.execute(activityId, requester);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, URI.create("/reservations/" + response.getId()).toString())
                .body(ApiResponse.of(response, "Reservation created"));
    }

    @GetMapping("/activities/{id}/reservations")
    @Operation(
            summary = "List reservations for an activity",
            description = "Returns paginated reservations for an activity. Requires CONGRESS_ADMIN owner/scoped access.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reservations listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Activity not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "IAM unavailable", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> listActivityReservations(
            @PathVariable("id") UUID activityId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ReservationRequesterContext requester = buildRequesterContext(authentication, authorization);
        Pageable pageable = normalizePageable(page, size, "reservedAt");
        PageResponse<ReservationResponse> response =
                listActivityReservationsUseCase.execute(activityId, pageable, requester);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/users/{id}/reservations")
    @Operation(
            summary = "List reservations for a user (self only)",
            description = "Returns paginated reservations for a user. Self-only endpoint for PARTICIPANT.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reservations listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> listUserReservations(
            @PathVariable("id") UUID requestedUserId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ReservationRequesterContext requester = buildRequesterContext(authentication, authorization);
        Pageable pageable = normalizePageable(page, size, "reservedAt");
        PageResponse<ReservationResponse> response =
                listUserReservationsUseCase.execute(requestedUserId, pageable, requester);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/reservations/{id}")
    @Operation(
            summary = "Cancel reservation",
            description = "Cancels reservation for its owner. Cancellation is blocked with conflict when attendance already exists.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reservation cancelled"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable("id") UUID reservationId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ReservationRequesterContext requester = buildRequesterContext(authentication, authorization);
        cancelReservationUseCase.execute(reservationId, requester);
        return ResponseEntity.ok(ApiResponse.of(null, "Reservation cancelled"));
    }

    private ReservationRequesterContext buildRequesterContext(Authentication authentication, String authorization) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw ReservationExceptions.forbidden("Authenticated user context is required");
        }
        if (user.getUserId() == null) {
            throw ReservationExceptions.forbidden("Authenticated user context is required");
        }
        return ReservationRequesterContext.builder()
                .userId(user.getUserId())
                .roles(user.getRoles() == null ? Set.of() : user.getRoles())
                .accessToken(extractBearerToken(authorization))
                .build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw ReservationExceptions.forbidden("Bearer token is required");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw ReservationExceptions.forbidden("Bearer token is required");
        }
        return token;
    }

    private Pageable normalizePageable(int page, int size, String sortField) {
        int normalizedPage = Math.max(DEFAULT_PAGE, page);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, sortField));
    }
}
