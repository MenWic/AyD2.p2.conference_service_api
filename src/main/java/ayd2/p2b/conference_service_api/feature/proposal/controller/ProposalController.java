package ayd2.p2b.conference_service_api.feature.proposal.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.feature.proposal.application.exception.ProposalExceptions;
import ayd2.p2b.conference_service_api.feature.proposal.application.list.ListProposalsByCallUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.application.list.ListUserProposalsUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.application.review.ApproveProposalUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.application.review.RejectProposalUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.application.submit.SubmitProposalUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalRequesterContext;
import ayd2.p2b.conference_service_api.feature.proposal.dto.request.CreateProposalRequest;
import ayd2.p2b.conference_service_api.feature.proposal.dto.response.ProposalResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@Tag(name = "Proposals", description = "Scientific proposal submission and review endpoints")
public class ProposalController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final SubmitProposalUseCase submitProposalUseCase;
    private final ListProposalsByCallUseCase listProposalsByCallUseCase;
    private final ListUserProposalsUseCase listUserProposalsUseCase;
    private final ApproveProposalUseCase approveProposalUseCase;
    private final RejectProposalUseCase rejectProposalUseCase;

    public ProposalController(
            SubmitProposalUseCase submitProposalUseCase,
            ListProposalsByCallUseCase listProposalsByCallUseCase,
            ListUserProposalsUseCase listUserProposalsUseCase,
            ApproveProposalUseCase approveProposalUseCase,
            RejectProposalUseCase rejectProposalUseCase
    ) {
        this.submitProposalUseCase = submitProposalUseCase;
        this.listProposalsByCallUseCase = listProposalsByCallUseCase;
        this.listUserProposalsUseCase = listUserProposalsUseCase;
        this.approveProposalUseCase = approveProposalUseCase;
        this.rejectProposalUseCase = rejectProposalUseCase;
    }

    @PostMapping("/calls/{id}/proposals")
    @Operation(
            summary = "Submit proposal to an open call",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Proposal submitted"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Call not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Domain invariant violated", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<ProposalResponse>> submitProposal(
            @PathVariable("id") UUID callId,
            @Valid @RequestBody CreateProposalRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ProposalRequesterContext requester = buildRequesterContext(authentication, authorization);
        ProposalResponse response = submitProposalUseCase.execute(callId, request, requester);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, URI.create("/proposals/" + response.getId()).toString())
                .body(ApiResponse.of(response, "Proposal submitted"));
    }

    @GetMapping("/calls/{id}/proposals")
    @Operation(
            summary = "List proposals by call",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proposals listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Call not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "IAM unavailable", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<ProposalResponse>>> listByCall(
            @PathVariable("id") UUID callId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ProposalRequesterContext requester = buildRequesterContext(authentication, authorization);
        Pageable pageable = normalizePageable(page, size);
        PageResponse<ProposalResponse> response = listProposalsByCallUseCase.execute(callId, pageable, requester);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/users/{id}/proposals")
    @Operation(
            summary = "List proposals by user (self only)",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proposals listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<ProposalResponse>>> listByUser(
            @PathVariable("id") UUID userId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ProposalRequesterContext requester = buildRequesterContext(authentication, authorization);
        Pageable pageable = normalizePageable(page, size);
        PageResponse<ProposalResponse> response = listUserProposalsUseCase.execute(userId, pageable, requester);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/proposals/{id}/approve")
    @Operation(
            summary = "Approve proposal",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proposal approved"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Proposal not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Proposal review conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<ProposalResponse>> approveProposal(
            @PathVariable("id") UUID proposalId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ProposalRequesterContext requester = buildRequesterContext(authentication, authorization);
        ProposalResponse response = approveProposalUseCase.execute(proposalId, requester);
        return ResponseEntity.ok(ApiResponse.of(response, "Proposal approved"));
    }

    @PatchMapping("/proposals/{id}/reject")
    @Operation(
            summary = "Reject proposal",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proposal rejected"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Proposal not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Proposal review conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<ProposalResponse>> rejectProposal(
            @PathVariable("id") UUID proposalId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        ProposalRequesterContext requester = buildRequesterContext(authentication, authorization);
        ProposalResponse response = rejectProposalUseCase.execute(proposalId, requester);
        return ResponseEntity.ok(ApiResponse.of(response, "Proposal rejected"));
    }

    private ProposalRequesterContext buildRequesterContext(Authentication authentication, String authorization) {
        if (authentication == null) {
            throw ProposalExceptions.forbidden("Authenticated user context is required");
        }
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw ProposalExceptions.forbidden("Authenticated user context is required");
        }
        if (user.getUserId() == null) {
            throw ProposalExceptions.forbidden("Authenticated user context is required");
        }
        return ProposalRequesterContext.builder()
                .userId(user.getUserId())
                .roles(user.getRoles() == null ? Set.of() : user.getRoles())
                .accessToken(extractBearerToken(authorization))
                .build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw ProposalExceptions.forbidden("Bearer token is required");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw ProposalExceptions.forbidden("Bearer token is required");
        }
        return token;
    }

    private Pageable normalizePageable(int page, int size) {
        int normalizedPage = Math.max(DEFAULT_PAGE, page);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
