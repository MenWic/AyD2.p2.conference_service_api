package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.congress.application.create.CreateCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.delete.DeleteCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.get.GetCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.list.ListCongressesUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.update.UpdateCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class CongressControllerTest {

    private static final String SECRET = "test_secret_key_with_at_least_32_chars";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCongressUseCase createCongressUseCase;
    @MockitoBean
    private ListCongressesUseCase listCongressesUseCase;
    @MockitoBean
    private GetCongressUseCase getCongressUseCase;
    @MockitoBean
    private UpdateCongressUseCase updateCongressUseCase;
    @MockitoBean
    private DeleteCongressUseCase deleteCongressUseCase;

    @Test
    void shouldAllowPublicGetCongressesWithoutToken() throws Exception {
        when(listCongressesUseCase.execute(any(), any())).thenReturn(PageResponse.<CongressResponse>builder()
                .items(List.of(sampleResponse()))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .build());

        mockMvc.perform(get("/congresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].institutionName").value("USAC"));
    }

    @Test
    void shouldAllowPublicGetCongressByIdWithoutToken() throws Exception {
        CongressResponse response = sampleResponse();
        when(getCongressUseCase.execute(response.getId())).thenReturn(response);

        mockMvc.perform(get("/congresses/{id}", response.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.institutionName").value("USAC"));
    }

    @Test
    void shouldEnforceAuthorizationForWriteEndpoints() throws Exception {
        UUID congressId = UUID.randomUUID();
        String participantToken = tokenWithRoles(List.of("PARTICIPANT"));
        String systemAdminToken = tokenWithRoles(List.of("SYSTEM_ADMIN", "PARTICIPANT"));

        mockMvc.perform(post("/congresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/congresses")
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/congresses")
                        .header("Authorization", "Bearer " + systemAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/congresses/{id}", congressId)
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nuevo\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/congresses/{id}", congressId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateWithCongressAdminTokenAndApiResponseEnvelope() throws Exception {
        String congressAdminToken = tokenWithRoles(List.of("CONGRESS_ADMIN", "PARTICIPANT"));
        CongressResponse response = sampleResponse();

        when(createCongressUseCase.execute(any(), any())).thenReturn(response);
        when(updateCongressUseCase.execute(eq(response.getId()), any(), any())).thenReturn(response);
        when(deleteCongressUseCase.execute(eq(response.getId()), any())).thenReturn(response);

        mockMvc.perform(post("/congresses")
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.institutionName").value("USAC"))
                .andExpect(jsonPath("$.message").value("Congress created"));

        mockMvc.perform(put("/congresses/{id}", response.getId())
                        .header("Authorization", "Bearer " + congressAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Congreso Actualizado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.institutionName").value("USAC"));

        mockMvc.perform(delete("/congresses/{id}", response.getId())
                        .header("Authorization", "Bearer " + congressAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.institutionName").value("USAC"))
                .andExpect(jsonPath("$.message").value("Congress deleted"));
    }

    private CongressResponse sampleResponse() {
        UUID institutionId = UUID.randomUUID();
        return CongressResponse.builder()
                .id(UUID.randomUUID())
                .name("Congreso Nacional")
                .description("Descripcion")
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 12))
                .location("Guatemala")
                .price(new BigDecimal("50.00"))
                .institutionId(institutionId)
                .institutionName("USAC")
                .createdBy(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String createPayload() {
        return """
                {
                  "name":"Congreso Nacional",
                  "description":"Descripcion",
                  "startDate":"2026-08-10",
                  "endDate":"2026-08-12",
                  "location":"Guatemala",
                  "price":50.00,
                  "institutionId":"9f7093df-806f-4ca8-ae6e-046f3996f50d"
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
