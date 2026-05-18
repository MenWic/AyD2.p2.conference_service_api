package ayd2.p2b.conference_service_api.unit.common.util;

import ayd2.p2b.conference_service_api.common.util.TextNormalizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextNormalizerTest {

    @Test
    void shouldTrimRequiredText() {
        assertThat(TextNormalizer.trimRequired("  value  ", "name")).isEqualTo("value");
    }

    @Test
    void shouldRejectMissingRequiredText() {
        assertThatThrownBy(() -> TextNormalizer.trimRequired("  ", "name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }

    @Test
    void shouldReturnNullForMissingOptionalText() {
        assertThat(TextNormalizer.trimOptional(" ")).isNull();
        assertThat(TextNormalizer.trimOptional(null)).isNull();
    }

    @Test
    void shouldLowercaseAndTrimRequiredText() {
        assertThat(TextNormalizer.lowerTrimRequired("  ABC  ", "code")).isEqualTo("abc");
    }
}
