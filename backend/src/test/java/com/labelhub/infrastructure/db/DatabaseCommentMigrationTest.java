package com.labelhub.infrastructure.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseCommentMigrationTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");
    private static final Path COMMENT_MIGRATION = MIGRATION_DIR.resolve("V5__add_table_column_comments.sql");

    @Test
    void commentMigrationCoversEveryBusinessTableAndColumnWithChineseComments() throws IOException {
        Map<String, List<String>> schema = loadBusinessSchema();

        assertThat(Files.exists(COMMENT_MIGRATION)).isTrue();
        String migration = Files.readString(COMMENT_MIGRATION);

        assertThat(schema).hasSize(28);
        assertThat(schema.values().stream().mapToInt(List::size).sum()).isEqualTo(325);

        schema.forEach((tableName, columns) -> {
            assertThat(findTableComment(migration, tableName))
                    .as("table comment for %s", tableName)
                    .containsPattern("\\p{IsHan}");

            columns.forEach(columnName -> assertThat(findColumnComment(migration, tableName, columnName))
                    .as("column comment for %s.%s", tableName, columnName)
                    .containsPattern("\\p{IsHan}"));
        });
    }

    private static Map<String, List<String>> loadBusinessSchema() throws IOException {
        Map<String, List<String>> schema = parseCreateTableColumns(
                Files.readString(MIGRATION_DIR.resolve("V1__baseline.sql"))
        );

        addColumn(schema, "dataset_items", "active_external_id");
        addColumn(schema, "export_jobs", "trace_id");

        return schema;
    }

    private static Map<String, List<String>> parseCreateTableColumns(String sql) {
        Map<String, List<String>> schema = new LinkedHashMap<>();
        Pattern createTablePattern = Pattern.compile("create\\s+table\\s+(\\w+)\\s*\\(", Pattern.CASE_INSENSITIVE);
        Matcher matcher = createTablePattern.matcher(sql);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            int bodyStart = matcher.end();
            int bodyEnd = findMatchingParenthesis(sql, bodyStart - 1);
            String body = sql.substring(bodyStart, bodyEnd);

            List<String> columns = splitDefinitions(body).stream()
                    .map(DatabaseCommentMigrationTest::columnName)
                    .filter(column -> column != null)
                    .toList();
            schema.put(tableName, new ArrayList<>(columns));
        }
        return schema;
    }

    private static int findMatchingParenthesis(String sql, int openIndex) {
        int depth = 0;
        boolean inQuote = false;
        for (int i = openIndex; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'') {
                inQuote = !inQuote;
            } else if (!inQuote && current == '(') {
                depth++;
            } else if (!inQuote && current == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("Unclosed create table statement");
    }

    private static List<String> splitDefinitions(String body) {
        List<String> definitions = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inQuote = false;
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '\'') {
                inQuote = !inQuote;
            } else if (!inQuote && ch == '(') {
                depth++;
            } else if (!inQuote && ch == ')') {
                depth--;
            } else if (!inQuote && ch == ',' && depth == 0) {
                definitions.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            definitions.add(current.toString().trim());
        }
        return definitions;
    }

    private static String columnName(String definition) {
        String normalized = definition.stripLeading().replaceAll("\\s+", " ");
        String lowerCase = normalized.toLowerCase();
        if (lowerCase.startsWith("primary key")
                || lowerCase.startsWith("constraint")
                || lowerCase.startsWith("foreign key")
                || lowerCase.startsWith("unique")
                || lowerCase.startsWith("check ")) {
            return null;
        }
        return normalized.split(" ")[0].replace("`", "");
    }

    private static void addColumn(Map<String, List<String>> schema, String tableName, String columnName) {
        List<String> columns = schema.get(tableName);
        int insertAfter = columns.indexOf("deleted");
        if ("trace_id".equals(columnName)) {
            insertAfter = columns.indexOf("error_message");
        }
        columns.add(insertAfter + 1, columnName);
    }

    private static String findTableComment(String migration, String tableName) {
        Pattern pattern = Pattern.compile(
                "alter\\s+table\\s+" + tableName + "\\s+comment\\s*=\\s*'([^']*)'\\s*;",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(migration);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String findColumnComment(String migration, String tableName, String columnName) {
        Pattern pattern = Pattern.compile(
                "alter\\s+table\\s+" + tableName
                        + "\\s+modify\\s+column\\s+" + columnName
                        + "\\b.*?comment\\s+'([^']*)'\\s*;",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(migration);
        return matcher.find() ? matcher.group(1) : "";
    }
}
