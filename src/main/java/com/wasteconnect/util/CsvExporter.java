package com.wasteconnect.util;

import com.wasteconnect.model.Exportable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utility for exporting domain entities and analytics datasets to CSV files.
 */
public final class CsvExporter {

    private CsvExporter() {}

    /**
     * Exports a list of Exportable items to a target file.
     */
    public static <T extends Exportable> File export(List<T> items, File targetFile) throws IOException {
        if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            if (items != null && !items.isEmpty()) {
                String[] headers = items.get(0).getCsvHeaders();
                writer.write(String.join(",", headers));
                writer.newLine();

                for (T item : items) {
                    writer.write(item.toCsvRow());
                    writer.newLine();
                }
            }
        }
        return targetFile;
    }

    /**
     * Exports arbitrary tabular data (headers + rows).
     */
    public static File exportRaw(String[] headers, List<String[]> rows, File targetFile) throws IOException {
        if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            if (headers != null && headers.length > 0) {
                writer.write(String.join(",", headers));
                writer.newLine();
            }
            if (rows != null) {
                for (String[] row : rows) {
                    writer.write(String.join(",", escapeRow(row)));
                    writer.newLine();
                }
            }
        }
        return targetFile;
    }

    private static String[] escapeRow(String[] row) {
        String[] escaped = new String[row.length];
        for (int i = 0; i < row.length; i++) {
            String val = row[i] == null ? "" : row[i];
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                val = "\"" + val.replace("\"", "\"\"") + "\"";
            }
            escaped[i] = val;
        }
        return escaped;
    }
}
