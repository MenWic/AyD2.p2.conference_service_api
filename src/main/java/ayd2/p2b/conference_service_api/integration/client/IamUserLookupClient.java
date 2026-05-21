package ayd2.p2b.conference_service_api.integration.client;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.dto.IamUserResponse;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;

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
            ApiResponse<IamUserResponse> response = fetchUserById(userId, accessToken);
            if (response == null || response.getData() == null) {
                throw IntegrationExceptions.iamUnavailable();
            }

            if (response.getData().getLinkedInstitutions() == null) {
                return false;
            }

            return response.getData().getLinkedInstitutions().contains(institutionId);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return false;
            }
            throw IntegrationExceptions.iamUnavailable();
        } catch (RestClientException ex) {
            throw IntegrationExceptions.iamUnavailable();
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw IntegrationExceptions.iamUnavailable();
        }
    }

    @Override
    public Map<UUID, IamUserSummary> getUsersSummary(Set<UUID> userIds, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw IntegrationExceptions.iamUnavailable("Bearer token is required for IAM leader validation");
        }
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, IamUserSummary> summaries = new LinkedHashMap<>();
        for (UUID userId : userIds.stream().sorted(Comparator.comparing(UUID::toString)).toList()) {
            if (userId == null) {
                continue;
            }
            try {
                ApiResponse<IamUserResponse> response = fetchUserById(userId, accessToken);
                IamUserResponse data = extractUserDataOrThrow(response);
                summaries.put(userId, IamUserSummary.builder()
                        .id(data.getId())
                        .active(Boolean.TRUE.equals(data.getActive()))
                        .roles(data.getRoles() == null ? Set.of() : Set.copyOf(data.getRoles()))
                        .build());
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                if (status == 403 || status == 404) {
                    continue;
                }
                throw IntegrationExceptions.iamUnavailable();
            } catch (RestClientException ex) {
                throw IntegrationExceptions.iamUnavailable();
            } catch (ApiException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw IntegrationExceptions.iamUnavailable();
            }
        }

        return summaries;
    }

    private ApiResponse<IamUserResponse> fetchUserById(UUID userId, String accessToken) {
        return restClient.get()
                .uri("/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private IamUserResponse extractUserDataOrThrow(ApiResponse<IamUserResponse> response) {
        if (response == null || response.getData() == null || response.getData().getId() == null) {
            throw IntegrationExceptions.iamUnavailable();
        }
        return response.getData();
    }
}
