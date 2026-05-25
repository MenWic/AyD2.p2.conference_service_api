package ayd2.p2b.conference_service_api.integration.client;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.dto.IamCommitteeCandidateSummary;
import ayd2.p2b.conference_service_api.integration.dto.IamCommitteeEligibilityResponse;
import ayd2.p2b.conference_service_api.integration.dto.IamInternalUserIdentityResponse;
import ayd2.p2b.conference_service_api.integration.dto.IamPersonalIdUserSummary;
import ayd2.p2b.conference_service_api.integration.dto.IamUserDetailSummary;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;

@Component
public class IamUserLookupClient implements IamUserLookupPort {

    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    private final RestClient restClient;
    private final String serviceToken;

    public IamUserLookupClient(
            RestClient.Builder restClientBuilder,
            @Value("${integration.iam.base-url:http://localhost:8081}") String iamBaseUrl,
            @Value("${integration.iam.service-token:}") String serviceToken
    ) {
        System.out.println("IAM Base URL: " + iamBaseUrl);
        this.restClient = restClientBuilder.baseUrl(iamBaseUrl).build();
        this.serviceToken = serviceToken;
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
    public IamCommitteeCandidateSummary getCommitteeCandidateSummary(UUID userId, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw IntegrationExceptions.iamUnavailable("Bearer token is required for IAM committee validation");
        }

        try {
            ApiResponse<IamCommitteeEligibilityResponse> response = fetchCommitteeEligibility(userId, accessToken);
            if (response == null || response.getData() == null || response.getData().getEligible() == null) {
                throw IntegrationExceptions.iamUnavailable();
            }
            IamCommitteeEligibilityResponse data = response.getData();
            boolean eligible = Boolean.TRUE.equals(data.getEligible());
            if (!eligible) {
                return IamCommitteeCandidateSummary.builder()
                        .eligible(false)
                        .userId(userId)
                        .build();
            }

            if (data.getUserId() == null
                    || !userId.equals(data.getUserId())
                    || data.getFullName() == null
                    || data.getFullName().isBlank()
                    || data.getEmail() == null
                    || data.getEmail().isBlank()) {
                throw IntegrationExceptions.iamUnavailable();
            }

            return IamCommitteeCandidateSummary.builder()
                    .eligible(true)
                    .userId(data.getUserId())
                    .fullName(data.getFullName())
                    .email(data.getEmail())
                    .build();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 403 || status == 404) {
                throw IntegrationExceptions.iamUnavailable();
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
            throw IntegrationExceptions.iamUnavailable("Bearer token is required for IAM user summary lookup");
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
                        .fullName(data.getFullName())
                        .email(data.getEmail())
                        .active(Boolean.TRUE.equals(data.getActive()))
                        .roles(data.getRoles() == null ? Set.of() : Set.copyOf(data.getRoles()))
                        .linkedInstitutions(data.getLinkedInstitutions() == null ? Set.of() : Set.copyOf(data.getLinkedInstitutions()))
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

    @Override
    public Optional<IamPersonalIdUserSummary> findUserByPersonalId(String personalId) {
        String normalizedPersonalId = normalizePersonalId(personalId);
        if (serviceToken == null || serviceToken.isBlank()) {
            throw IntegrationExceptions.iamUnavailable("IAM service token is not configured");
        }

        try {
            ApiResponse<IamInternalUserIdentityResponse> response = fetchUserByPersonalIdInternal(normalizedPersonalId);
            if (response == null || response.getData() == null) {
                throw IntegrationExceptions.iamUnavailable();
            }
            IamInternalUserIdentityResponse data = response.getData();
            if (data.getId() == null || data.getPersonalId() == null || data.getPersonalId().trim().isBlank()) {
                throw IntegrationExceptions.iamUnavailable();
            }
            return Optional.of(IamPersonalIdUserSummary.builder()
                    .userId(data.getId())
                    .personalId(data.getPersonalId().trim())
                    .build());
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 404) {
                return Optional.empty();
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
    public Map<UUID, IamUserDetailSummary> getUserDetailsSummary(Set<UUID> userIds, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw IntegrationExceptions.iamUnavailable("Bearer token is required for IAM user detail lookup");
        }
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, IamUserDetailSummary> summaries = new LinkedHashMap<>();
        for (UUID userId : userIds.stream().sorted(Comparator.comparing(UUID::toString)).toList()) {
            if (userId == null) {
                continue;
            }
            try {
                ApiResponse<IamUserResponse> response = fetchUserById(userId, accessToken);
                IamUserResponse data = extractUserDataOrThrow(response);
                summaries.put(userId, IamUserDetailSummary.builder()
                        .id(data.getId())
                        .fullName(data.getFullName())
                        .email(data.getEmail())
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

    private ApiResponse<IamCommitteeEligibilityResponse> fetchCommitteeEligibility(UUID userId, String accessToken) {
        return restClient.get()
                .uri("/users/{id}/can-be-committee", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private ApiResponse<IamInternalUserIdentityResponse> fetchUserByPersonalIdInternal(String personalId) {
        return restClient.get()
                .uri("/internal/users/by-personal-id/{personalId}", personalId)
                .header(SERVICE_TOKEN_HEADER, serviceToken)
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

    private String normalizePersonalId(String personalId) {
        if (personalId == null || personalId.trim().isBlank()) {
            throw IntegrationExceptions.iamUnavailable("personalId is required for IAM lookup");
        }
        return personalId.trim();
    }
}
