package com.labelhub.modules.dataset;

import com.labelhub.modules.dataset.dto.BatchAppendItemsRequest;
import com.labelhub.modules.dataset.dto.BatchAppendJsonItemsRequest;
import com.labelhub.modules.dataset.dto.BatchUpdateItemsRequest;
import com.labelhub.modules.dataset.dto.DatasetItemAppendRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetItemRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void batchAppendRejectsMissingFileId() {
        var violations = validator.validate(new BatchAppendItemsRequest(null));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void batchAppendJsonRejectsEmptyItems() {
        var violations = validator.validate(new BatchAppendJsonItemsRequest(List.of()));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void batchAppendJsonRejectsBlankExternalId() {
        var violations = validator.validate(new BatchAppendJsonItemsRequest(List.of(
                new DatasetItemAppendRequest(" ", Map.of("question", "one"), null)
        )));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void batchAppendJsonRejectsMissingItemJson() {
        var violations = validator.validate(new BatchAppendJsonItemsRequest(List.of(
                new DatasetItemAppendRequest("q1", null, null)
        )));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void batchUpdateRejectsNullItemElements() {
        var violations = validator.validate(new BatchUpdateItemsRequest(Collections.singletonList(null)));

        assertThat(violations).isNotEmpty();
    }
}
