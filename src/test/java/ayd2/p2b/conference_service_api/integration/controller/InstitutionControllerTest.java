package ayd2.p2b.conference_service_api.integration.controller;

import ayd2.p2b.conference_service_api.feature.institution.application.create.CreateInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.delete.DeleteInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.get.GetInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.list.ListInstitutionsUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.update.UpdateInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
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
class InstitutionControllerTest {

    private static final String SECRET = "test_secret_key_with_at_least_32_chars";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateInstitutionUseCase createInstitutionUseCase;
    @MockitoBean
    private ListInstitutionsUseCase listInstitutionsUseCase;
    @MockitoBean
    private GetInstitutionUseCase getInstitutionUseCase;
    @MockitoBean
    private UpdateInstitutionUseCase updateInstitutionUseCase;
    @MockitoBean
    private DeleteInstitutionUseCase deleteInstitutionUseCase;

    @Test
    void shouldAllowPublicGetInstitutionsWithoutToken() throws Exception {
        when(listInstitutionsUseCase.execute(any())).thenReturn(
                ayd2.p2b.conference_service_api.common.response.PageResponse.<InstitutionResponse>builder()
                        .items(List.of(sampleResponse()))
                        .page(0)
                        .size(20)
                        .totalItems(1)
                        .totalPages(1)
                        .build()
        );

        mockMvc.perform(get("/institutions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("USAC"))
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    void shouldAllowPublicGetInstitutionByIdWithoutToken() throws Exception {
        UUID institutionId = UUID.randomUUID();
        InstitutionResponse response = sampleResponse();
        response.setId(institutionId);
        when(getInstitutionUseCase.execute(institutionId)).thenReturn(response);

        mockMvc.perform(get("/institutions/{id}", institutionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(institutionId.toString()))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void shouldRequireSystemAdminForWriteEndpoints() throws Exception {
        String participantToken = tokenWithRoles(List.of("PARTICIPANT"));
        UUID institutionId = UUID.randomUUID();

        mockMvc.perform(post("/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"USAC\",\"description\":\"Public\",\"contactEmail\":\"admin@usac.edu.gt\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/institutions")
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"USAC\",\"description\":\"Public\",\"contactEmail\":\"admin@usac.edu.gt\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/institutions/{id}", institutionId)
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/institutions/{id}", institutionId)
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnApiResponseEnvelopeForCreateAndDelete() throws Exception {
        String adminToken = tokenWithRoles(List.of("SYSTEM_ADMIN", "PARTICIPANT"));
        InstitutionResponse response = sampleResponse();

        when(createInstitutionUseCase.execute(any(), any())).thenReturn(response);
        when(deleteInstitutionUseCase.execute(eq(response.getId()), any())).thenReturn(response);

        mockMvc.perform(post("/institutions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"USAC\",\"description\":\"Public\",\"contactEmail\":\"admin@usac.edu.gt\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.message").value("Institution created"));

        mockMvc.perform(delete("/institutions/{id}", response.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.message").value("Institution deactivated"));
    }

    private InstitutionResponse sampleResponse() {
        return InstitutionResponse.builder()
                .id(UUID.randomUUID())
                .name("USAC")
                .description("Public University")
                .contactEmail("admin@usac.edu.gt")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
