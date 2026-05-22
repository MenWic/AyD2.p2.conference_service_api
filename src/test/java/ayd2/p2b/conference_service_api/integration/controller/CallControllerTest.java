package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.call.application.close.CloseCallUseCase;
import ayd2.p2b.conference_service_api.feature.call.application.list.ListCallsUseCase;
import ayd2.p2b.conference_service_api.feature.call.application.open.OpenCallUseCase;
import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
import ayd2.p2b.conference_service_api.feature.call.dto.response.CallResponse;
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
class CallControllerTest {

    private static final String SECRET = "test_secret_key_with_at_least_32_chars";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenCallUseCase openCallUseCase;
    @MockitoBean
    private ListCallsUseCase listCallsUseCase;
    @MockitoBean
    private CloseCallUseCase closeCallUseCase;

    @Test
    void shouldAllowPublicListCallsWithoutToken() throws Exception {
        UUID congressId = UUID.randomUUID();
        CallResponse response = sampleResponse(congressId);
        when(listCallsUseCase.execute(eq(congressId), any())).thenReturn(PageResponse.<CallResponse>builder()
                .items(List.of(response))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .build());

        mockMvc.perform(get("/congresses/{id}/calls", congressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].status").value("OPEN"));
    }

    @Test
    void shouldRequireAuthenticationForWriteEndpoints() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID callId = UUID.randomUUID();

        mockMvc.perform(post("/congresses/{id}/calls", congressId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/calls/{id}/close", callId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectNonCongressAdminForWrites() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID callId = UUID.randomUUID();
        String participantToken = tokenWithRoles(List.of("PARTICIPANT"));
        String systemAdminToken = tokenWithRoles(List.of("SYSTEM_ADMIN", "PARTICIPANT"));

        mockMvc.perform(post("/congresses/{id}/calls", congressId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/congresses/{id}/calls", congressId)
                        .header("Authorization", "Bearer " + systemAdminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/calls/{id}/close", callId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldOpenAndCloseWithoutRequestBody() throws Exception {
        UUID congressId = UUID.randomUUID();
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        CallResponse response = sampleResponse(congressId);

        when(openCallUseCase.execute(eq(congressId), any())).thenReturn(response);
        when(closeCallUseCase.execute(eq(response.getId()), any())).thenReturn(CallResponse.builder()
                .id(response.getId())
                .congressId(response.getCongressId())
                .status(CallStatus.CLOSED)
                .openedAt(response.getOpenedAt())
                .closedAt(OffsetDateTime.parse("2026-10-10T18:00:00Z"))
                .build());

        mockMvc.perform(post("/congresses/{id}/calls", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.message").value("Call opened"));

        mockMvc.perform(patch("/calls/{id}/close", response.getId())
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.message").value("Call closed"));
    }

    @Test
    void shouldReturnConflictWhenOpenUseCaseReportsExistingOpenCall() throws Exception {
        UUID congressId = UUID.randomUUID();
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));

        when(openCallUseCase.execute(eq(congressId), any()))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "resource.conflict", "open call exists"));

        mockMvc.perform(post("/congresses/{id}/calls", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("resource.conflict"));
    }

    @Test
    void shouldReturnConflictWhenClosingAlreadyClosedCall() throws Exception {
        UUID callId = UUID.randomUUID();
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));

        when(closeCallUseCase.execute(eq(callId), any()))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "resource.conflict", "already closed"));

        mockMvc.perform(patch("/calls/{id}/close", callId)
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("resource.conflict"));
    }

    @Test
    void shouldNormalizeCallsPaginationBoundaries() throws Exception {
        UUID congressId = UUID.randomUUID();
        when(listCallsUseCase.execute(eq(congressId), any())).thenReturn(PageResponse.<CallResponse>builder()
                .items(List.of())
                .page(0)
                .size(20)
                .totalItems(0)
                .totalPages(0)
                .build());

        mockMvc.perform(get("/congresses/{id}/calls?page=-1&size=0", congressId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/congresses/{id}/calls?page=2&size=999", congressId))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(listCallsUseCase, org.mockito.Mockito.times(2)).execute(eq(congressId), captor.capture());
        assertThat(captor.getAllValues().getFirst().getPageNumber()).isEqualTo(0);
        assertThat(captor.getAllValues().getFirst().getPageSize()).isEqualTo(20);
        assertThat(captor.getAllValues().get(1).getPageNumber()).isEqualTo(2);
        assertThat(captor.getAllValues().get(1).getPageSize()).isEqualTo(100);
    }

    private CallResponse sampleResponse(UUID congressId) {
        return CallResponse.builder()
                .id(UUID.randomUUID())
                .congressId(congressId)
                .status(CallStatus.OPEN)
                .openedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .closedAt(null)
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
