package ayd2.p2b.conference_service_api.feature.diploma.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.feature.diploma.application.exception.DiplomaExceptions;
import ayd2.p2b.conference_service_api.feature.diploma.application.get.GetDiplomaMetadataUseCase;
import ayd2.p2b.conference_service_api.feature.diploma.application.list_by_user.ListUserDiplomasUseCase;
import ayd2.p2b.conference_service_api.feature.diploma.application.print_data.GetDiplomaPrintDataUseCase;
import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaRequesterContext;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaPrintDataResponse;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Diplomas", description = "Diploma listing and metadata endpoints")
public class DiplomaController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ListUserDiplomasUseCase listUserDiplomasUseCase;
    private final GetDiplomaMetadataUseCase getDiplomaMetadataUseCase;
    private final GetDiplomaPrintDataUseCase getDiplomaPrintDataUseCase;

    public DiplomaController(
            ListUserDiplomasUseCase listUserDiplomasUseCase,
            GetDiplomaMetadataUseCase getDiplomaMetadataUseCase,
            GetDiplomaPrintDataUseCase getDiplomaPrintDataUseCase
    ) {
        this.listUserDiplomasUseCase = listUserDiplomasUseCase;
        this.getDiplomaMetadataUseCase = getDiplomaMetadataUseCase;
        this.getDiplomaPrintDataUseCase = getDiplomaPrintDataUseCase;
    }

    @GetMapping("/users/{id}/diplomas")
    @Operation(
            summary = "List user diplomas (self only)",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Diplomas listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<DiplomaResponse>>> listUserDiplomas(
            @PathVariable("id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        DiplomaRequesterContext requester = buildRequesterContext(authentication);
        Pageable pageable = normalizePageable(page, size);
        PageResponse<DiplomaResponse> response = listUserDiplomasUseCase.execute(userId, pageable, requester);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/diplomas/{id}")
    @Operation(
            summary = "Get diploma metadata",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Diploma metadata"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Diploma not found", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<DiplomaResponse>> getDiplomaMetadata(
            @PathVariable("id") UUID diplomaId,
            Authentication authentication
    ) {
        DiplomaRequesterContext requester = buildRequesterContext(authentication);
        DiplomaResponse response = getDiplomaMetadataUseCase.execute(diplomaId, requester);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/diplomas/{id}/print-data")
    @Operation(
            summary = "Get official diploma print data",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Diploma print data"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Diploma not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "IAM unavailable", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<DiplomaPrintDataResponse>> getDiplomaPrintData(
            @PathVariable("id") UUID diplomaId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        DiplomaRequesterContext requester = buildRequesterContext(authentication, authorization);
        DiplomaPrintDataResponse response = getDiplomaPrintDataUseCase.execute(diplomaId, requester);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    private DiplomaRequesterContext buildRequesterContext(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw DiplomaExceptions.forbidden("Authenticated participant context is required");
        }
        if (user.getUserId() == null) {
            throw DiplomaExceptions.forbidden("Authenticated participant context is required");
        }
        return DiplomaRequesterContext.builder()
                .userId(user.getUserId())
                .roles(user.getRoles() == null ? Set.of() : user.getRoles())
                .build();
    }

    private DiplomaRequesterContext buildRequesterContext(Authentication authentication, String authorization) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw DiplomaExceptions.forbidden("Authenticated participant context is required");
        }
        if (user.getUserId() == null) {
            throw DiplomaExceptions.forbidden("Authenticated participant context is required");
        }
        return DiplomaRequesterContext.builder()
                .userId(user.getUserId())
                .roles(user.getRoles() == null ? Set.of() : user.getRoles())
                .accessToken(extractBearerToken(authorization))
                .build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw DiplomaExceptions.forbidden("Bearer token is required");
        }
        String token = authorization.substring(7).trim();
        if (token.isBlank()) {
            throw DiplomaExceptions.forbidden("Bearer token is required");
        }
        return token;
    }

    private Pageable normalizePageable(int page, int size) {
        int normalizedPage = Math.max(DEFAULT_PAGE, page);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "issuedAt"));
    }
}
