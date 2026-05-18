package ayd2.p2b.conference_service_api.unit.feature.congress.domain;

import ayd2.p2b.conference_service_api.feature.congress.domain.exception.CongressDomainException;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CongressTest {

    @Test
    void shouldRejectPriceBelowMinimum() {
        assertThatThrownBy(() -> Congress.validatePrice(new BigDecimal("34.99")))
                .isInstanceOf(CongressDomainException.class)
                .hasMessage("price must be >= 35.00");
    }

    @Test
    void shouldRejectStartDateAfterEndDate() {
        assertThatThrownBy(() -> Congress.validateDates(
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 9)
        ))
                .isInstanceOf(CongressDomainException.class)
                .hasMessage("startDate must be <= endDate. startDate=2026-10-10, endDate=2026-10-09");
    }

    @Test
    void shouldBuildValidCongress() {
        Congress congress = Congress.builder()
                .id(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .name("Congreso")
                .description("Descripcion")
                .startDate(LocalDate.of(2026, 10, 10))
                .endDate(LocalDate.of(2026, 10, 12))
                .location("Guatemala")
                .price(new BigDecimal("35.00"))
                .createdBy(UUID.randomUUID())
                .build();

        assertThatCode(congress::validateInvariants).doesNotThrowAnyException();
    }
}
