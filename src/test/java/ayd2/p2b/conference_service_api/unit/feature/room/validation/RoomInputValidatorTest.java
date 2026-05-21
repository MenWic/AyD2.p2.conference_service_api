package ayd2.p2b.conference_service_api.unit.feature.room.validation;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.room.application.RoomInputValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomInputValidatorTest {

    @Test
    void shouldTrimRequiredValue() {
        String value = RoomInputValidator.requiredTrimmed("  Sala Magna  ", "name");
        assertThat(value).isEqualTo("Sala Magna");
    }

    @Test
    void shouldRejectBlankRequiredValue() {
        assertThatThrownBy(() -> RoomInputValidator.requiredTrimmed("  ", "name"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidation((ApiException) ex));
    }

    @Test
    void shouldTrimOptionalNonBlankValue() {
        String value = RoomInputValidator.optionalTrimmedNonBlank("  Norte  ", "name");
        assertThat(value).isEqualTo("Norte");
    }

    @Test
    void shouldRejectBlankOptionalNonBlankValue() {
        assertThatThrownBy(() -> RoomInputValidator.optionalTrimmedNonBlank(" ", "name"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidation((ApiException) ex));
    }

    @Test
    void shouldNormalizeOptionalLocationBlankToNull() {
        assertThat(RoomInputValidator.optionalTrimmedToNull("   ")).isNull();
        assertThat(RoomInputValidator.optionalTrimmedToNull("  Edificio A  ")).isEqualTo("Edificio A");
    }

    @Test
    void shouldRejectNonPositiveCapacity() {
        assertThatThrownBy(() -> RoomInputValidator.optionalPositiveCapacity(0))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertValidation((ApiException) ex));
    }

    private void assertValidation(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo("validation.failed");
    }
}
