package ayd2.p2b.conference_service_api.feature.report.application.earnings;

import ayd2.p2b.conference_service_api.feature.report.application.exception.ReportExceptions;
import ayd2.p2b.conference_service_api.feature.report.application.support.ReportAccessPolicy;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsCongressItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsInstitutionItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.EarningsReportResponse;
import ayd2.p2b.conference_service_api.integration.dto.WalletCongressEarningsItem;
import ayd2.p2b.conference_service_api.integration.dto.WalletInstitutionEarningsItem;
import ayd2.p2b.conference_service_api.integration.dto.WalletPlatformEarningsReportResponse;
import ayd2.p2b.conference_service_api.integration.port.WalletReportPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class EarningsReportUseCase {

    private final WalletReportPort walletReportPort;

    public EarningsReportUseCase(WalletReportPort walletReportPort) {
        this.walletReportPort = walletReportPort;
    }

    public EarningsReportResponse execute(
            UUID institutionId,
            LocalDate dateFrom,
            LocalDate dateTo,
            ReportRequesterContext requester
    ) {
        ReportAccessPolicy.ensureSystemAdmin(requester);
        ensureValidDateRange(dateFrom, dateTo);

        WalletPlatformEarningsReportResponse walletResponse = walletReportPort.getPlatformEarnings(
                institutionId,
                dateFrom,
                dateTo
        );

        List<EarningsInstitutionItem> items = safeList(walletResponse.getItems())
                .stream()
                .map(this::mapInstitutionItem)
                .toList();

        return EarningsReportResponse.builder()
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

    private EarningsInstitutionItem mapInstitutionItem(WalletInstitutionEarningsItem walletItem) {
        return EarningsInstitutionItem.builder()
                .institutionId(walletItem.getInstitutionId())
                .institutionName(walletItem.getInstitutionName())
                .congresses(safeList(walletItem.getCongresses()).stream().map(this::mapCongressItem).toList())
                .institutionTotalAmount(walletItem.getInstitutionTotalAmount())
                .institutionTotalCommission(walletItem.getInstitutionTotalCommission())
                .institutionTotalNet(walletItem.getInstitutionTotalNet())
                .paymentCount(walletItem.getPaymentCount())
                .build();
    }

    private EarningsCongressItem mapCongressItem(WalletCongressEarningsItem walletCongress) {
        return EarningsCongressItem.builder()
                .congressId(walletCongress.getCongressId())
                .congressName(walletCongress.getCongressName())
                .totalAmount(walletCongress.getTotalAmount())
                .commissionAmount(walletCongress.getCommissionAmount())
                .netAmount(walletCongress.getNetAmount())
                .paymentCount(walletCongress.getPaymentCount())
                .build();
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
