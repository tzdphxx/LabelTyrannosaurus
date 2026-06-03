package com.labelhub.modules.dataset.service;

import java.util.Optional;

public interface DatasetClaimService {

    Optional<DatasetItemSnapshot> reserveClaimableItem(Long taskId, Long labelerId, Integer overlapCount);

    /**
     * Reserve a specific dataset item for claiming (used for ASSIGNED strategy).
     * Increments assigned_count and returns the item snapshot with itemJson.
     */
    Optional<DatasetItemSnapshot> reserveSpecificItem(Long taskId, Long labelerId, Long datasetItemId);

    void increaseSubmittedCount(Long itemId);

    void increaseApprovedCount(Long itemId);
}
