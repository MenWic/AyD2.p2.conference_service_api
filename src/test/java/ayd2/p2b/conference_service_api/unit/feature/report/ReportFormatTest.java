package ayd2.p2b.conference_service_api.unit.feature.report;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportFormat;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportFormatTest {

    @Test
    void parse_null_returns_json() {
        assertThat(ReportFormat.parse(null)).isEqualTo(ReportFormat.JSON);
    }

    @Test
    void parse_blank_returns_json() {
        assertThat(ReportFormat.parse("   ")).isEqualTo(ReportFormat.JSON);
    }

    @Test
    void parse_json_lowercase_returns_json() {
        assertThat(ReportFormat.parse("json")).isEqualTo(ReportFormat.JSON);
    }

    @Test
    void parse_json_uppercase_returns_json() {
        assertThat(ReportFormat.parse("JSON")).isEqualTo(ReportFormat.JSON);
    }

    @Test
    void parse_html_lowercase_returns_html() {
        assertThat(ReportFormat.parse("html")).isEqualTo(ReportFormat.HTML);
    }

    @Test
    void parse_html_uppercase_returns_html() {
        assertThat(ReportFormat.parse("HTML")).isEqualTo(ReportFormat.HTML);
    }

    @Test
    void parse_invalid_value_throws_400() {
        assertThatThrownBy(() -> ReportFormat.parse("pdf"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("validation.failed");
                });
    }

    @Test
    void parse_xml_throws_400() {
        assertThatThrownBy(() -> ReportFormat.parse("xml"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}
