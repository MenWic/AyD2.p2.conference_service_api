package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.reservation.application.cancel.CancelReservationUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.application.list_by_activity.ListActivityReservationsUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.application.list_by_user.ListUserReservationsUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.application.reserve.ReserveActivityUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.dto.response.ReservationResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class ReservationControllerTest {

    private static final String SECRET = "test_secret_key_with_at_least_32_chars";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReserveActivityUseCase reserveActivityUseCase;
    @MockitoBean
    private ListActivityReservationsUseCase listActivityReservationsUseCase;
    @MockitoBean
    private ListUserReservationsUseCase listUserReservationsUseCase;
    @MockitoBean
    private CancelReservationUseCase cancelReservationUseCase;

    @Test
    void shouldRequireAuthenticationForReservationEndpoints() throws Exception {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        mockMvc.perform(post("/activities/{id}/reservations", activityId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/activities/{id}/reservations", activityId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users/{id}/reservations", userId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/reservations/{id}", reservationId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateReservationAndReturnApiResponse() throws Exception {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ReservationResponse response = reservationResponse(activityId, userId);

        when(reserveActivityUseCase.execute(eq(activityId), any())).thenReturn(response);

        mockMvc.perform(post("/activities/{id}/reservations", activityId)
                        .header("Authorization", "Bearer " + tokenWithRoles(userId, List.of("PARTICIPANT"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.activityId").value(activityId.toString()))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.message").value("Reservation created"));
    }

    @Test
    void shouldListActivityReservationsWithPageEnvelope() throws Exception {
        UUID activityId = UUID.randomUUID();
        ReservationResponse item = reservationResponse(activityId, UUID.randomUUID());

        when(listActivityReservationsUseCase.execute(eq(activityId), any(), any()))
                .thenReturn(PageResponse.<ReservationResponse>builder()
                        .items(List.of(item))
                        .page(0)
                        .size(20)
                        .totalItems(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/activities/{id}/reservations", activityId)
                        .header("Authorization", "Bearer " + tokenWithRoles(UUID.randomUUID(), List.of("CONGRESS_ADMIN", "PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(item.getId().toString()))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void shouldListUserReservationsWithPageEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        ReservationResponse item = reservationResponse(UUID.randomUUID(), userId);

        when(listUserReservationsUseCase.execute(eq(userId), any(), any()))
                .thenReturn(PageResponse.<ReservationResponse>builder()
                        .items(List.of(item))
                        .page(0)
                        .size(20)
                        .totalItems(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/users/{id}/reservations", userId)
                        .header("Authorization", "Bearer " + tokenWithRoles(userId, List.of("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].userId").value(userId.toString()));
    }

    @Test
    void shouldReturnJsonOnCancelReservationSuccess() throws Exception {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doNothing().when(cancelReservationUseCase).execute(eq(reservationId), any());

        mockMvc.perform(delete("/reservations/{id}", reservationId)
                        .header("Authorization", "Bearer " + tokenWithRoles(userId, List.of("PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation cancelled"));
    }

    @Test
    void shouldReturnForbiddenWhenCancelByNonOwner() throws Exception {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", "cannot cancel another user's reservation"))
                .when(cancelReservationUseCase).execute(eq(reservationId), any());

        mockMvc.perform(delete("/reservations/{id}", reservationId)
                        .header("Authorization", "Bearer " + tokenWithRoles(userId, List.of("PARTICIPANT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void shouldReturnBadRequestForInvalidUuidPath() throws Exception {
        mockMvc.perform(get("/activities/not-a-uuid/reservations")
                        .header("Authorization", "Bearer " + tokenWithRoles(UUID.randomUUID(), List.of("CONGRESS_ADMIN", "PARTICIPANT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    private ReservationResponse reservationResponse(UUID activityId, UUID userId) {
        return ReservationResponse.builder()
                .id(UUID.randomUUID())
                .activityId(activityId)
                .userId(userId)
                .reservedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
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
