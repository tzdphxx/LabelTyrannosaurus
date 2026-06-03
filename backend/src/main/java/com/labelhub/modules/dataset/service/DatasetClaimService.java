package com.labelhub.modules.dataset.service;

import java.util.Optional;

public interface DatasetClaimService {

    Optional<DatasetItemSnapshot> reserveClaimableItem(Long taskId, Long labelerId);

    void increaseSubmittedCount(Long itemId);

    void increaseApprovedCount(Long itemId);
}
