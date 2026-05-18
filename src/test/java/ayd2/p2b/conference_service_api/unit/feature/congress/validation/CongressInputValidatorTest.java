package ayd2.p2b.conference_service_api.unit.feature.congress.validation;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.congress.application.CongressInputValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CongressInputValidatorTest {

    @Test
    void shouldTrimRequiredValue() {
        String value = CongressInputValidator.requiredTrimmed("  Congreso  ", "name");
        assertThat(value).isEqualTo("Congreso");
    }

    @Test
    void shouldRejectBlankRequiredValue() {
        assertThatThrownBy(() -> CongressInputValidator.requiredTrimmed("   ", "name"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidationFailed((ApiException) ex));
    }

    @Test
    void shouldReturnNullForMissingOptionalValue() {
        String value = CongressInputValidator.optionalTrimmed(null, "description");
        assertThat(value).isNull();
    }

    @Test
    void shouldTrimOptionalValueWhenPresent() {
        String value = CongressInputValidator.optionalTrimmed("  Desc  ", "description");
        assertThat(value).isEqualTo("Desc");
    }

    @Test
    void shouldRejectBlankOptionalValue() {
        assertThatThrownBy(() -> CongressInputValidator.optionalTrimmed(" ", "description"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidationFailed((ApiException) ex));
    }

    private void assertValidationFailed(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("validation.failed");
    }
}
