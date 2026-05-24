package ayd2.p2b.conference_service_api.unit.integration.client;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.integration.client.WalletPaymentClient;
import ayd2.p2b.conference_service_api.integration.dto.WalletPaymentRegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WalletPaymentClientTest {

  private static final String BASE_URL = "http://wallet.local";
  private static final UUID PAYMENT_ID = UUID.randomUUID();

  private RestClient.Builder restClientBuilder;
  private MockRestServiceServer server;
  private WalletPaymentClient client;

  @BeforeEach
  void setUp() {
    restClientBuilder = RestClient.builder();
    server = MockRestServiceServer.bindTo(restClientBuilder).build();
    client = new WalletPaymentClient(restClientBuilder, BASE_URL, "svc-token");
  }

  @Test
  void returnsPaymentIdFor201Created() {
    server.expect(requestTo(BASE_URL + "/payments/register"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
        .andExpect(header("Idempotency-Key", "idem-1"))
        .andExpect(header("X-Service-Token", "svc-token"))
        .andRespond(withStatus(HttpStatus.CREATED)
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {"data":{"id":"%s"}}
                """.formatted(PAYMENT_ID)));

    UUID result = client.registerPayment(request(), "idem-1", "access-token");

    assertThat(result).isEqualTo(PAYMENT_ID);
    server.verify();
  }

  @Test
  void returnsPaymentIdFor200Replay() {
    server.expect(requestTo(BASE_URL + "/payments/register"))
        .andRespond(withSuccess("""
            {"message":"idempotency.replay","data":{"id":"%s"}}
            """.formatted(PAYMENT_ID), MediaType.APPLICATION_JSON));

    UUID result = client.registerPayment(request(), "idem-2", "access-token");

    assertThat(result).isEqualTo(PAYMENT_ID);
    server.verify();
  }

  @Test
  void maps409ToControlledConflict() {
    server.expect(requestTo(BASE_URL + "/payments/register"))
        .andRespond(withStatus(HttpStatus.CONFLICT));

    assertThatThrownBy(() -> client.registerPayment(request(), "idem-3", "access-token"))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
          assertThat(apiEx.getCode()).isEqualTo("resource.conflict");
        });
  }

  @Test
  void maps422InsufficientFunds() {
    server.expect(requestTo(BASE_URL + "/payments/register"))
        .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {"code":"wallet.insufficient_funds","message":"No funds"}
                """));

    assertThatThrownBy(() -> client.registerPayment(request(), "idem-4", "access-token"))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
          assertThat(apiEx.getCode()).isEqualTo("wallet.insufficient_funds");
        });
  }

  @Test
  void maps422UnknownCodeToIntegrationError() {
    server.expect(requestTo(BASE_URL + "/payments/register"))
        .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {"code":"wallet.unknown_error","message":"Unexpected"}
                """));

    assertThatThrownBy(() -> client.registerPayment(request(), "idem-5", "access-token"))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
          assertThat(apiEx.getCode()).isEqualTo("system.integration_error");
        });
  }

  @Test
  void maps401And403ToIntegrationError() {
    server.expect(requestTo(BASE_URL + "/payments/register"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
    server.expect(requestTo(BASE_URL + "/payments/register"))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));

    assertThatThrownBy(() -> client.registerPayment(request(), "idem-6", "access-token"))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("system.integration_error"));

    assertThatThrownBy(() -> client.registerPayment(request(), "idem-7", "access-token"))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("system.integration_error"));
  }

  @Test
  void maps5xxToIntegrationError() {
    server.expect(requestTo(BASE_URL + "/payments/register"))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    assertThatThrownBy(() -> client.registerPayment(request(), "idem-8", "access-token"))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
          assertThat(apiEx.getCode()).isEqualTo("system.integration_error");
        });
  }

  @Test
  void mapsMissingDataIdToIntegrationError() {
    server.expect(requestTo(BASE_URL + "/payments/register"))
        .andRespond(withSuccess("""
            {"data":{}}
            """, MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.registerPayment(request(), "idem-9", "access-token"))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("system.integration_error"));
  }

  @Test
  void blankServiceTokenFailsBeforeHttpCall() {
    WalletPaymentClient invalidClient = new WalletPaymentClient(restClientBuilder, BASE_URL, "   ");

    assertThatThrownBy(() -> invalidClient.registerPayment(request(), "idem-10", "access-token"))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
          assertThat(apiEx.getCode()).isEqualTo("system.integration_error");
        });
  }

  private WalletPaymentRegisterRequest request() {
    return WalletPaymentRegisterRequest.builder()
        .userId(UUID.randomUUID())
        .congressId(UUID.randomUUID())
        .institutionId(UUID.randomUUID())
        .congressNameSnapshot("Congress")
        .institutionNameSnapshot("Institution")
        .amount(new BigDecimal("100.00"))
        .paymentDate(LocalDate.of(2026, 6, 15))
        .build();
  }
}
