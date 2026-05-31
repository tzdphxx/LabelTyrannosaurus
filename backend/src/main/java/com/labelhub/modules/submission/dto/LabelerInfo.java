package com.labelhub.modules.submission.dto;

/**
 * 导出快照中的标注员展示信息。
 */
public record LabelerInfo(Long labelerId,
                          String username,
                          String displayName,
                          String email) {
}
