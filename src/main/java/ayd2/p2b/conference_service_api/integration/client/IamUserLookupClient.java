package ayd2.p2b.conference_service_api.integration.client;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.congress.application.exception.CongressExceptions;
import ayd2.p2b.conference_service_api.integration.dto.IamUserResponse;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class IamUserLookupClient implements IamUserLookupPort {

    private final RestClient restClient;

    public IamUserLookupClient(
            RestClient.Builder restClientBuilder,
            @Value("${integration.iam.base-url:http://localhost:8081}") String iamBaseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(iamBaseUrl).build();
    }

    @Override
    public boolean isCongressAdminLinkedToInstitution(UUID userId, UUID institutionId, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }

        try {
            ApiResponse<IamUserResponse> response = restClient.get()
                    .uri("/users/{id}", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.getData() == null) {
                throw CongressExceptions.iamUnavailable();
            }

            if (response.getData().getLinkedInstitutions() == null) {
                return false;
            }

            return response.getData().getLinkedInstitutions().contains(institutionId);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return false;
            }
            throw CongressExceptions.iamUnavailable();
        } catch (RestClientException ex) {
            throw CongressExceptions.iamUnavailable();
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw CongressExceptions.iamUnavailable();
        }
    }
}
