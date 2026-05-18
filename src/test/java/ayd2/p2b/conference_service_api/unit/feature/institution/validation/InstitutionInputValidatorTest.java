package ayd2.p2b.conference_service_api.unit.feature.institution.validation;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.institution.application.InstitutionInputValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstitutionInputValidatorTest {

    @Test
    void shouldTrimRequiredAndOptionalFields() {
        assertThat(InstitutionInputValidator.requiredTrimmed("  USAC  ", "name")).isEqualTo("USAC");
        assertThat(InstitutionInputValidator.optionalTrimmed("  Public  ", "description")).isEqualTo("Public");
    }

    @Test
    void shouldRejectBlankRequiredAndOptionalFields() {
        assertThatThrownBy(() -> InstitutionInputValidator.requiredTrimmed(" ", "name"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidation((ApiException) ex));

        assertThatThrownBy(() -> InstitutionInputValidator.optionalTrimmed(" ", "description"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidation((ApiException) ex));
    }

    @Test
    void shouldValidateRequiredEmail() {
        assertThat(InstitutionInputValidator.validateEmailRequired("  user@example.com  "))
                .isEqualTo("user@example.com");
    }

    @Test
    void shouldRejectInvalidEmails() {
        assertThatThrownBy(() -> InstitutionInputValidator.validateEmailRequired("invalid-email"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidation((ApiException) ex));

        assertThatThrownBy(() -> InstitutionInputValidator.validateEmailOptional("invalid-email"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidation((ApiException) ex));
    }

    @Test
    void shouldAllowNullOptionalEmail() {
        assertThat(InstitutionInputValidator.validateEmailOptional(null)).isNull();
    }

    private void assertValidation(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("validation.failed");
    }
}
