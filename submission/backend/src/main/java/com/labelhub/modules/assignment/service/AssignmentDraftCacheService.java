package com.labelhub.modules.assignment.service;

import java.util.Optional;

public interface AssignmentDraftCacheService {

    Optional<AssignmentDraftCacheEntry> get(Long assignmentId);

    void put(AssignmentDraftCacheEntry entry);
}
