package ayd2.p2b.conference_service_api.unit.integration.client;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.integration.client.IamUserLookupClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

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
}
