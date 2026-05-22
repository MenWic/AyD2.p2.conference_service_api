package ayd2.p2b.conference_service_api.feature.committee.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.feature.committee.application.add.AddCommitteeMemberUseCase;
import ayd2.p2b.conference_service_api.feature.committee.application.exception.CommitteeExceptions;
import ayd2.p2b.conference_service_api.feature.committee.application.list.ListCommitteeMembersUseCase;
import ayd2.p2b.conference_service_api.feature.committee.application.remove.RemoveCommitteeMemberUseCase;
import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeRequesterContext;
import ayd2.p2b.conference_service_api.feature.committee.dto.request.AddCommitteeMemberRequest;
import ayd2.p2b.conference_service_api.feature.committee.dto.response.CommitteeMemberResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Committee", description = "Scientific committee management endpoints")
public class CommitteeController {

    private static final String ROLE_CONGRESS_ADMIN = "ROLE_CONGRESS_ADMIN";
    private static final String ROLE_SYSTEM_ADMIN = "ROLE_SYSTEM_ADMIN";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AddCommitteeMemberUseCase addCommitteeMemberUseCase;
    private final ListCommitteeMembersUseCase listCommitteeMembersUseCase;
    private final RemoveCommitteeMemberUseCase removeCommitteeMemberUseCase;

    public CommitteeController(
            AddCommitteeMemberUseCase addCommitteeMemberUseCase,
            ListCommitteeMembersUseCase listCommitteeMembersUseCase,
            RemoveCommitteeMemberUseCase removeCommitteeMemberUseCase
    ) {
        this.addCommitteeMemberUseCase = addCommitteeMemberUseCase;
        this.listCommitteeMembersUseCase = listCommitteeMembersUseCase;
        this.removeCommitteeMemberUseCase = removeCommitteeMemberUseCase;
    }

    @PostMapping("/congresses/{id}/committee")
    @Operation(
            summary = "Add committee member to congress",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Committee member added"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Committee membership conflict", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Domain invariant violated", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "IAM unavailable", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<CommitteeMemberResponse>> addCommitteeMember(
            @PathVariable("id") UUID congressId,
            @Valid @RequestBody AddCommitteeMemberRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        CommitteeRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        CommitteeMemberResponse response = addCommitteeMemberUseCase.execute(congressId, request, requester);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, URI.create("/congresses/" + congressId + "/committee/" + response.getUserId()).toString())
                .body(ApiResponse.of(response, "Committee member added"));
    }

    @GetMapping("/congresses/{id}/committee")
    @Operation(
            summary = "List committee members by congress",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Committee members listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "IAM unavailable", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<CommitteeMemberResponse>>> listCommitteeMembers(
            @PathVariable("id") UUID congressId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CommitteeRequesterContext requester = requireCommitteeReadRequester(authentication, authorization);
        Pageable pageable = normalizePageable(page, size);
        PageResponse<CommitteeMemberResponse> response = listCommitteeMembersUseCase.execute(congressId, pageable, requester);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/congresses/{id}/committee/{userId}")
    @Operation(
            summary = "Remove committee member from congress",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Committee member removed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress or membership not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "IAM unavailable", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<Void>> removeCommitteeMember(
            @PathVariable("id") UUID congressId,
            @PathVariable("userId") UUID userId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        CommitteeRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        removeCommitteeMemberUseCase.execute(congressId, userId, requester);
        return ResponseEntity.ok(ApiResponse.of(null, "Committee member removed"));
    }

    private CommitteeRequesterContext requireCongressAdminRequester(Authentication authentication, String authorization) {
        if (authentication == null) {
            throw CommitteeExceptions.forbidden("Congress admin role is required");
        }
        boolean congressAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_CONGRESS_ADMIN::equals);
        if (!congressAdmin) {
            throw CommitteeExceptions.forbidden("Congress admin role is required");
        }
        return buildRequesterContext(authentication, authorization);
    }

    private CommitteeRequesterContext requireCommitteeReadRequester(Authentication authentication, String authorization) {
        if (authentication == null) {
            throw CommitteeExceptions.forbidden("System admin or congress admin role is required");
        }
        boolean allowed = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> ROLE_CONGRESS_ADMIN.equals(authority) || ROLE_SYSTEM_ADMIN.equals(authority));
        if (!allowed) {
            throw CommitteeExceptions.forbidden("System admin or congress admin role is required");
        }
        return buildRequesterContext(authentication, authorization);
    }

    private CommitteeRequesterContext buildRequesterContext(Authentication authentication, String authorization) {
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw CommitteeExceptions.forbidden("Authenticated user context is required");
        }
        if (user.getUserId() == null) {
            throw CommitteeExceptions.forbidden("Authenticated user context is required");
        }
        return CommitteeRequesterContext.builder()
                .userId(user.getUserId())
                .roles(user.getRoles() == null ? Set.of() : user.getRoles())
                .accessToken(extractBearerToken(authorization))
                .build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw CommitteeExceptions.forbidden("Bearer token is required");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw CommitteeExceptions.forbidden("Bearer token is required");
        }
        return token;
    }

    private Pageable normalizePageable(int page, int size) {
        int normalizedPage = Math.max(DEFAULT_PAGE, page);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.ASC, "addedAt"));
    }
}
