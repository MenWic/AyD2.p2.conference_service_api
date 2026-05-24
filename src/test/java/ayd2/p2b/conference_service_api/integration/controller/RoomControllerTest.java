package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.room.application.create.CreateRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.delete.DeleteRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.get.GetRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.list.ListRoomsUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.update.UpdateRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class RoomControllerTest {

    private static final String SECRET = "test_secret_key_with_at_least_32_chars";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateRoomUseCase createRoomUseCase;
    @MockitoBean
    private ListRoomsUseCase listRoomsUseCase;
    @MockitoBean
    private GetRoomUseCase getRoomUseCase;
    @MockitoBean
    private UpdateRoomUseCase updateRoomUseCase;
    @MockitoBean
    private DeleteRoomUseCase deleteRoomUseCase;

    @Test
    void shouldAllowPublicGetRoomsWithoutToken() throws Exception {
        UUID congressId = UUID.randomUUID();
        when(listRoomsUseCase.execute(eq(congressId), any())).thenReturn(PageResponse.<RoomResponse>builder()
                .items(List.of(sampleResponse(congressId)))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .build());

        mockMvc.perform(get("/congresses/{id}/rooms", congressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Sala Magna"));
    }

    @Test
    void shouldAllowPublicGetRoomByIdWithoutToken() throws Exception {
        UUID congressId = UUID.randomUUID();
        RoomResponse response = sampleResponse(congressId);
        when(getRoomUseCase.execute(response.getId())).thenReturn(response);

        mockMvc.perform(get("/rooms/{id}", response.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.congressId").value(congressId.toString()));
    }

    @Test
    void shouldRequireAuthenticationForWriteEndpoints() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();

        mockMvc.perform(post("/congresses/{id}/rooms", congressId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/rooms/{id}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sala Norte\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/rooms/{id}", roomId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectParticipantAndSystemAdminForRoomWrites() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        String participantToken = tokenWithRoles(List.of("PARTICIPANT"));
        String systemAdminToken = tokenWithRoles(List.of("SYSTEM_ADMIN", "PARTICIPANT"));

        mockMvc.perform(post("/congresses/{id}/rooms", congressId)
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/congresses/{id}/rooms", congressId)
                        .header("Authorization", "Bearer " + systemAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sala Norte\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + systemAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sala Norte\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + systemAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReachUseCasesWithCongressAdminToken() throws Exception {
        UUID congressId = UUID.randomUUID();
        RoomResponse response = sampleResponse(congressId);
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));

        when(createRoomUseCase.execute(eq(congressId), any(), any())).thenReturn(response);
        when(updateRoomUseCase.execute(eq(response.getId()), any(), any())).thenReturn(response);
        when(deleteRoomUseCase.execute(eq(response.getId()), any())).thenReturn(response);

        mockMvc.perform(post("/congresses/{id}/rooms", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.message").value("Room created"));

        mockMvc.perform(put("/rooms/{id}", response.getId())
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sala Norte\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Room updated"));

        mockMvc.perform(delete("/rooms/{id}", response.getId())
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Room deleted"));

        verify(createRoomUseCase).execute(eq(congressId), any(), any());
        verify(updateRoomUseCase).execute(eq(response.getId()), any(), any());
        verify(deleteRoomUseCase).execute(eq(response.getId()), any());
    }

    @Test
    void shouldReturnValidationFailedForInvalidRequestBody() throws Exception {
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        UUID congressId = UUID.randomUUID();

        mockMvc.perform(post("/congresses/{id}/rooms", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    @Test
    void shouldReturnConflictWhenDeleteUseCaseReportsDependencies() throws Exception {
        UUID roomId = UUID.randomUUID();
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        when(deleteRoomUseCase.execute(eq(roomId), any()))
                .thenThrow(new ayd2.p2b.conference_service_api.common.exception.ApiException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "resource.conflict",
                        "Room has dependent activities"
                ));

        mockMvc.perform(delete("/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("resource.conflict"));
    }

    @Test
    void shouldNormalizeRoomPaginationBoundaries() throws Exception {
        UUID congressId = UUID.randomUUID();
        when(listRoomsUseCase.execute(eq(congressId), any())).thenReturn(PageResponse.<RoomResponse>builder()
                .items(List.of())
                .page(0)
                .size(20)
                .totalItems(0)
                .totalPages(0)
                .build());

        mockMvc.perform(get("/congresses/{id}/rooms?page=-1&size=0", congressId))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(listRoomsUseCase).execute(eq(congressId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void shouldCapRoomPaginationSizeToOneHundred() throws Exception {
        UUID congressId = UUID.randomUUID();
        when(listRoomsUseCase.execute(eq(congressId), any())).thenReturn(PageResponse.<RoomResponse>builder()
                .items(List.of())
                .page(0)
                .size(100)
                .totalItems(0)
                .totalPages(0)
                .build());

        mockMvc.perform(get("/congresses/{id}/rooms?page=3&size=999", congressId))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(listRoomsUseCase).execute(eq(congressId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(3);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    private RoomResponse sampleResponse(UUID congressId) {
        return RoomResponse.builder()
                .id(UUID.randomUUID())
                .congressId(congressId)
                .name("Sala Magna")
                .capacity(120)
                .location("Edificio A")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String createPayload() {
        return """
                {
                  "name":"Sala Magna",
                  "capacity":120,
                  "location":"Edificio A"
                }
                """;
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
