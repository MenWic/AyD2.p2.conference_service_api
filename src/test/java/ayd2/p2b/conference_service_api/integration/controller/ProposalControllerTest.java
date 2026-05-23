package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.proposal.application.list.ListProposalsByCallUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.application.list.ListUserProposalsUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.application.review.ApproveProposalUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.application.review.RejectProposalUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.application.submit.SubmitProposalUseCase;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import ayd2.p2b.conference_service_api.feature.proposal.dto.response.ProposalResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
        "security.jwt.secret=test_secret_key_with_at_least_32_chars",
        "spring.main.lazy-initialization=true"
})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
class ProposalControllerTest {

    private static final String SECRET = "test_secret_key_with_at_least_32_chars";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmitProposalUseCase submitProposalUseCase;
    @MockitoBean
    private ListProposalsByCallUseCase listProposalsByCallUseCase;
    @MockitoBean
    private ListUserProposalsUseCase listUserProposalsUseCase;
    @MockitoBean
    private ApproveProposalUseCase approveProposalUseCase;
    @MockitoBean
    private RejectProposalUseCase rejectProposalUseCase;

    @Test
    void shouldRequireAuthenticationForAllProposalEndpoints() throws Exception {
        UUID callId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/calls/{id}/proposals", callId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Observability at scale",
                                  "description": "Metrics and tracing strategy.",
                                  "type": "PONENCIA"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/calls/{id}/proposals", callId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users/{id}/proposals", userId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/proposals/{id}/approve", proposalId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/proposals/{id}/reject", proposalId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldSubmitProposalAndReturnApiResponse() throws Exception {
        UUID callId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        ProposalResponse response = proposalResponse(callId, authorId, ProposalStatus.PENDING);
        String token = tokenWithRoles(authorId, List.of("PARTICIPANT"));

        when(submitProposalUseCase.execute(eq(callId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/calls/{id}/proposals", callId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Event sourcing for conference workflows",
                                  "description": "Practical event sourcing patterns.",
                                  "type": "PONENCIA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.callId").value(callId.toString()))
                .andExpect(jsonPath("$.data.authorUserId").value(authorId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Proposal submitted"));
    }

    @Test
    void shouldListProposalsByCallWithPageResponseEnvelope() throws Exception {
        UUID callId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        ProposalResponse item = proposalResponse(callId, authorId, ProposalStatus.PENDING);
        String token = tokenWithRoles(UUID.randomUUID(), List.of("CONGRESS_ADMIN", "PARTICIPANT"));

        when(listProposalsByCallUseCase.execute(eq(callId), any(), any())).thenReturn(
                PageResponse.<ProposalResponse>builder()
                        .items(List.of(item))
                        .page(0)
                        .size(20)
                        .totalItems(1)
                        .totalPages(1)
                        .build()
        );

        mockMvc.perform(get("/calls/{id}/proposals", callId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(item.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void shouldEnforceSelfOnlyListByUserPath() throws Exception {
        UUID requesterId = UUID.randomUUID();
        UUID pathUserId = UUID.randomUUID();
        String token = tokenWithRoles(requesterId, List.of("PARTICIPANT"));

        when(listUserProposalsUseCase.execute(eq(pathUserId), any(), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", "self only"));

        mockMvc.perform(get("/users/{id}/proposals", pathUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void shouldAllowSelfListByUserPath() throws Exception {
        UUID requesterId = UUID.randomUUID();
        String token = tokenWithRoles(requesterId, List.of("PARTICIPANT"));
        ProposalResponse item = proposalResponse(UUID.randomUUID(), requesterId, ProposalStatus.PENDING);

        when(listUserProposalsUseCase.execute(eq(requesterId), any(), any())).thenReturn(
                PageResponse.<ProposalResponse>builder()
                        .items(List.of(item))
                        .page(0)
                        .size(20)
                        .totalItems(1)
                        .totalPages(1)
                        .build()
        );

        mockMvc.perform(get("/users/{id}/proposals", requesterId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].authorUserId").value(requesterId.toString()));

        verify(listUserProposalsUseCase).execute(eq(requesterId), any(), any());
    }

    @Test
    void shouldApproveProposalWithoutRequestBody() throws Exception {
        UUID proposalId = UUID.randomUUID();
        ProposalResponse response = proposalResponse(UUID.randomUUID(), UUID.randomUUID(), ProposalStatus.APPROVED);
        response.setId(proposalId);
        String token = tokenWithRoles(UUID.randomUUID(), List.of("PARTICIPANT"));

        when(approveProposalUseCase.execute(eq(proposalId), any())).thenReturn(response);

        mockMvc.perform(patch("/proposals/{id}/approve", proposalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(proposalId.toString()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.message").value("Proposal approved"));
    }

    @Test
    void shouldRejectProposalWithoutRequestBody() throws Exception {
        UUID proposalId = UUID.randomUUID();
        ProposalResponse response = proposalResponse(UUID.randomUUID(), UUID.randomUUID(), ProposalStatus.REJECTED);
        response.setId(proposalId);
        String token = tokenWithRoles(UUID.randomUUID(), List.of("PARTICIPANT"));

        when(rejectProposalUseCase.execute(eq(proposalId), any())).thenReturn(response);

        mockMvc.perform(patch("/proposals/{id}/reject", proposalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(proposalId.toString()))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.message").value("Proposal rejected"));
    }

    @Test
    void shouldReturnForbiddenWhenSystemAdminApprovesWithoutCommitteeMembership() throws Exception {
        UUID proposalId = UUID.randomUUID();
        String token = tokenWithRoles(UUID.randomUUID(), List.of("SYSTEM_ADMIN", "PARTICIPANT"));
        when(approveProposalUseCase.execute(eq(proposalId), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", "not committee member"));

        mockMvc.perform(patch("/proposals/{id}/approve", proposalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void shouldReturnForbiddenWhenCongressAdminApprovesWithoutCommitteeMembership() throws Exception {
        UUID proposalId = UUID.randomUUID();
        String token = tokenWithRoles(UUID.randomUUID(), List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        when(approveProposalUseCase.execute(eq(proposalId), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", "not committee member"));

        mockMvc.perform(patch("/proposals/{id}/approve", proposalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void shouldReturnForbiddenWhenCommitteeMemberFromOtherCongressRejects() throws Exception {
        UUID proposalId = UUID.randomUUID();
        String token = tokenWithRoles(UUID.randomUUID(), List.of("PARTICIPANT"));
        when(rejectProposalUseCase.execute(eq(proposalId), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", "not committee member for congress"));

        mockMvc.perform(patch("/proposals/{id}/reject", proposalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void shouldReturnBadRequestForMalformedSubmitBody() throws Exception {
        UUID callId = UUID.randomUUID();
        String token = tokenWithRoles(UUID.randomUUID(), List.of("PARTICIPANT"));

        mockMvc.perform(post("/calls/{id}/proposals", callId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Distributed data",
                                  "description": "Malformed enum sample",
                                  "type": "INVALID_TYPE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    @Test
    void shouldReturnBadRequestForInvalidUuidPath() throws Exception {
        String token = tokenWithRoles(UUID.randomUUID(), List.of("PARTICIPANT"));

        mockMvc.perform(get("/calls/not-a-uuid/proposals")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));

        mockMvc.perform(patch("/proposals/not-a-uuid/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    private ProposalResponse proposalResponse(UUID callId, UUID authorId, ProposalStatus status) {
        return ProposalResponse.builder()
                .id(UUID.randomUUID())
                .callId(callId)
                .authorUserId(authorId)
                .title("Event sourcing for conference workflows")
                .description("Practical event sourcing patterns.")
                .type(ProposalType.PONENCIA)
                .status(status)
                .reviewedBy(status == ProposalStatus.PENDING ? null : UUID.randomUUID())
                .reviewedAt(status == ProposalStatus.PENDING ? null : OffsetDateTime.parse("2026-10-10T12:00:00Z"))
                .createdAt(OffsetDateTime.parse("2026-10-09T09:00:00Z"))
                .build();
    }

    private String tokenWithRoles(UUID userId, List<String> roles) {
        return Jwts.builder()
                .claim("tokenType", "ACCESS")
                .claim("userId", userId.toString())
                .claim("email", "participant@example.com")
                .claim("roles", roles)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
