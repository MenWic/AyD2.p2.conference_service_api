package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.enrollment.application.list.ListCongressEnrollmentsUseCase;
import ayd2.p2b.conference_service_api.feature.enrollment.application.list.ListUserEnrollmentsUseCase;
import ayd2.p2b.conference_service_api.feature.enrollment.application.register.EnrollParticipantResult;
import ayd2.p2b.conference_service_api.feature.enrollment.application.register.EnrollParticipantUseCase;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.response.EnrollmentResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
class EnrollmentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private EnrollParticipantUseCase enrollParticipantUseCase;

  @MockitoBean
  private ListUserEnrollmentsUseCase listUserEnrollmentsUseCase;

  @MockitoBean
  private ListCongressEnrollmentsUseCase listCongressEnrollmentsUseCase;

  private static final String SECRET = "test_secret_key_with_at_least_32_chars";
  private static final UUID CONGRESS_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID ENROLLMENT_ID = UUID.randomUUID();
  private static final UUID PAYMENT_ID = UUID.randomUUID();

  @Test
  void enroll_missing_idempotency_key_returns_400() throws Exception {
    String token = buildToken(USER_ID, "PARTICIPANT");

    mockMvc.perform(post("/congresses/{id}/enrollments", CONGRESS_ID)
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"paymentDate\":\"2026-06-15\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation.failed"));
  }

  @Test
  void enroll_blank_idempotency_key_returns_400() throws Exception {
    String token = buildToken(USER_ID, "PARTICIPANT");

    mockMvc.perform(post("/congresses/{id}/enrollments", CONGRESS_ID)
        .header("Authorization", "Bearer " + token)
        .header("Idempotency-Key", "   ")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"paymentDate\":\"2026-06-15\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation.failed"));
  }

  @Test
  void enroll_too_long_idempotency_key_returns_400() throws Exception {
    String token = buildToken(USER_ID, "PARTICIPANT");
    String longKey = "k".repeat(121);

    mockMvc.perform(post("/congresses/{id}/enrollments", CONGRESS_ID)
            .header("Authorization", "Bearer " + token)
            .header("Idempotency-Key", longKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"paymentDate\":\"2026-06-15\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation.failed"));
  }

  @Test
  void enroll_without_token_returns_401() throws Exception {
    mockMvc.perform(post("/congresses/{id}/enrollments", CONGRESS_ID)
        .header("Idempotency-Key", "key-123")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"paymentDate\":\"2026-06-15\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void enroll_successful_returns_201() throws Exception {
    String token = buildToken(USER_ID, "PARTICIPANT");
    EnrollmentResponse response = sampleEnrollmentResponse();

    when(enrollParticipantUseCase.execute(any(), any(), anyString(), any()))
        .thenReturn(EnrollParticipantResult.builder()
            .enrollment(response)
            .replay(false)
            .build());

    mockMvc.perform(post("/congresses/{id}/enrollments", CONGRESS_ID)
        .header("Authorization", "Bearer " + token)
        .header("Idempotency-Key", "key-123")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"paymentDate\":\"2026-06-15\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.data.id").value(ENROLLMENT_ID.toString()));
  }

  @Test
  void enroll_replay_returns_200() throws Exception {
    String token = buildToken(USER_ID, "PARTICIPANT");
    EnrollmentResponse response = sampleEnrollmentResponse();

    when(enrollParticipantUseCase.execute(any(), any(), anyString(), any()))
        .thenReturn(EnrollParticipantResult.builder()
            .enrollment(response)
            .replay(true)
            .build());

    mockMvc.perform(post("/congresses/{id}/enrollments", CONGRESS_ID)
        .header("Authorization", "Bearer " + token)
        .header("Idempotency-Key", "key-123")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"paymentDate\":\"2026-06-15\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("idempotency.replay"))
        .andExpect(jsonPath("$.data.id").value(ENROLLMENT_ID.toString()));
  }

  @Test
  void list_user_enrollments_returns_200() throws Exception {
    String token = buildToken(USER_ID, "PARTICIPANT");
    PageResponse<EnrollmentResponse> page = PageResponse.<EnrollmentResponse>builder()
        .items(List.of(sampleEnrollmentResponse()))
        .page(0).size(20).totalItems(1).totalPages(1)
        .build();

    when(listUserEnrollmentsUseCase.execute(eq(USER_ID), any(), any())).thenReturn(page);

    mockMvc.perform(get("/users/{id}/enrollments", USER_ID)
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalItems").value(1));
  }

  @Test
  void list_congress_enrollments_without_congress_admin_returns_403() throws Exception {
    String token = buildToken(USER_ID, "PARTICIPANT");
    when(listCongressEnrollmentsUseCase.execute(any(), any(), any()))
        .thenThrow(new ayd2.p2b.conference_service_api.common.exception.ApiException(
            org.springframework.http.HttpStatus.FORBIDDEN, "auth.forbidden", "Only CONGRESS_ADMIN"));

    mockMvc.perform(get("/congresses/{id}/enrollments", CONGRESS_ID)
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("auth.forbidden"));
  }

  private EnrollmentResponse sampleEnrollmentResponse() {
    return EnrollmentResponse.builder()
        .id(ENROLLMENT_ID)
        .congressId(CONGRESS_ID)
        .userId(USER_ID)
        .paymentId(PAYMENT_ID)
        .enrolledAt(Instant.now())
        .paymentDate(LocalDate.of(2026, 6, 15))
        .build();
  }

  private String buildToken(UUID userId, String... roles) {
    var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .subject(userId.toString())
        .claim("userId", userId.toString())
        .claim("email", "test@example.com")
        .claim("roles", List.of(roles))
        .claim("tokenType", "ACCESS")
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plusSeconds(3600)))
        .signWith(key)
        .compact();
  }
}
