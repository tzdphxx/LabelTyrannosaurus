package com.labelhub.infrastructure.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MyBatisAnnotationSqlTest {

    private static final Path MAIN_JAVA_DIR = Path.of("src/main/java");
    private static final Pattern SCRIPT_BLOCK = Pattern.compile("<script>[\\s\\S]*?</script>");

    @Test
    void myBatisScriptAnnotationsDoNotUseRawXmlComparisonOperators() throws IOException {
        List<String> offenders;
        try (var paths = Files.walk(MAIN_JAVA_DIR)) {
            offenders = paths
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(MyBatisAnnotationSqlTest::containsRawComparisonInScriptBlock)
                    .map(MAIN_JAVA_DIR::relativize)
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertThat(offenders)
                .as("Use != or &lt;&gt; inside MyBatis <script> annotation SQL")
                .isEmpty();
    }

    private static boolean containsRawComparisonInScriptBlock(Path path) {
        try {
            String content = Files.readString(path);
            return SCRIPT_BLOCK.matcher(content)
                    .results()
                    .anyMatch(match -> match.group().contains("<>"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + path, ex);
        }
    }
}
