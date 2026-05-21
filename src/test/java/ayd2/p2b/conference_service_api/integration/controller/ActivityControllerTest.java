package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.activity.application.create.CreateActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.delete.DeleteActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.get.GetActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.list.ListActivitiesUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.update.UpdateActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.domain.exception.ActivityDomainException;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "security.jwt.secret=test_secret_key_with_at_least_32_chars"
})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
class ActivityControllerTest {

    private static final String SECRET = "test_secret_key_with_at_least_32_chars";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateActivityUseCase createActivityUseCase;
    @MockitoBean
    private ListActivitiesUseCase listActivitiesUseCase;
    @MockitoBean
    private GetActivityUseCase getActivityUseCase;
    @MockitoBean
    private UpdateActivityUseCase updateActivityUseCase;
    @MockitoBean
    private DeleteActivityUseCase deleteActivityUseCase;

    @Test
    void shouldAllowPublicGetActivitiesWithoutToken() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        when(listActivitiesUseCase.execute(eq(congressId), any(), any())).thenReturn(PageResponse.<ActivityResponse>builder()
                .items(List.of(sampleResponse(congressId, List.of(leaderId))))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .build());

        mockMvc.perform(get("/congresses/{id}/activities", congressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Taller"))
                .andExpect(jsonPath("$.data.items[0].leaders").isArray())
                .andExpect(jsonPath("$.data.items[0].leaders[0]").value(leaderId.toString()))
                .andExpect(jsonPath("$.data.items[0].leaderType").doesNotExist());
    }

    @Test
    void shouldAllowPublicGetActivityByIdWithoutToken() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        ActivityResponse response = sampleResponse(congressId, List.of(leaderId));
        when(getActivityUseCase.execute(response.getId())).thenReturn(response);

        mockMvc.perform(get("/activities/{id}", response.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.leaders").isArray())
                .andExpect(jsonPath("$.data.leaders[0]").value(leaderId.toString()))
                .andExpect(jsonPath("$.data.leaderType").doesNotExist());
    }

    @Test
    void shouldRequireAuthenticationForWriteEndpoints() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        mockMvc.perform(post("/congresses/{id}/activities", congressId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(List.of())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/activities/{id}", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nuevo\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/activities/{id}", activityId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectNonCongressAdminForWrites() throws Exception {
        UUID congressId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        String participantToken = tokenWithRoles(List.of("PARTICIPANT"));
        String systemAdminToken = tokenWithRoles(List.of("SYSTEM_ADMIN", "PARTICIPANT"));

        mockMvc.perform(post("/congresses/{id}/activities", congressId)
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(List.of())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/congresses/{id}/activities", congressId)
                        .header("Authorization", "Bearer " + systemAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(List.of())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/activities/{id}", activityId)
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nuevo\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/activities/{id}", activityId)
                        .header("Authorization", "Bearer " + systemAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnValidationFailedForInvalidBody() throws Exception {
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        UUID congressId = UUID.randomUUID();

        mockMvc.perform(post("/congresses/{id}/activities", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"\",\"type\":\"TALLER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    @Test
    void shouldReachWriteUseCasesWithCongressAdminToken() throws Exception {
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        UUID congressId = UUID.randomUUID();
        UUID leaderId = UUID.randomUUID();
        ActivityResponse baseResponse = sampleResponse(congressId, List.of());
        ActivityResponse response = ActivityResponse.builder()
                .id(baseResponse.getId())
                .congressId(baseResponse.getCongressId())
                .roomId(baseResponse.getRoomId())
                .name(baseResponse.getName())
                .description(baseResponse.getDescription())
                .type(baseResponse.getType())
                .startTime(baseResponse.getStartTime())
                .endTime(baseResponse.getEndTime())
                .leaders(List.of(leaderId))
                .workshopCapacity(baseResponse.getWorkshopCapacity())
                .createdAt(baseResponse.getCreatedAt())
                .updatedAt(baseResponse.getUpdatedAt())
                .build();

        when(createActivityUseCase.execute(eq(congressId), any(), any())).thenReturn(response);
        when(updateActivityUseCase.execute(eq(response.getId()), any(), any())).thenReturn(response);
        when(deleteActivityUseCase.execute(eq(response.getId()), any())).thenReturn(response);

        mockMvc.perform(post("/congresses/{id}/activities", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(List.of(leaderId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.leaders").isArray())
                .andExpect(jsonPath("$.data.leaders[0]").value(leaderId.toString()))
                .andExpect(jsonPath("$.data.leaderType").doesNotExist())
                .andExpect(jsonPath("$.message").value("Activity created"));

        mockMvc.perform(put("/activities/{id}", response.getId())
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nuevo nombre\",\"leaders\":[\"" + leaderId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.leaders").isArray())
                .andExpect(jsonPath("$.data.leaders[0]").value(leaderId.toString()))
                .andExpect(jsonPath("$.data.leaderType").doesNotExist())
                .andExpect(jsonPath("$.message").value("Activity updated"));

        mockMvc.perform(delete("/activities/{id}", response.getId())
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.leaders").isArray())
                .andExpect(jsonPath("$.data.leaders[0]").value(leaderId.toString()))
                .andExpect(jsonPath("$.data.leaderType").doesNotExist())
                .andExpect(jsonPath("$.message").value("Activity deleted"));
    }

    @Test
    void shouldReturnDomainInvariantViolationWhenUseCaseThrowsDomainException() throws Exception {
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        UUID congressId = UUID.randomUUID();

        when(createActivityUseCase.execute(eq(congressId), any(), any()))
                .thenThrow(new ActivityDomainException("startTime must be before endTime"));

        mockMvc.perform(post("/congresses/{id}/activities", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(List.of())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("domain.invariant_violated"));
    }

    @Test
    void shouldReturnConflictWhenDeleteHasDependencies() throws Exception {
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        UUID activityId = UUID.randomUUID();
        when(deleteActivityUseCase.execute(eq(activityId), any()))
                .thenThrow(new ayd2.p2b.conference_service_api.common.exception.ApiException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "resource.conflict",
                        "dependency conflict"
                ));

        mockMvc.perform(delete("/activities/{id}", activityId)
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("resource.conflict"));
    }

    @Test
    void shouldReturnValidationFailedForDuplicateLeaders() throws Exception {
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        UUID congressId = UUID.randomUUID();
        UUID duplicatedLeader = UUID.randomUUID();
        when(createActivityUseCase.execute(eq(congressId), any(), any()))
                .thenThrow(new ayd2.p2b.conference_service_api.common.exception.ApiException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "validation.failed",
                        "leaders cannot contain duplicates"
                ));

        mockMvc.perform(post("/congresses/{id}/activities", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(List.of(duplicatedLeader, duplicatedLeader))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenCreateUseCaseReportsIamUnavailable() throws Exception {
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        UUID congressId = UUID.randomUUID();
        when(createActivityUseCase.execute(eq(congressId), any(), any()))
                .thenThrow(new ayd2.p2b.conference_service_api.common.exception.ApiException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "integration.iam_unavailable",
                        "IAM service unavailable"
                ));

        mockMvc.perform(post("/congresses/{id}/activities", congressId)
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(List.of(UUID.randomUUID()))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("integration.iam_unavailable"));
    }

    @Test
    void shouldNormalizePagination() throws Exception {
        UUID congressId = UUID.randomUUID();
        when(listActivitiesUseCase.execute(eq(congressId), any(), any())).thenReturn(PageResponse.<ActivityResponse>builder()
                .items(List.of())
                .page(0)
                .size(20)
                .totalItems(0)
                .totalPages(0)
                .build());

        mockMvc.perform(get("/congresses/{id}/activities?page=-1&size=0", congressId))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        mockMvc.perform(get("/congresses/{id}/activities?page=2&size=999", congressId))
                .andExpect(status().isOk());

        verify(listActivitiesUseCase, times(2)).execute(eq(congressId), any(), captor.capture());
        assertThat(captor.getAllValues().getFirst().getPageNumber()).isEqualTo(0);
        assertThat(captor.getAllValues().getFirst().getPageSize()).isEqualTo(20);
        assertThat(captor.getAllValues().get(1).getPageNumber()).isEqualTo(2);
        assertThat(captor.getAllValues().get(1).getPageSize()).isEqualTo(100);
    }

    @Test
    void shouldReturnValidationFailedForInvalidDateRangeFilter() throws Exception {
        UUID congressId = UUID.randomUUID();

        mockMvc.perform(get("/congresses/{id}/activities?dateFrom=2026-10-10T12:00:00Z&dateTo=2026-10-10T10:00:00Z", congressId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    private ActivityResponse sampleResponse(UUID congressId, List<UUID> leaders) {
        return ActivityResponse.builder()
                .id(UUID.randomUUID())
                .congressId(congressId)
                .roomId(UUID.randomUUID())
                .name("Taller")
                .description("Desc")
                .type(ActivityType.TALLER)
                .startTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .endTime(OffsetDateTime.parse("2026-10-10T11:00:00Z"))
                .leaders(leaders)
                .workshopCapacity(30)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private String createPayload(List<UUID> leaders) {
        String leadersJson = leaders.stream()
                .map(uuid -> "\"" + uuid + "\"")
                .reduce((left, right) -> left + "," + right)
                .map(value -> "[" + value + "]")
                .orElse("[]");
        return """
                {
                  "name":"Taller",
                  "description":"Descripcion",
                  "roomId":"8ad4c2c6-c4cd-4b4b-b794-2a146290f636",
                  "type":"TALLER",
                  "startTime":"2026-10-10T10:00:00Z",
                  "endTime":"2026-10-10T11:00:00Z",
                  "workshopCapacity":30,
                  "leaders":%s
                }
                """.formatted(leadersJson);
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
