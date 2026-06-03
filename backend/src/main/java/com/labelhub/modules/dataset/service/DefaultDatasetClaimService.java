package com.labelhub.modules.dataset.service;

import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultDatasetClaimService implements DatasetClaimService {

    private final DatasetItemMapper datasetItemMapper;

    public DefaultDatasetClaimService(DatasetItemMapper datasetItemMapper) {
        this.datasetItemMapper = datasetItemMapper;
    }

    @Override
    public Optional<DatasetItemSnapshot> reserveClaimableItem(Long taskId, Long labelerId) {
        Long datasetItemId = datasetItemMapper.selectClaimableItemId(taskId, labelerId);
        if (datasetItemId == null) {
            return Optional.empty();
        }
        if (datasetItemMapper.reserveIfAvailable(datasetItemId) != 1) {
            return Optional.empty();
        }
        DatasetItem datasetItem = datasetItemMapper.selectById(datasetItemId);
        if (datasetItem == null) {
            return Optional.empty();
        }
        return Optional.of(new DatasetItemSnapshot(datasetItem.getId(), datasetItem.getItemJson()));
    }

    @Override
    public void increaseSubmittedCount(Long itemId) {
        datasetItemMapper.increaseSubmittedCount(itemId);
    }

    @Override
    public void increaseApprovedCount(Long itemId) {
        datasetItemMapper.increaseApprovedCount(itemId);
    }
}
