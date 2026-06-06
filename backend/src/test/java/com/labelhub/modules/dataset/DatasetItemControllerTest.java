package com.labelhub.modules.dataset;

import com.labelhub.modules.dataset.controller.DatasetItemController;
import com.labelhub.modules.dataset.dto.BatchAppendItemsRequest;
import com.labelhub.modules.dataset.dto.BatchAppendJsonItemsRequest;
import com.labelhub.modules.dataset.dto.BatchItemResult;
import com.labelhub.modules.dataset.dto.DatasetItemAppendRequest;
import com.labelhub.modules.dataset.service.DatasetItemService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetItemControllerTest {

    private final DatasetItemService datasetItemService = mock(DatasetItemService.class);
    private final DatasetItemController controller = new DatasetItemController(datasetItemService);

    @Test
    void batchAppendDelegatesItemsToService() {
        BatchAppendItemsRequest request = new BatchAppendItemsRequest(List.of(
                new DatasetItemAppendRequest("q1", Map.of("question", "one"), Map.of("source", "manual"))
        ));
        List<BatchItemResult> results = List.of(BatchItemResult.success(100L, "q1"));
        when(datasetItemService.batchAppend(1L, request)).thenReturn(results);

        var response = controller.batchAppend(1L, request);

        assertThat(response.data()).isEqualTo(results);
        verify(datasetItemService).batchAppend(1L, request);
    }

    @Test
    void batchAppendJsonDelegatesItemsToService() {
        List<DatasetItemAppendRequest> items = List.of(
                new DatasetItemAppendRequest("q1", Map.of("question", "one"), Map.of("source", "manual"))
        );
        BatchAppendJsonItemsRequest request = new BatchAppendJsonItemsRequest(items);
        BatchAppendItemsRequest delegatedRequest = new BatchAppendItemsRequest(items);
        List<BatchItemResult> results = List.of(BatchItemResult.success(100L, "q1"));
        when(datasetItemService.batchAppend(1L, delegatedRequest)).thenReturn(results);

        var response = controller.batchAppendJson(1L, request);

        assertThat(response.data()).isEqualTo(results);
        verify(datasetItemService).batchAppend(1L, delegatedRequest);
    }
}
