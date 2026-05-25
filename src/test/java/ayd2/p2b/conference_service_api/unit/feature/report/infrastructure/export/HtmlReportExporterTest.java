package ayd2.p2b.conference_service_api.unit.feature.report.infrastructure.export;

import ayd2.p2b.conference_service_api.feature.report.infrastructure.export.HtmlReportExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlReportExporterTest {

    private HtmlReportExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new HtmlReportExporter();
    }

    @Test
    void export_contains_table_tag() {
        String result = exporter.export("Title", List.of("Col1"), List.of(List.of("val1")));
        assertThat(result).contains("<table");
    }

    @Test
    void export_contains_title() {
        String result = exporter.export("My Report Title", List.of("A"), List.of());
        assertThat(result).contains("My Report Title");
    }

    @Test
    void export_contains_headers_as_th() {
        String result = exporter.export("T", List.of("Name", "Email", "Phone"), List.of());
        assertThat(result).contains("<th").contains("Name").contains("Email").contains("Phone");
    }

    @Test
    void export_contains_cell_values_as_td() {
        String result = exporter.export("T",
                List.of("Col1", "Col2"),
                List.of(List.of("Alice", "alice@example.com"), List.of("Bob", "bob@example.com")));
        assertThat(result).contains("<td").contains("Alice").contains("alice@example.com")
                .contains("Bob").contains("bob@example.com");
    }

    @Test
    void export_empty_rows_still_contains_table() {
        String result = exporter.export("Empty", List.of("H1", "H2"), List.of());
        assertThat(result).contains("<table").contains("H1").contains("H2");
        assertThat(result).doesNotContain("<td");
    }

    @Test
    void export_produces_valid_html_structure() {
        String result = exporter.export("Report", List.of("Col"), List.of(List.of("val")));
        assertThat(result).contains("<!DOCTYPE html>").contains("<html").contains("</html>");
    }
}
