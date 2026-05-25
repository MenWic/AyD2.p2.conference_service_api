package ayd2.p2b.conference_service_api.feature.report.infrastructure.export;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HtmlReportExporter {

    public String export(String title, List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
                .append("<title>").append(escapeHtml(title)).append("</title>")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;margin:24px}")
                .append("h1{font-size:20px;margin-bottom:16px}")
                .append("table{border-collapse:collapse;width:100%}")
                .append("th,td{border:1px solid #d2d2d2;padding:8px 12px;text-align:left}")
                .append("th{background:#f8f9fa;font-weight:600}")
                .append("tr:nth-child(even){background:#fafafa}")
                .append("</style></head><body>")
                .append("<h1>").append(escapeHtml(title)).append("</h1>")
                .append("<table><thead><tr>");

        for (String header : headers) {
            sb.append("<th>").append(escapeHtml(header)).append("</th>");
        }
        sb.append("</tr></thead><tbody>");

        for (List<String> row : rows) {
            sb.append("<tr>");
            for (String cell : row) {
                sb.append("<td>").append(cell != null ? escapeHtml(cell) : "").append("</td>");
            }
            sb.append("</tr>");
        }

        sb.append("</tbody></table></body></html>");
        return sb.toString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
