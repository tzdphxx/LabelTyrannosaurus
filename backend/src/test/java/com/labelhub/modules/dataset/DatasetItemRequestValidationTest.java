package com.labelhub.modules.dataset;

import com.labelhub.modules.dataset.dto.BatchAppendItemsRequest;
import com.labelhub.modules.dataset.dto.BatchUpdateItemsRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetItemRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void batchAppendRejectsNullItemElements() {
        var violations = validator.validate(new BatchAppendItemsRequest(Collections.singletonList(null)));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void batchUpdateRejectsNullItemElements() {
        var violations = validator.validate(new BatchUpdateItemsRequest(Collections.singletonList(null)));

        assertThat(violations).isNotEmpty();
    }
}
