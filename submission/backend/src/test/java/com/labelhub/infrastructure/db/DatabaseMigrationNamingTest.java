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

class DatabaseMigrationNamingTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");
    private static final Pattern VERSIONED_MIGRATION =
            Pattern.compile("^V([0-9][0-9._]*)__([a-z0-9_]+)\\.sql$");

    @Test
    void flywayMigrationVersionsAreUniqueAndUseCanonicalNames() throws IOException {
        Map<String, List<String>> migrationsByVersion = new LinkedHashMap<>();
        List<String> invalidNames = new ArrayList<>();

        try (var paths = Files.list(MIGRATION_DIR)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .forEach(fileName -> {
                        Matcher matcher = VERSIONED_MIGRATION.matcher(fileName);
                        if (!matcher.matches()) {
                            invalidNames.add(fileName);
                            return;
                        }
                        String normalizedVersion = matcher.group(1).replace('_', '.');
                        migrationsByVersion
                                .computeIfAbsent(normalizedVersion, ignored -> new ArrayList<>())
                                .add(fileName);
                    });
        }

        assertThat(invalidNames).isEmpty();
        assertThat(migrationsByVersion)
                .allSatisfy((version, files) -> assertThat(files)
                        .as("Flyway version %s", version)
                        .hasSize(1));
    }
}
