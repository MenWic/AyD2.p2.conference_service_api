package ayd2.p2b.conference_service_api.integration.port;

import ayd2.p2b.conference_service_api.integration.dto.WalletEarningsByCongressReportResponse;
import ayd2.p2b.conference_service_api.integration.dto.WalletPlatformEarningsReportResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface WalletReportPort {

    WalletEarningsByCongressReportResponse getEarningsByCongress(
            UUID congressId,
            UUID institutionId,
            LocalDate dateFrom,
            LocalDate dateTo
    );

    WalletPlatformEarningsReportResponse getPlatformEarnings(
            UUID institutionId,
            LocalDate dateFrom,
            LocalDate dateTo
    );
}
