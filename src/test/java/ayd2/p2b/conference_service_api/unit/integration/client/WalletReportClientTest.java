package ayd2.p2b.conference_service_api.unit.integration.client;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.integration.client.WalletReportClient;
import ayd2.p2b.conference_service_api.integration.dto.WalletEarningsByCongressReportResponse;
import ayd2.p2b.conference_service_api.integration.dto.WalletPlatformEarningsReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WalletReportClientTest {

    private static final String BASE_URL = "http://wallet.local";
    private static final String SERVICE_TOKEN = "svc-token";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private WalletReportClient client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new WalletReportClient(restClientBuilder, BASE_URL, SERVICE_TOKEN);
    }

    @Test
    void earnings_by_congress_sends_service_token_without_authorization_and_non_null_query_params() {
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        LocalDate dateFrom = LocalDate.of(2026, 1, 1);

        server.expect(requestTo(BASE_URL + "/internal/reports/earnings-by-congress?congressId=" + congressId
                + "&institutionId=" + institutionId + "&dateFrom=" + dateFrom))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Service-Token", SERVICE_TOKEN))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess("""
                        {"data":{"items":[{"congressId":"%s","congressName":"AydConf 2026","institutionId":"%s","institutionName":"USAC","totalAmount":1200.00,"commissionAmount":120.00,"netAmount":1080.00,"paymentCount":24}],"totalItems":1,"grandTotalAmount":1200.00,"grandTotalCommission":120.00,"grandTotalNet":1080.00}}
                        """.formatted(congressId, institutionId), MediaType.APPLICATION_JSON));

        WalletEarningsByCongressReportResponse response = client.getEarningsByCongress(
                congressId, institutionId, dateFrom, null);

        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getCongressId()).isEqualTo(congressId);
        assertThat(response.getItems().get(0).getInstitutionId()).isEqualTo(institutionId);
        server.verify();
    }

    @Test
    void maps_successful_platform_earnings_response() {
        UUID institutionId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        LocalDate dateFrom = LocalDate.of(2026, 1, 1);
        LocalDate dateTo = LocalDate.of(2026, 12, 31);

        server.expect(requestTo(BASE_URL + "/internal/reports/earnings?institutionId=" + institutionId
                + "&dateFrom=" + dateFrom + "&dateTo=" + dateTo))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Service-Token", SERVICE_TOKEN))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess("""
                        {"data":{"items":[{"institutionId":"%s","institutionName":"USAC","congresses":[{"congressId":"%s","congressName":"AydConf 2026","totalAmount":1200.00,"commissionAmount":120.00,"netAmount":1080.00,"paymentCount":24}],"institutionTotalAmount":1200.00,"institutionTotalCommission":120.00,"institutionTotalNet":1080.00,"paymentCount":24}],"totalItems":1,"grandTotalAmount":1200.00,"grandTotalCommission":120.00,"grandTotalNet":1080.00}}
                        """.formatted(institutionId, congressId), MediaType.APPLICATION_JSON));

        WalletPlatformEarningsReportResponse response = client.getPlatformEarnings(institutionId, dateFrom, dateTo);

        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getInstitutionId()).isEqualTo(institutionId);
        assertThat(response.getItems().get(0).getCongresses()).hasSize(1);
        assertThat(response.getItems().get(0).getCongresses().get(0).getCongressId()).isEqualTo(congressId);
        server.verify();
    }

    @Test
    void blank_service_token_fails_before_http() {
        WalletReportClient invalidClient = new WalletReportClient(restClientBuilder, BASE_URL, "   ");

        assertThatThrownBy(() -> invalidClient.getPlatformEarnings(null, null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiEx.getCode()).isEqualTo("system.integration_error");
                });
    }

    @Test
    void null_body_or_null_data_maps_to_503() {
        server.expect(requestTo(BASE_URL + "/internal/reports/earnings"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/internal/reports/earnings"))
                .andRespond(withSuccess("{\"data\":null}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getPlatformEarnings(null, null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiEx.getCode()).isEqualTo("system.integration_error");
                });

        assertThatThrownBy(() -> client.getPlatformEarnings(null, null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiEx.getCode()).isEqualTo("system.integration_error");
                });
    }

    @Test
    void maps_401_403_and_5xx_to_503_integration_error() {
        server.expect(requestTo(BASE_URL + "/internal/reports/earnings"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(BASE_URL + "/internal/reports/earnings"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        server.expect(requestTo(BASE_URL + "/internal/reports/earnings"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.getPlatformEarnings(null, null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("system.integration_error"));

        assertThatThrownBy(() -> client.getPlatformEarnings(null, null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("system.integration_error"));

        assertThatThrownBy(() -> client.getPlatformEarnings(null, null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("system.integration_error"));
    }

    @Test
    void wallet_400_validation_failed_maps_to_bad_request_validation_failed() {
        server.expect(requestTo(BASE_URL + "/internal/reports/earnings"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"status":400,"detail":"dateFrom must be less than or equal to dateTo","code":"validation.failed"}
                                """));

        assertThatThrownBy(() -> client.getPlatformEarnings(null, null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("validation.failed");
                    assertThat(apiEx.getMessage()).contains("dateFrom must be less than or equal to dateTo");
                });
    }
}
