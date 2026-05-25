package ayd2.p2b.conference_service_api.feature.report.infrastructure.export;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ReportTableModel {
    String title;
    List<String> headers;
    List<List<String>> rows;
}
