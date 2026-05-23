package ayd2.p2b.conference_service_api.feature.call.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.feature.call.application.close.CloseCallUseCase;
import ayd2.p2b.conference_service_api.feature.call.application.exception.CallExceptions;
import ayd2.p2b.conference_service_api.feature.call.application.list.ListCallsUseCase;
import ayd2.p2b.conference_service_api.feature.call.application.open.OpenCallUseCase;
import ayd2.p2b.conference_service_api.feature.call.dto.internal.CallRequesterContext;
import ayd2.p2b.conference_service_api.feature.call.dto.response.CallResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@Tag(name = "Calls", description = "Scientific call management endpoints")
public class CallController {

    private static final String ROLE_CONGRESS_ADMIN = "ROLE_CONGRESS_ADMIN";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final OpenCallUseCase openCallUseCase;
    private final ListCallsUseCase listCallsUseCase;
    private final CloseCallUseCase closeCallUseCase;

    public CallController(
            OpenCallUseCase openCallUseCase,
            ListCallsUseCase listCallsUseCase,
            CloseCallUseCase closeCallUseCase
    ) {
        this.openCallUseCase = openCallUseCase;
        this.listCallsUseCase = listCallsUseCase;
        this.closeCallUseCase = closeCallUseCase;
    }

    @PostMapping("/congresses/{id}/calls")
    @Operation(
            summary = "Open a new call for a congress",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Call opened"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Open call conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<CallResponse>> openCall(
            @PathVariable("id") UUID congressId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        CallRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        CallResponse response = openCallUseCase.execute(congressId, requester);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, URI.create("/calls/" + response.getId()).toString())
                .body(ApiResponse.of(response, "Call opened"));
    }

    @GetMapping("/congresses/{id}/calls")
    @Operation(
            summary = "List calls by congress",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Calls listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress not found", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<CallResponse>>> listCallsByCongress(
            @PathVariable("id") UUID congressId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = normalizePageable(page, size);
        PageResponse<CallResponse> response = listCallsUseCase.execute(congressId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/calls/{id}/close")
    @Operation(
            summary = "Close an open call",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Call closed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Call or congress not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Call state conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<CallResponse>> closeCall(
            @PathVariable("id") UUID callId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        CallRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        CallResponse response = closeCallUseCase.execute(callId, requester);
        return ResponseEntity.ok(ApiResponse.of(response, "Call closed"));
    }

    private CallRequesterContext requireCongressAdminRequester(Authentication authentication, String authorization) {
        if (authentication == null) {
            throw CallExceptions.forbidden("Congress admin role is required");
        }
        boolean congressAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_CONGRESS_ADMIN::equals);
        if (!congressAdmin) {
            throw CallExceptions.forbidden("Congress admin role is required");
        }
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw CallExceptions.forbidden("Authenticated user context is required");
        }
        if (user.getUserId() == null) {
            throw CallExceptions.forbidden("Authenticated user context is required");
        }

        return CallRequesterContext.builder()
                .userId(user.getUserId())
                .roles(user.getRoles() == null ? Set.of() : user.getRoles())
                .accessToken(extractBearerToken(authorization))
                .build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw CallExceptions.forbidden("Bearer token is required");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw CallExceptions.forbidden("Bearer token is required");
        }
        return token;
    }

    private Pageable normalizePageable(int page, int size) {
        int normalizedPage = Math.max(DEFAULT_PAGE, page);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "openedAt"));
    }
}
