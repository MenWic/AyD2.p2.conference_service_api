package ayd2.p2b.conference_service_api.feature.congress.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.openapi.OpenApiExamples;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.feature.congress.application.exception.CongressExceptions;
import ayd2.p2b.conference_service_api.feature.congress.application.create.CreateCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.delete.DeleteCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.get.GetCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.list.ListCongressesUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.update.UpdateCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressRequesterContext;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressSearchCriteria;
import ayd2.p2b.conference_service_api.feature.congress.dto.request.CreateCongressRequest;
import ayd2.p2b.conference_service_api.feature.congress.dto.request.UpdateCongressRequest;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
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
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/congresses")
@Tag(name = "Congresses", description = "Congress management endpoints")
public class CongressController {

    private static final String ROLE_CONGRESS_ADMIN = "ROLE_CONGRESS_ADMIN";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final CreateCongressUseCase createCongressUseCase;
    private final ListCongressesUseCase listCongressesUseCase;
    private final GetCongressUseCase getCongressUseCase;
    private final UpdateCongressUseCase updateCongressUseCase;
    private final DeleteCongressUseCase deleteCongressUseCase;

    public CongressController(
            CreateCongressUseCase createCongressUseCase,
            ListCongressesUseCase listCongressesUseCase,
            GetCongressUseCase getCongressUseCase,
            UpdateCongressUseCase updateCongressUseCase,
            DeleteCongressUseCase deleteCongressUseCase
    ) {
        this.createCongressUseCase = createCongressUseCase;
        this.listCongressesUseCase = listCongressesUseCase;
        this.getCongressUseCase = getCongressUseCase;
        this.updateCongressUseCase = updateCongressUseCase;
        this.deleteCongressUseCase = deleteCongressUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Create congress",
            description = "Creates a congress for an institution linked to the requester CongressAdmin. Enforces price >= 35.00 and valid date range.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Congress created"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.TOKEN_INVALID_ERROR))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.FORBIDDEN_ERROR))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Institution not found", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.NOT_FOUND_ERROR))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Domain invariant violated", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.DOMAIN_INVARIANT_ERROR)))
            }
    )
    public ResponseEntity<ApiResponse<CongressResponse>> createCongress(
            @Valid @RequestBody CreateCongressRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        CongressRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        CongressResponse response = createCongressUseCase.execute(request, requester);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, URI.create("/congresses/" + response.getId()).toString())
                .body(ApiResponse.of(response, "Congress created"));
    }

    @GetMapping
    @Operation(
            summary = "List congresses",
            description = "Public paginated listing of congresses with optional filters by institution, start-date range, and search text.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Congresses listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.VALIDATION_ERROR)))
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<CongressResponse>>> listCongresses(
            @Parameter(description = "Institution filter (UUID)", example = "d2719de1-0409-4d2e-bf9b-a06f0ea74df7")
            @RequestParam(required = false) UUID institutionId,
            @Parameter(description = "Lower bound for congress start date (inclusive, yyyy-MM-dd)", example = "2026-09-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
            @Parameter(description = "Upper bound for congress start date (inclusive, yyyy-MM-dd)", example = "2026-09-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,
            @Parameter(description = "Case-insensitive search by congress name", example = "ingenieria")
            @RequestParam(required = false) String search,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        if (startDateFrom != null && startDateTo != null && startDateFrom.isAfter(startDateTo)) {
            throw CongressExceptions.validationFailed("startDateFrom must be <= startDateTo");
        }

        CongressSearchCriteria criteria = CongressSearchCriteria.builder()
                .institutionId(institutionId)
                .startDateFrom(startDateFrom)
                .startDateTo(startDateTo)
                .search(search == null ? null : search.trim())
                .build();

        Pageable pageable = normalizePageable(page, size);
        PageResponse<CongressResponse> response = listCongressesUseCase.execute(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get congress by id",
            description = "Returns a congress detail by UUID. Public endpoint.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Congress found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.NOT_FOUND_ERROR)))
            }
    )
    public ResponseEntity<ApiResponse<CongressResponse>> getCongress(@PathVariable UUID id) {
        CongressResponse response = getCongressUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update congress",
            description = "Updates a congress. Requires CONGRESS_ADMIN owner/scoped access to the congress institution.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Congress updated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Domain invariant violated", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<CongressResponse>> updateCongress(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCongressRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        CongressRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        CongressResponse response = updateCongressUseCase.execute(id, request, requester);
        return ResponseEntity.ok(ApiResponse.of(response, "Congress updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete congress",
            description = "Deletes a congress when no blocking dependencies exist. Requires CONGRESS_ADMIN owner/scoped access.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Congress deleted"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<CongressResponse>> deleteCongress(
            @PathVariable UUID id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        CongressRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        CongressResponse response = deleteCongressUseCase.execute(id, requester);
        return ResponseEntity.ok(ApiResponse.of(response, "Congress deleted"));
    }

    private CongressRequesterContext requireCongressAdminRequester(Authentication authentication, String authorization) {
        if (authentication == null) {
            throw CongressExceptions.forbidden("Congress admin role is required");
        }
        boolean congressAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_CONGRESS_ADMIN::equals);
        if (!congressAdmin) {
            throw CongressExceptions.forbidden("Congress admin role is required");
        }
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw CongressExceptions.forbidden("Authenticated user context is required");
        }
        if (user.getUserId() == null) {
            throw CongressExceptions.forbidden("Authenticated user context is required");
        }
        String token = extractBearerToken(authorization);

        return CongressRequesterContext.builder()
                .userId(user.getUserId())
                .roles(user.getRoles() == null ? Set.of() : user.getRoles())
                .accessToken(token)
                .build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw CongressExceptions.forbidden("Bearer token is required");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw CongressExceptions.forbidden("Bearer token is required");
        }
        return token;
    }

    private Pageable normalizePageable(int page, int size) {
        int normalizedPage = Math.max(DEFAULT_PAGE, page);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.ASC, "startDate"));
    }
}
