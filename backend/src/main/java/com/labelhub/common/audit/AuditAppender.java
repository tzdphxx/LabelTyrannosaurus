package com.labelhub.common.audit;

/**
 * Unified append-only audit writer.
 *
 * <p>The audit table is owned by BE-B. BE-A and BE-B must write audit records
 * through this interface and must not update or delete existing audit rows.</p>
 */
public interface AuditAppender {

    /**
     * Appends a single audit log row and returns its generated id.
     *
     * @param command immutable audit append command; traceId is required
     * @return generated audit log id
     */
    Long append(AuditCommand command);
}
