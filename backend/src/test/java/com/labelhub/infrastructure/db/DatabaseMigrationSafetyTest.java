package com.labelhub.infrastructure.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationSafetyTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @Test
    void conflictStatusMigrationNormalizesLegacyValuesBeforeAddingNewCheck() throws IOException {
        String migration = Files.readString(MIGRATION_DIR.resolve("V11__conflict_golden_snapshot.sql"));

        assertThat(migration).contains("information_schema.table_constraints");
        assertThat(migration).contains("UPDATE conflict_groups");
        assertThat(migration).contains("CONFLICTED");
        assertThat(migration).contains("CONSENSUS_REACHED");
        assertThat(migration.indexOf("UPDATE conflict_groups"))
                .isLessThan(migration.indexOf("ADD CONSTRAINT chk_conflict_groups_status"));
    }

    @Test
    void aiFlowPolicyMigrationSkipsColumnsAlreadyPresentInBaseline() throws IOException {
        String baseline = Files.readString(MIGRATION_DIR.resolve("V1__baseline.sql"));
        String migration = Files.readString(MIGRATION_DIR.resolve("V9__ai_flow_policy.sql"));

        assertThat(baseline).contains("`reject_threshold` decimal(5,2) DEFAULT NULL");
        assertThat(migration).contains("information_schema.columns");
        assertThat(migration).contains("column_name = 'reject_threshold'");
        assertThat(migration).contains("PREPARE add_ai_review_configs_reject_threshold_stmt");
    }

    @Test
    void conflictGroupUniqueConstraintMigrationIsIdempotentForFreshBaseline() throws IOException {
        String baseline = Files.readString(MIGRATION_DIR.resolve("V1__baseline.sql"));
        String migration = Files.readString(MIGRATION_DIR.resolve("V13__conflict_group_unique_constraint.sql"));

        assertThat(baseline).contains("UNIQUE KEY `uk_conflict_groups_task_item`");
        assertThat(migration).contains("information_schema.statistics");
        assertThat(migration).contains("uk_conflict_groups_task_item");
        assertThat(migration).contains("PREPARE");
    }

    @Test
    void llmProviderOwnerMigrationFailsEarlyForRowsWithoutCreatedBy() throws IOException {
        String migration = Files.readString(MIGRATION_DIR.resolve("V15__llm_providers_owner_isolation.sql"));

        assertThat(migration).contains("created_by IS NULL");
        assertThat(migration).contains("chk_llm_provider_owner_backfill_source");
        assertThat(migration.indexOf("created_by IS NULL"))
                .isLessThan(migration.indexOf("MODIFY COLUMN owner_id BIGINT NOT NULL"));
    }

    @Test
    void llmProviderAdminGlobalMigrationGuardsOwnerRemoval() throws IOException {
        String migration = Files.readString(MIGRATION_DIR.resolve("V29__llm_providers_admin_global.sql"));

        assertThat(migration).contains("information_schema.referential_constraints");
        assertThat(migration).contains("fk_llm_providers_owner");
        assertThat(migration).contains("information_schema.statistics");
        assertThat(migration).contains("uk_llm_providers_owner_code");
        assertThat(migration).contains("uk_llm_providers_code");
        assertThat(migration).contains("information_schema.columns");
        assertThat(migration).contains("DROP COLUMN owner_id");
        assertThat(migration.indexOf("DROP FOREIGN KEY fk_llm_providers_owner"))
                .isLessThan(migration.indexOf("DROP COLUMN owner_id"));
    }

    @Test
    void ownerTemplateMigrationBackfillsBeforeEnforcingOwner() throws IOException {
        String migration = Files.readString(MIGRATION_DIR.resolve("V27__owner_template_library.sql"));

        assertThat(migration).contains("ADD COLUMN owner_id BIGINT NULL");
        assertThat(migration).contains("UPDATE templates t");
        assertThat(migration).contains("JOIN tasks task ON task.id = t.task_id");
        assertThat(migration).contains("chk_template_owner_backfill_source");
        assertThat(migration.indexOf("UPDATE templates t"))
                .isLessThan(migration.indexOf("MODIFY COLUMN owner_id BIGINT NOT NULL"));
        assertThat(migration.indexOf("DROP FOREIGN KEY fk_templates_task"))
                .isLessThan(migration.indexOf("MODIFY COLUMN task_id BIGINT NULL"));
    }
}
