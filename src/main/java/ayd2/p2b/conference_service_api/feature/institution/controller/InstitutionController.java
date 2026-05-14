package ayd2.p2b.conference_service_api.feature.institution.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.feature.institution.application.create.CreateInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.delete.DeleteInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.get.GetInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.list.ListInstitutionsUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.update.UpdateInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.domain.exception.InstitutionExceptions;
import ayd2.p2b.conference_service_api.feature.institution.dto.request.CreateInstitutionRequest;
import ayd2.p2b.conference_service_api.feature.institution.dto.request.UpdateInstitutionRequest;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/institutions")
@Tag(name = "Institutions", description = "Institution management endpoints")
public class InstitutionController {

    private static final String ROLE_SYSTEM_ADMIN = "ROLE_SYSTEM_ADMIN";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final CreateInstitutionUseCase createInstitutionUseCase;
    private final ListInstitutionsUseCase listInstitutionsUseCase;
    private final GetInstitutionUseCase getInstitutionUseCase;
    private final UpdateInstitutionUseCase updateInstitutionUseCase;
    private final DeleteInstitutionUseCase deleteInstitutionUseCase;

    public InstitutionController(
            CreateInstitutionUseCase createInstitutionUseCase,
            ListInstitutionsUseCase listInstitutionsUseCase,
            GetInstitutionUseCase getInstitutionUseCase,
            UpdateInstitutionUseCase updateInstitutionUseCase,
            DeleteInstitutionUseCase deleteInstitutionUseCase
    ) {
        this.createInstitutionUseCase = createInstitutionUseCase;
        this.listInstitutionsUseCase = listInstitutionsUseCase;
        this.getInstitutionUseCase = getInstitutionUseCase;
        this.updateInstitutionUseCase = updateInstitutionUseCase;
        this.deleteInstitutionUseCase = deleteInstitutionUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Create institution",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Institution created"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Name conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<InstitutionResponse>> createInstitution(
            @Valid @RequestBody CreateInstitutionRequest request,
            Authentication authentication
    ) {
        UUID actorId = requireSystemAdmin(authentication);
        InstitutionResponse response = createInstitutionUseCase.execute(request, actorId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, URI.create("/institutions/" + response.getId()).toString())
                .body(ApiResponse.of(response, "Institution created"));
    }

    @GetMapping
    @Operation(
            summary = "List active institutions",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Institutions listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<InstitutionResponse>>> listInstitutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = normalizePageable(page, size, sortBy, sortDir);
        PageResponse<InstitutionResponse> response = listInstitutionsUseCase.execute(pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get active institution by id",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Institution found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<InstitutionResponse>> getInstitution(@PathVariable UUID id) {
        InstitutionResponse response = getInstitutionUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update institution",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Institution updated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Name conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<InstitutionResponse>> updateInstitution(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInstitutionRequest request,
            Authentication authentication
    ) {
        UUID actorId = requireSystemAdmin(authentication);
        InstitutionResponse response = updateInstitutionUseCase.execute(id, request, actorId);
        return ResponseEntity.ok(ApiResponse.of(response, "Institution updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Soft-delete institution",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Institution deactivated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<InstitutionResponse>> deleteInstitution(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID actorId = requireSystemAdmin(authentication);
        InstitutionResponse response = deleteInstitutionUseCase.execute(id, actorId);
        return ResponseEntity.ok(ApiResponse.of(response, "Institution deactivated"));
    }

    private UUID requireSystemAdmin(Authentication authentication) {
        if (authentication == null) {
            throw InstitutionExceptions.forbidden();
        }

        boolean systemAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_SYSTEM_ADMIN::equals);
        if (!systemAdmin) {
            throw InstitutionExceptions.forbidden();
        }

        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw InstitutionExceptions.forbidden();
        }

        return user.getUserId();
    }

    private Pageable normalizePageable(int page, int size, String sortBy, String sortDir) {
        int normalizedPage = Math.max(DEFAULT_PAGE, page);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(direction, sortBy));
    }
}
