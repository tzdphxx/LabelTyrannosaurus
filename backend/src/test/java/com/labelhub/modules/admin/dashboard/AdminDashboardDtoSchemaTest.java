package com.labelhub.modules.admin.dashboard;

import com.labelhub.modules.admin.dashboard.dto.AdminDashboardAlert;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardAlertLevel;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardAlertType;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardKpis;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardOverviewResponse;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardTopLabeler;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardTopTask;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardTrendPoint;
import com.labelhub.modules.admin.dashboard.dto.AdminDashboardUserSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminDashboardDtoSchemaTest {

    @Test
    void dashboardResponseRecordsExposeChineseSchemaDescriptions() {
        List.of(
                AdminDashboardOverviewResponse.class,
                AdminDashboardKpis.class,
                AdminDashboardUserSummary.class,
                AdminDashboardTrendPoint.class,
                AdminDashboardTopLabeler.class,
                AdminDashboardTopTask.class,
                AdminDashboardAlert.class
        ).forEach(this::assertRecordSchema);
    }

    @Test
    void dashboardResponseEnumsExposeChineseSchemaDescriptions() {
        assertTypeSchema(AdminDashboardAlertType.class);
        assertTypeSchema(AdminDashboardAlertLevel.class);
    }

    private void assertRecordSchema(Class<?> type) {
        assertTypeSchema(type);
        for (RecordComponent component : type.getRecordComponents()) {
            Schema schema = component.getAccessor().getAnnotation(Schema.class);
            assertThat(schema)
                    .as(type.getSimpleName() + "." + component.getName() + " should have @Schema")
                    .isNotNull();
            assertThat(schema.description())
                    .as(type.getSimpleName() + "." + component.getName() + " should have Chinese description")
                    .isNotBlank();
        }
    }

    private void assertTypeSchema(Class<?> type) {
        Schema schema = type.getAnnotation(Schema.class);
        assertThat(schema)
                .as(type.getSimpleName() + " should have @Schema")
                .isNotNull();
        assertThat(schema.description())
                .as(type.getSimpleName() + " should have Chinese description")
                .isNotBlank();
    }
}
