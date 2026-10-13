package be.npl.shelfwise.report.helper;

import java.util.List;
import java.util.stream.Collectors;

/** Minimal CSV writer (F-REP-01): comma separated, values with commas, quotes or line breaks are quoted. */
public final class CsvWriter {

    private CsvWriter() {
    }

    public static String toCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder(line(headers)).append("\r\n");
        rows.forEach(row -> csv.append(line(row)).append("\r\n"));
        return csv.toString();
    }

    private static String line(List<String> values) {
        return values.stream().map(CsvWriter::escape).collect(Collectors.joining(","));
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
