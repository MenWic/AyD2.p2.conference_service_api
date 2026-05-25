package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.attendance.application.list.ListAttendanceUseCase;
import ayd2.p2b.conference_service_api.feature.attendance.application.register.RegisterAttendanceUseCase;
import ayd2.p2b.conference_service_api.feature.attendance.application.user.GetUserAttendanceUseCase;
import ayd2.p2b.conference_service_api.feature.attendance.dto.response.AttendanceResponse;
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
import static org.mockito.Mockito.when;
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
class AttendanceControllerTest {

    private static final String SECRET = "test_secret_key_with_at_least_32_chars";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterAttendanceUseCase registerAttendanceUseCase;
    @MockitoBean
    private ListAttendanceUseCase listAttendanceUseCase;
    @MockitoBean
    private GetUserAttendanceUseCase getUserAttendanceUseCase;

    @Test
    void shouldRequireAuthenticationForAttendanceEndpoints() throws Exception {
        mockMvc.perform(post("/attendance/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityId":"%s",
                                  "personalId":"ABC12345"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/attendance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRegisterAttendanceAndReturnApiResponse() throws Exception {
        UUID adminId = UUID.randomUUID();
        AttendanceResponse response = attendanceResponse();

        when(registerAttendanceUseCase.execute(any(), any())).thenReturn(response);

        mockMvc.perform(post("/attendance/register")
                        .header("Authorization", "Bearer " + tokenWithRoles(adminId, List.of("CONGRESS_ADMIN", "PARTICIPANT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityId":"%s",
                                  "personalId":"ABC12345"
                                }
                                """.formatted(response.getActivityId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.activityId").value(response.getActivityId().toString()))
                .andExpect(jsonPath("$.data.personalId").value(response.getPersonalId()))
                .andExpect(jsonPath("$.message").value("Attendance registered"));
    }

    @Test
    void shouldListAttendanceWithPageEnvelope() throws Exception {
        AttendanceResponse item = attendanceResponse();

        when(listAttendanceUseCase.execute(any(), any(), any()))
                .thenReturn(PageResponse.<AttendanceResponse>builder()
                        .items(List.of(item))
                        .page(0)
                        .size(20)
                        .totalItems(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/attendance")
                        .header("Authorization", "Bearer " + tokenWithRoles(UUID.randomUUID(), List.of("CONGRESS_ADMIN", "PARTICIPANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(item.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].personalId").value(item.getPersonalId()))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void shouldReturnForbiddenWhenRequesterIsNotScopedCongressAdmin() throws Exception {
        when(listAttendanceUseCase.execute(any(), any(), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", "not scoped"));

        mockMvc.perform(get("/attendance")
                        .header("Authorization", "Bearer " + tokenWithRoles(UUID.randomUUID(), List.of("CONGRESS_ADMIN", "PARTICIPANT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void shouldReturnForbiddenWhenRegisterRequesterIsNotScopedCongressAdmin() throws Exception {
        when(registerAttendanceUseCase.execute(any(), any()))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", "not scoped"));

        mockMvc.perform(post("/attendance/register")
                        .header("Authorization", "Bearer " + tokenWithRoles(UUID.randomUUID(), List.of("CONGRESS_ADMIN", "PARTICIPANT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityId":"%s",
                                  "personalId":"ABC12345"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void shouldReturnBadRequestForMalformedRegisterBody() throws Exception {
        mockMvc.perform(post("/attendance/register")
                        .header("Authorization", "Bearer " + tokenWithRoles(UUID.randomUUID(), List.of("CONGRESS_ADMIN", "PARTICIPANT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activityId":"not-a-uuid",
                                  "personalId":"ABC12345"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    @Test
    void shouldReturnBadRequestForInvalidUuidQueryParam() throws Exception {
        mockMvc.perform(get("/attendance?activityId=not-a-uuid")
                        .header("Authorization", "Bearer " + tokenWithRoles(UUID.randomUUID(), List.of("CONGRESS_ADMIN", "PARTICIPANT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    @Test
    void shouldReturnBadRequestWhenDateRangeIsInvalid() throws Exception {
        mockMvc.perform(get("/attendance")
                        .queryParam("dateFrom", "2026-10-20")
                        .queryParam("dateTo", "2026-10-10")
                        .header("Authorization", "Bearer " + tokenWithRoles(UUID.randomUUID(), List.of("CONGRESS_ADMIN", "PARTICIPANT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    private AttendanceResponse attendanceResponse() {
        return AttendanceResponse.builder()
                .id(UUID.randomUUID())
                .activityId(UUID.randomUUID())
                .personalId("ABC12345")
                .registeredBy(UUID.randomUUID())
                .registeredAt(OffsetDateTime.parse("2026-10-10T11:00:00Z"))
                .build();
    }

    private String tokenWithRoles(UUID userId, List<String> roles) {
        return Jwts.builder()
                .claim("tokenType", "ACCESS")
                .claim("userId", userId.toString())
                .claim("email", "admin@example.com")
                .claim("roles", roles)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
