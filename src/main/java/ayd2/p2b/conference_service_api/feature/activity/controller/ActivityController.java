package ayd2.p2b.conference_service_api.feature.activity.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.feature.activity.application.create.CreateActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.delete.DeleteActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.exception.ActivityExceptions;
import ayd2.p2b.conference_service_api.feature.activity.application.get.GetActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.list.ListActivitiesUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.update.UpdateActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityRequesterContext;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivitySearchCriteria;
import ayd2.p2b.conference_service_api.feature.activity.dto.request.CreateActivityRequest;
import ayd2.p2b.conference_service_api.feature.activity.dto.request.UpdateActivityRequest;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Activities", description = "Activity management endpoints")
public class ActivityController {

    private static final String ROLE_CONGRESS_ADMIN = "ROLE_CONGRESS_ADMIN";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final CreateActivityUseCase createActivityUseCase;
    private final ListActivitiesUseCase listActivitiesUseCase;
    private final GetActivityUseCase getActivityUseCase;
    private final UpdateActivityUseCase updateActivityUseCase;
    private final DeleteActivityUseCase deleteActivityUseCase;

    public ActivityController(
            CreateActivityUseCase createActivityUseCase,
            ListActivitiesUseCase listActivitiesUseCase,
            GetActivityUseCase getActivityUseCase,
            UpdateActivityUseCase updateActivityUseCase,
            DeleteActivityUseCase deleteActivityUseCase
    ) {
        this.createActivityUseCase = createActivityUseCase;
        this.listActivitiesUseCase = listActivitiesUseCase;
        this.getActivityUseCase = getActivityUseCase;
        this.updateActivityUseCase = updateActivityUseCase;
        this.deleteActivityUseCase = deleteActivityUseCase;
    }

    @PostMapping("/congresses/{id}/activities")
    @Operation(
            summary = "Create activity in congress",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Activity created"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress or room not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Domain invariant violated", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "IAM unavailable", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<ActivityResponse>> createActivity(
            @PathVariable("id") UUID congressId,
            @Valid @RequestBody CreateActivityRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ActivityRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        ActivityResponse response = createActivityUseCase.execute(congressId, request, requester);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, URI.create("/activities/" + response.getId()).toString())
                .body(ApiResponse.of(response, "Activity created"));
    }

    @GetMapping("/congresses/{id}/activities")
    @Operation(
            summary = "List activities by congress",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Activities listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress not found", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<ActivityResponse>>> listActivitiesByCongress(
            @PathVariable("id") UUID congressId,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw ActivityExceptions.validationFailed("dateFrom must be <= dateTo");
        }

        ActivitySearchCriteria criteria = ActivitySearchCriteria.builder()
                .roomId(roomId)
                .type(type)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();

        Pageable pageable = normalizePageable(page, size);
        PageResponse<ActivityResponse> response = listActivitiesUseCase.execute(congressId, criteria, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/activities/{id}")
    @Operation(
            summary = "Get activity by id",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Activity found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<ActivityResponse>> getActivity(@PathVariable("id") UUID activityId) {
        ActivityResponse response = getActivityUseCase.execute(activityId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PutMapping("/activities/{id}")
    @Operation(
            summary = "Update activity",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Activity updated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Domain invariant violated", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "IAM unavailable", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<ActivityResponse>> updateActivity(
            @PathVariable("id") UUID activityId,
            @Valid @RequestBody UpdateActivityRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ActivityRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        ActivityResponse response = updateActivityUseCase.execute(activityId, request, requester);
        return ResponseEntity.ok(ApiResponse.of(response, "Activity updated"));
    }

    @DeleteMapping("/activities/{id}")
    @Operation(
            summary = "Delete activity",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Activity deleted"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<ActivityResponse>> deleteActivity(
            @PathVariable("id") UUID activityId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ActivityRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        ActivityResponse response = deleteActivityUseCase.execute(activityId, requester);
        return ResponseEntity.ok(ApiResponse.of(response, "Activity deleted"));
    }

    private ActivityRequesterContext requireCongressAdminRequester(Authentication authentication, String authorization) {
        if (authentication == null) {
            throw ActivityExceptions.forbidden("Congress admin role is required");
        }
        boolean congressAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_CONGRESS_ADMIN::equals);
        if (!congressAdmin) {
            throw ActivityExceptions.forbidden("Congress admin role is required");
        }
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw ActivityExceptions.forbidden("Authenticated user context is required");
        }
        if (user.getUserId() == null) {
            throw ActivityExceptions.forbidden("Authenticated user context is required");
        }

        return ActivityRequesterContext.builder()
                .userId(user.getUserId())
                .roles(user.getRoles() == null ? Set.of() : user.getRoles())
                .accessToken(extractBearerToken(authorization))
                .build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw ActivityExceptions.forbidden("Bearer token is required");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw ActivityExceptions.forbidden("Bearer token is required");
        }
        return token;
    }

    private Pageable normalizePageable(int page, int size) {
        int normalizedPage = Math.max(DEFAULT_PAGE, page);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.ASC, "startTime"));
    }
}
