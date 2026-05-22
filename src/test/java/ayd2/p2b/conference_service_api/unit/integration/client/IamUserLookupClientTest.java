package ayd2.p2b.conference_service_api.unit.integration.client;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.integration.client.IamUserLookupClient;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IamUserLookupClientTest {

    private static final String BASE_URL = "http://iam.local";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private IamUserLookupClient client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new IamUserLookupClient(restClientBuilder, BASE_URL);
    }

    @Test
    void shouldReturnTrueWhenInstitutionIsLinked() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess(
                        """
                        {
                          "data": {
                            "id": "%s",
                            "linkedInstitutions": ["%s"]
                          }
                        }
                        """.formatted(userId, institutionId),
                        MediaType.APPLICATION_JSON
                ));

        boolean linked = client.isCongressAdminLinkedToInstitution(userId, institutionId, "token");

        assertThat(linked).isTrue();
        server.verify();
    }

    @Test
    void shouldReturnFalseWhenInstitutionIsNotLinked() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID otherInstitution = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId))
                .andRespond(withSuccess(
                        """
                        {
                          "data": {
                            "id": "%s",
                            "linkedInstitutions": ["%s"]
                          }
                        }
                        """.formatted(userId, otherInstitution),
                        MediaType.APPLICATION_JSON
                ));

        boolean linked = client.isCongressAdminLinkedToInstitution(userId, institutionId, "token");

        assertThat(linked).isFalse();
        server.verify();
    }

    @Test
    void shouldReturnFalseWhenLinkedInstitutionsIsNull() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId))
                .andRespond(withSuccess(
                        """
                        {
                          "data": {
                            "id": "%s",
                            "linkedInstitutions": null
                          }
                        }
                        """.formatted(userId),
                        MediaType.APPLICATION_JSON
                ));

        boolean linked = client.isCongressAdminLinkedToInstitution(userId, institutionId, "token");

        assertThat(linked).isFalse();
        server.verify();
    }

    @Test
    void shouldReturnFalseWhenIamReturnsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        boolean linked = client.isCongressAdminLinkedToInstitution(userId, institutionId, "token");

        assertThat(linked).isFalse();
        server.verify();
    }

    @Test
    void shouldThrowIamUnavailableWhenDataWrapperIsMissing() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId))
                .andRespond(withSuccess("{\"message\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.isCongressAdminLinkedToInstitution(userId, institutionId, "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    @Test
    void shouldThrowIamUnavailableOnTechnicalFailure() {
        UUID userId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.isCongressAdminLinkedToInstitution(userId, institutionId, "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    @Test
    void shouldReturnFalseWhenBearerTokenIsMissing() {
        boolean linked = client.isCongressAdminLinkedToInstitution(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "   "
        );

        assertThat(linked).isFalse();
    }

    @Test
    void shouldReturnUserSummaryMapWhenUsersExist() {
        UUID userId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess(
                        """
                        {
                          "data": {
                            "id": "%s",
                            "fullName": "Ada Lovelace",
                            "email": "ada@example.com",
                            "active": true,
                            "roles": ["PARTICIPANT", "GUEST_SPEAKER"],
                            "linkedInstitutions": []
                          }
                        }
                        """.formatted(userId),
                        MediaType.APPLICATION_JSON
                ));

        Map<UUID, IamUserSummary> summaries =
                client.getUsersSummary(Set.of(userId), "token");

        assertThat(summaries).containsKey(userId);
        assertThat(summaries.get(userId).isActive()).isTrue();
        assertThat(summaries.get(userId).getFullName()).isEqualTo("Ada Lovelace");
        assertThat(summaries.get(userId).getEmail()).isEqualTo("ada@example.com");
        assertThat(summaries.get(userId).getRoles()).contains("PARTICIPANT", "GUEST_SPEAKER");
        server.verify();
    }

    @Test
    void shouldSkipLeaderWhenIamReturnsForbiddenOrNotFound() {
        UUID forbiddenUser = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID foundUser = UUID.fromString("00000000-0000-0000-0000-000000000002");

        server.expect(requestTo(BASE_URL + "/users/" + forbiddenUser))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        server.expect(requestTo(BASE_URL + "/users/" + foundUser))
                .andRespond(withSuccess(
                        """
                        {
                          "data": {
                            "id": "%s",
                            "fullName": "Grace Hopper",
                            "email": "grace@example.com",
                            "active": true,
                            "roles": ["PARTICIPANT"],
                            "linkedInstitutions": []
                          }
                        }
                        """.formatted(foundUser),
                        MediaType.APPLICATION_JSON
                ));

        Map<UUID, IamUserSummary> summaries =
                client.getUsersSummary(Set.of(forbiddenUser, foundUser), "token");

        assertThat(summaries).containsOnlyKeys(foundUser);
        server.verify();
    }

    @Test
    void shouldThrowIamUnavailableWhenSummaryWrapperIsInvalid() {
        UUID userId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId))
                .andRespond(withSuccess("{\"message\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getUsersSummary(Set.of(userId), "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    @Test
    void shouldThrowIamUnavailableForSummaryOnTechnicalFailure() {
        UUID userId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.getUsersSummary(Set.of(userId), "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    @Test
    void shouldReturnCommitteeCandidateSummaryWhenEligibleAndProfilePresent() {
        UUID userId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId + "/can-be-committee"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess(
                        """
                        {
                          "data": {
                            "eligible": true,
                            "userId": "%s",
                            "fullName": "Ada Lovelace",
                            "email": "ada@example.com"
                          }
                        }
                        """.formatted(userId),
                        MediaType.APPLICATION_JSON
                ));

        var summary = client.getCommitteeCandidateSummary(userId, "token");

        assertThat(summary.isEligible()).isTrue();
        assertThat(summary.getUserId()).isEqualTo(userId);
        assertThat(summary.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(summary.getEmail()).isEqualTo("ada@example.com");
        server.verify();
    }

    @Test
    void shouldReturnIneligibleCommitteeCandidateWithoutRequiringProfileFields() {
        UUID userId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId + "/can-be-committee"))
                .andRespond(withSuccess(
                        """
                        {
                          "data": {
                            "eligible": false
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        var summary = client.getCommitteeCandidateSummary(userId, "token");

        assertThat(summary.isEligible()).isFalse();
        assertThat(summary.getUserId()).isEqualTo(userId);
        assertThat(summary.getFullName()).isNull();
        assertThat(summary.getEmail()).isNull();
        server.verify();
    }

    @Test
    void shouldThrowIamUnavailableWhenCommitteeEligibilityWrapperIsInvalid() {
        UUID userId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId + "/can-be-committee"))
                .andRespond(withSuccess("{\"message\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getCommitteeCandidateSummary(userId, "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    @Test
    void shouldThrowIamUnavailableWhenEligibleCandidateProfileIsMissing() {
        UUID userId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId + "/can-be-committee"))
                .andRespond(withSuccess(
                        """
                        {
                          "data": {
                            "eligible": true,
                            "userId": "%s"
                          }
                        }
                        """.formatted(userId),
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.getCommitteeCandidateSummary(userId, "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    @Test
    void shouldThrowIamUnavailableWhenCommitteeEligibilityReturnsForbiddenOrNotFound() {
        UUID forbiddenUser = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID missingUser = UUID.fromString("00000000-0000-0000-0000-000000000011");

        server.expect(requestTo(BASE_URL + "/users/" + forbiddenUser + "/can-be-committee"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        server.expect(requestTo(BASE_URL + "/users/" + missingUser + "/can-be-committee"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getCommitteeCandidateSummary(forbiddenUser, "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });

        assertThatThrownBy(() -> client.getCommitteeCandidateSummary(missingUser, "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
        server.verify();
    }

    @Test
    void shouldThrowIamUnavailableWhenCommitteeEligibilityOnTechnicalFailure() {
        UUID userId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/users/" + userId + "/can-be-committee"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.getCommitteeCandidateSummary(userId, "token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }
}
