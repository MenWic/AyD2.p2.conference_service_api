package ayd2.p2b.conference_service_api.integration.client;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.integration.dto.WalletEarningsByCongressReportResponse;
import ayd2.p2b.conference_service_api.integration.dto.WalletPlatformEarningsReportResponse;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.WalletReportPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class WalletReportClient implements WalletReportPort {

    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    private final RestClient restClient;
    private final String serviceToken;
    private final ObjectMapper objectMapper;

    public WalletReportClient(
            RestClient.Builder restClientBuilder,
            @Value("${integration.wallet.base-url:http://localhost:8083}") String walletBaseUrl,
            @Value("${integration.wallet.service-token:}") String serviceToken
    ) {
        this.restClient = restClientBuilder.baseUrl(walletBaseUrl).build();
        this.serviceToken = serviceToken;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public WalletEarningsByCongressReportResponse getEarningsByCongress(
            UUID congressId,
            UUID institutionId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        ensureServiceTokenConfigured();

        try {
            ApiResponse<WalletEarningsByCongressReportResponse> response = restClient.get()
                    .uri(uriBuilder -> buildEarningsByCongressUri(uriBuilder, congressId, institutionId, dateFrom, dateTo))
                    .header(SERVICE_TOKEN_HEADER, serviceToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return extractDataOrThrow(response);
        } catch (RestClientResponseException ex) {
            throw mapWalletResponseException(ex);
        } catch (RestClientException ex) {
            throw IntegrationExceptions.walletUnavailable();
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw IntegrationExceptions.walletUnavailable();
        }
    }

    @Override
    public WalletPlatformEarningsReportResponse getPlatformEarnings(
            UUID institutionId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        ensureServiceTokenConfigured();

        try {
            ApiResponse<WalletPlatformEarningsReportResponse> response = restClient.get()
                    .uri(uriBuilder -> buildPlatformEarningsUri(uriBuilder, institutionId, dateFrom, dateTo))
                    .header(SERVICE_TOKEN_HEADER, serviceToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return extractDataOrThrow(response);
        } catch (RestClientResponseException ex) {
            throw mapWalletResponseException(ex);
        } catch (RestClientException ex) {
            throw IntegrationExceptions.walletUnavailable();
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw IntegrationExceptions.walletUnavailable();
        }
    }

    private void ensureServiceTokenConfigured() {
        if (serviceToken == null || serviceToken.isBlank()) {
            throw IntegrationExceptions.walletUnavailable("Wallet service token is not configured");
        }
    }

    private ApiException mapWalletResponseException(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status == HttpStatus.BAD_REQUEST.value()) {
            String code = extractErrorCode(ex);
            if ("validation.failed".equals(code)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "validation.failed",
                        extractErrorDetail(ex, "Validation failed")
                );
            }
            throw IntegrationExceptions.walletUnavailable("Wallet service returned an unsupported 400 response");
        }
        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value() || status >= 500) {
            throw IntegrationExceptions.walletUnavailable("Conference service cannot authenticate with wallet service");
        }
        throw IntegrationExceptions.walletUnavailable();
    }

    private <T> T extractDataOrThrow(ApiResponse<T> response) {
        if (response == null || response.getData() == null) {
            throw IntegrationExceptions.walletUnavailable("Wallet service returned an invalid response body");
        }
        return response.getData();
    }

    private URI buildEarningsByCongressUri(
            UriBuilder uriBuilder,
            UUID congressId,
            UUID institutionId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        uriBuilder.path("/internal/reports/earnings-by-congress");
        if (congressId != null) {
            uriBuilder.queryParam("congressId", congressId);
        }
        if (institutionId != null) {
            uriBuilder.queryParam("institutionId", institutionId);
        }
        if (dateFrom != null) {
            uriBuilder.queryParam("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            uriBuilder.queryParam("dateTo", dateTo);
        }
        return uriBuilder.build();
    }

    private URI buildPlatformEarningsUri(
            UriBuilder uriBuilder,
            UUID institutionId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        uriBuilder.path("/internal/reports/earnings");
        if (institutionId != null) {
            uriBuilder.queryParam("institutionId", institutionId);
        }
        if (dateFrom != null) {
            uriBuilder.queryParam("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            uriBuilder.queryParam("dateTo", dateTo);
        }
        return uriBuilder.build();
    }

    private String extractErrorCode(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode code = root.get("code");
            if (code == null || code.isNull()) {
                return null;
            }
            String value = code.asText();
            return value == null || value.isBlank() ? null : value;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractErrorDetail(RestClientResponseException ex, String fallback) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return fallback;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode detail = root.get("detail");
            if (detail == null || detail.isNull()) {
                return fallback;
            }
            String value = detail.asText();
            return value == null || value.isBlank() ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
