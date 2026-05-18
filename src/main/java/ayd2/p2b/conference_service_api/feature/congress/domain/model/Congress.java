package ayd2.p2b.conference_service_api.feature.congress.domain.model;

import ayd2.p2b.conference_service_api.feature.congress.domain.exception.CongressExceptions;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class Congress {

    public static final BigDecimal MIN_PRICE = new BigDecimal("35.00");

    UUID id;
    UUID institutionId;
    String name;
    String description;
    LocalDate startDate;
    LocalDate endDate;
    String location;
    BigDecimal price;
    UUID createdBy;
    LocalDateTime createdAt;
    UUID updatedBy;
    LocalDateTime updatedAt;

    public void validateInvariants() {
        validatePrice(price);
        validateDates(startDate, endDate);
    }

    public static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(MIN_PRICE) < 0) {
            throw CongressExceptions.priceTooLow(MIN_PRICE);
        }
    }

    public static void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw CongressExceptions.invalidDateRange(startDate, endDate);
        }
    }
}
