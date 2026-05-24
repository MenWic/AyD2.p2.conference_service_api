package ayd2.p2b.conference_service_api.integration.client;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.integration.dto.WalletPaymentRegisterRequest;
import ayd2.p2b.conference_service_api.integration.dto.WalletPaymentRegisterResponse;
import ayd2.p2b.conference_service_api.integration.exception.IntegrationExceptions;
import ayd2.p2b.conference_service_api.integration.port.WalletPaymentPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class WalletPaymentClient implements WalletPaymentPort {

  private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
  private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

  private final RestClient restClient;
  private final String serviceToken;

  public WalletPaymentClient(
      RestClient.Builder restClientBuilder,
      @Value("${integration.wallet.base-url:http://localhost:8083}") String walletBaseUrl,
      @Value("${integration.wallet.service-token:}") String serviceToken) {
    this.restClient = restClientBuilder.baseUrl(walletBaseUrl).build();
    this.serviceToken = serviceToken;
  }

  @Override
  public UUID registerPayment(WalletPaymentRegisterRequest request, String idempotencyKey, String accessToken) {
    try {
      ApiResponse<WalletPaymentRegisterResponse> response = restClient.post()
          .uri("/payments/register")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
          .header(SERVICE_TOKEN_HEADER, serviceToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(new ParameterizedTypeReference<>() {
          });

      if (response == null || response.getData() == null || response.getData().getId() == null) {
        throw IntegrationExceptions.walletUnavailable();
      }
      return response.getData().getId();

    } catch (RestClientResponseException ex) {
      int statusCode = ex.getStatusCode().value();
      if (statusCode == HttpStatus.CONFLICT.value()) {
        // Wallet returned 409 resource.conflict: same idempotency key was already used
        // with a different request body. Propagate as conflict — do NOT treat as
        // replay.
        // A true idempotency replay from wallet is 200 OK (handled above by the success
        // path).
        throw new ApiException(HttpStatus.CONFLICT, "resource.conflict",
            "Idempotency key already used with a different payment request in wallet service");
      }
      if (statusCode == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "wallet.insufficient_funds",
            "Insufficient funds to complete enrollment");
      }
      if (statusCode == HttpStatus.UNAUTHORIZED.value() || statusCode == HttpStatus.FORBIDDEN.value()) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "system.integration_error",
            "Conference service cannot authenticate with wallet service");
      }
      throw IntegrationExceptions.walletUnavailable();

    } catch (RestClientException ex) {
      throw IntegrationExceptions.walletUnavailable();
    } catch (ApiException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw IntegrationExceptions.walletUnavailable();
    }
  }
}
