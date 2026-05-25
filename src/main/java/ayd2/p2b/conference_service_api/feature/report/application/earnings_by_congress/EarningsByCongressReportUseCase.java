package ayd2.p2b.conference_service_api.feature.report.application.earnings_by_congress;

import ayd2.p2b.conference_service_api.feature.report.application.exception.ReportExceptions;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsCongressScopePort;
import ayd2.p2b.conference_service_api.feature.report.application.support.ReportAccessPolicy;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsByCongressItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsByCongressReportResponse;
import ayd2.p2b.conference_service_api.integration.dto.WalletEarningsByCongressItem;
import ayd2.p2b.conference_service_api.integration.dto.WalletEarningsByCongressReportResponse;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import ayd2.p2b.conference_service_api.integration.port.WalletReportPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class EarningsByCongressReportUseCase {

    private final ParticipantsCongressScopePort congressScopePort;
    private final IamUserLookupPort iamUserLookupPort;
    private final WalletReportPort walletReportPort;

    public EarningsByCongressReportUseCase(
            ParticipantsCongressScopePort congressScopePort,
            IamUserLookupPort iamUserLookupPort,
            WalletReportPort walletReportPort
    ) {
        this.congressScopePort = congressScopePort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.walletReportPort = walletReportPort;
    }

    public EarningsByCongressReportResponse execute(
            UUID congressId,
            LocalDate dateFrom,
            LocalDate dateTo,
            ReportRequesterContext requester
    ) {
        ReportAccessPolicy.ensureCongressAdmin(requester);

        if (congressId == null) {
            throw ReportExceptions.missingCongressId();
        }
        ensureValidDateRange(dateFrom, dateTo);

        CongressInstitutionSummary summary = congressScopePort.findCongressSummary(congressId)
                .orElseThrow(() -> ReportExceptions.congressNotFound(congressId));

        if (!iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(), summary.getInstitutionId(), requester.getAccessToken())) {
            throw ReportExceptions.congressAccessDenied(congressId);
        }

        WalletEarningsByCongressReportResponse walletResponse = walletReportPort.getEarningsByCongress(
                congressId,
                summary.getInstitutionId(),
                dateFrom,
                dateTo
        );

        List<EarningsByCongressItem> items = safeList(walletResponse.getItems())
                .stream()
                .map(this::mapItem)
                .toList();

        return EarningsByCongressReportResponse.builder()
                .items(items)
                .totalItems(walletResponse.getTotalItems())
                .grandTotalAmount(walletResponse.getGrandTotalAmount())
                .grandTotalCommission(walletResponse.getGrandTotalCommission())
                .grandTotalNet(walletResponse.getGrandTotalNet())
                .build();
    }

    private void ensureValidDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw ReportExceptions.invalidDateRange(dateFrom, dateTo);
        }
    }

    private EarningsByCongressItem mapItem(WalletEarningsByCongressItem walletItem) {
        return EarningsByCongressItem.builder()
                .congressId(walletItem.getCongressId())
                .congressName(walletItem.getCongressName())
                .totalAmount(walletItem.getTotalAmount())
                .commissionAmount(walletItem.getCommissionAmount())
                .netAmount(walletItem.getNetAmount())
                .paymentCount(walletItem.getPaymentCount())
                .build();
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
