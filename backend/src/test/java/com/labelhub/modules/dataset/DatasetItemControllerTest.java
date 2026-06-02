package com.labelhub.modules.dataset;

import com.labelhub.modules.dataset.controller.DatasetItemController;
import com.labelhub.modules.dataset.dto.BatchAppendItemsRequest;
import com.labelhub.modules.dataset.dto.DatasetImportJobResponse;
import com.labelhub.modules.dataset.dto.DatasetImportRequest;
import com.labelhub.modules.dataset.service.DatasetImportService;
import com.labelhub.modules.dataset.service.DatasetItemService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetItemControllerTest {

    private final DatasetItemService datasetItemService = mock(DatasetItemService.class);
    private final DatasetImportService datasetImportService = mock(DatasetImportService.class);
    private final DatasetItemController controller = new DatasetItemController(datasetItemService, datasetImportService);

    @Test
    void batchAppendDelegatesUploadedFileToAppendImport() {
        DatasetImportJobResponse job = new DatasetImportJobResponse(
                300L, 1L, "PENDING", "APPEND", 0, 0, 0,
                null, null, null, null, null, null);
        when(datasetImportService.createAppendImport(1L, new DatasetImportRequest(99L))).thenReturn(job);

        var response = controller.batchAppend(1L, new BatchAppendItemsRequest(99L));

        assertThat(response.data()).isEqualTo(job);
        verify(datasetImportService).createAppendImport(1L, new DatasetImportRequest(99L));
    }
}
