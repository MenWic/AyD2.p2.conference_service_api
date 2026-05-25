package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.diploma.application.get.GetDiplomaMetadataUseCase;
import ayd2.p2b.conference_service_api.feature.diploma.application.list_by_user.ListUserDiplomasUseCase;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class DiplomaControllerTest {

    private static final String SECRET = "test_secret_key_with_at_least_32_chars";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListUserDiplomasUseCase listUserDiplomasUseCase;
    @MockitoBean
    private GetDiplomaMetadataUseCase getDiplomaMetadataUseCase;

    @Test
    void shouldReturnOkForSelfDiplomaList() throws Exception {
        UUID userId = UUID.randomUUID();
        DiplomaResponse response = participationResponse(userId);
        when(listUserDiplomasUseCase.execute(eq(userId), any(), any()))
                .thenReturn(PageResponse.<DiplomaResponse>builder()
                        .items(List.of(response))
                        .page(0)
                        .size(20)
                        .totalItems(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/users/{id}/diplomas", userId)
                        .header("Authorization", "Bearer " + tokenWithRoles(userId, List.of("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].available").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void shouldReturnForbiddenForAnotherUserDiplomaList() throws Exception {
        UUID requestedUserId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(listUserDiplomasUseCase.execute(eq(requestedUserId), any(), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", "self only"));

        mockMvc.perform(get("/users/{id}/diplomas", requestedUserId)
                        .header("Authorization", "Bearer " + tokenWithRoles(requesterId, List.of("PARTICIPANT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void shouldReturnOkForOwnerDiplomaMetadata() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID diplomaId = UUID.randomUUID();
        DiplomaResponse response = participationResponse(userId);
        response.setId(diplomaId);

        when(getDiplomaMetadataUseCase.execute(eq(diplomaId), any())).thenReturn(response);

        mockMvc.perform(get("/diplomas/{id}", diplomaId)
                        .header("Authorization", "Bearer " + tokenWithRoles(userId, List.of("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(diplomaId.toString()))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.type").value("PARTICIPATION"));
    }

    @Test
    void shouldReturnForbiddenForNonOwnerDiplomaMetadata() throws Exception {
        UUID diplomaId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(getDiplomaMetadataUseCase.execute(eq(diplomaId), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", "owner only"));

        mockMvc.perform(get("/diplomas/{id}", diplomaId)
                        .header("Authorization", "Bearer " + tokenWithRoles(requesterId, List.of("PARTICIPANT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void listResponseShouldUseApiResponsePageResponseShape() throws Exception {
        UUID userId = UUID.randomUUID();
        DiplomaResponse response = participationResponse(userId);
        when(listUserDiplomasUseCase.execute(eq(userId), any(), any()))
                .thenReturn(PageResponse.<DiplomaResponse>builder()
                        .items(List.of(response))
                        .page(0)
                        .size(20)
                        .totalItems(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/users/{id}/diplomas", userId)
                        .header("Authorization", "Bearer " + tokenWithRoles(userId, List.of("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    void metadataResponseShouldUseApiResponseShape() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID diplomaId = UUID.randomUUID();
        DiplomaResponse response = participationResponse(userId);
        response.setId(diplomaId);
        when(getDiplomaMetadataUseCase.execute(eq(diplomaId), any())).thenReturn(response);

        mockMvc.perform(get("/diplomas/{id}", diplomaId)
                        .header("Authorization", "Bearer " + tokenWithRoles(userId, List.of("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(diplomaId.toString()));
    }

    private DiplomaResponse participationResponse(UUID userId) {
        return DiplomaResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .congressId(UUID.randomUUID())
                .type(DiplomaType.PARTICIPATION)
                .activityId(null)
                .issuedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .congressName("Congreso")
                .activityName(null)
                .available(true)
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
