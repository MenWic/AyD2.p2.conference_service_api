package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.committee.application.add.AddCommitteeMemberUseCase;
import ayd2.p2b.conference_service_api.feature.committee.application.list.ListCommitteeMembersUseCase;
import ayd2.p2b.conference_service_api.feature.committee.application.remove.RemoveCommitteeMemberUseCase;
import ayd2.p2b.conference_service_api.feature.committee.dto.response.CommitteeMemberResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class CommitteeControllerTest {

    private static final String SECRET = "test_secret_key_with_at_least_32_chars";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddCommitteeMemberUseCase addCommitteeMemberUseCase;
    @MockitoBean
    private ListCommitteeMembersUseCase listCommitteeMembersUseCase;
    @MockitoBean
    private RemoveCommitteeMemberUseCase removeCommitteeMemberUseCase;

    @Test
    void shouldRequireAuthenticationForCommitteeEndpoints() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(post("/congresses/{id}/committee", congressId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberId + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/congresses/{id}/committee", congressId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/congresses/{id}/committee/{userId}", congressId, memberId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectParticipantRoleForCommitteeEndpoints() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String participantToken = tokenWithRoles(List.of("PARTICIPANT"));

        mockMvc.perform(post("/congresses/{id}/committee", congressId)
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberId + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/congresses/{id}/committee", congressId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/congresses/{id}/committee/{userId}", congressId, memberId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAddMemberWithCongressAdminToken() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        CommitteeMemberResponse response = sampleResponse(congressId, memberId);

        when(addCommitteeMemberUseCase.execute(eq(congressId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/congresses/{id}/committee", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(memberId.toString()))
                .andExpect(jsonPath("$.data.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.data.email").value("ada@example.com"))
                .andExpect(jsonPath("$.message").value("Committee member added"));
    }

    @Test
    void shouldReturnConflictWhenAddingDuplicateMember() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        when(addCommitteeMemberUseCase.execute(eq(congressId), any(), any()))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "resource.conflict", "duplicate"));

        mockMvc.perform(post("/congresses/{id}/committee", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberId + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("resource.conflict"));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenCandidateIsNotEligible() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        when(addCommitteeMemberUseCase.execute(eq(congressId), any(), any()))
                .thenThrow(new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "domain.invariant_violated", "ineligible"));

        mockMvc.perform(post("/congresses/{id}/committee", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberId + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("domain.invariant_violated"));
    }

    @Test
    void shouldAllowSystemAdminToListCommittee() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String systemAdminToken = tokenWithRoles(List.of("SYSTEM_ADMIN", "PARTICIPANT"));
        CommitteeMemberResponse item = sampleResponse(congressId, memberId);

        when(listCommitteeMembersUseCase.execute(eq(congressId), any(), any())).thenReturn(
                PageResponse.<CommitteeMemberResponse>builder()
                        .items(List.of(item))
                        .page(0)
                        .size(20)
                        .totalItems(1)
                        .totalPages(1)
                        .build()
        );

        mockMvc.perform(get("/congresses/{id}/committee", congressId)
                        .header("Authorization", "Bearer " + systemAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.data.items[0].email").value("ada@example.com"));
    }

    @Test
    void shouldRejectSystemAdminForCommitteeWriteEndpoints() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String systemAdminToken = tokenWithRoles(List.of("SYSTEM_ADMIN", "PARTICIPANT"));

        mockMvc.perform(post("/congresses/{id}/committee", congressId)
                        .header("Authorization", "Bearer " + systemAdminToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberId + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));

        mockMvc.perform(delete("/congresses/{id}/committee/{userId}", congressId, memberId)
                        .header("Authorization", "Bearer " + systemAdminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenListUseCaseReportsIamUnavailable() throws Exception {
        UUID congressId = UUID.randomUUID();
        String systemAdminToken = tokenWithRoles(List.of("SYSTEM_ADMIN", "PARTICIPANT"));
        when(listCommitteeMembersUseCase.execute(eq(congressId), any(), any()))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "integration.iam_unavailable", "iam down"));

        mockMvc.perform(get("/congresses/{id}/committee", congressId)
                        .header("Authorization", "Bearer " + systemAdminToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("integration.iam_unavailable"));
    }

    @Test
    void shouldReturnJsonApiResponseWhenRemovingMember() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));

        mockMvc.perform(delete("/congresses/{id}/committee/{userId}", congressId, memberId)
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Committee member removed"));

        verify(removeCommitteeMemberUseCase).execute(eq(congressId), eq(memberId), any());
    }

    @Test
    void shouldReturnNotFoundWhenRemovingMissingMember() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        doThrow(new ApiException(HttpStatus.NOT_FOUND, "resource.not_found", "missing"))
                .when(removeCommitteeMemberUseCase)
                .execute(eq(congressId), eq(memberId), any());

        mockMvc.perform(delete("/congresses/{id}/committee/{userId}", congressId, memberId)
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("resource.not_found"));
    }

    @Test
    void shouldReturnForbiddenWhenUseCaseRejectsUnrelatedCongressAdmin() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", "not owner"))
                .when(removeCommitteeMemberUseCase)
                .execute(eq(congressId), eq(memberId), any());

        mockMvc.perform(delete("/congresses/{id}/committee/{userId}", congressId, memberId)
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void shouldNormalizeCommitteePaginationBoundaries() throws Exception {
        UUID congressId = UUID.randomUUID();
        String systemAdminToken = tokenWithRoles(List.of("SYSTEM_ADMIN", "PARTICIPANT"));

        when(listCommitteeMembersUseCase.execute(eq(congressId), any(), any())).thenReturn(
                PageResponse.<CommitteeMemberResponse>builder()
                        .items(List.of())
                        .page(0)
                        .size(20)
                        .totalItems(0)
                        .totalPages(0)
                        .build()
        );

        mockMvc.perform(get("/congresses/{id}/committee?page=-1&size=0", congressId)
                        .header("Authorization", "Bearer " + systemAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/congresses/{id}/committee?page=3&size=999", congressId)
                        .header("Authorization", "Bearer " + systemAdminToken))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(listCommitteeMembersUseCase, org.mockito.Mockito.times(2)).execute(eq(congressId), captor.capture(), any());
        assertThat(captor.getAllValues().getFirst().getPageNumber()).isEqualTo(0);
        assertThat(captor.getAllValues().getFirst().getPageSize()).isEqualTo(20);
        assertThat(captor.getAllValues().get(1).getPageNumber()).isEqualTo(3);
        assertThat(captor.getAllValues().get(1).getPageSize()).isEqualTo(100);
    }

    @Test
    void shouldReturnBadRequestForInvalidUuidPath() throws Exception {
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        String systemAdminToken = tokenWithRoles(List.of("SYSTEM_ADMIN", "PARTICIPANT"));

        mockMvc.perform(get("/congresses/not-a-uuid/committee")
                        .header("Authorization", "Bearer " + systemAdminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));

        mockMvc.perform(delete("/congresses/{id}/committee/not-a-uuid", UUID.randomUUID())
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    private CommitteeMemberResponse sampleResponse(UUID congressId, UUID memberId) {
        return CommitteeMemberResponse.builder()
                .congressId(congressId)
                .userId(memberId)
                .fullName("Ada Lovelace")
                .email("ada@example.com")
                .addedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .build();
    }

    private String tokenWithRoles(List<String> roles) {
        return Jwts.builder()
                .claim("tokenType", "ACCESS")
                .claim("userId", UUID.randomUUID().toString())
                .claim("email", "admin@example.com")
                .claim("roles", roles)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
