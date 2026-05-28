package com.labelhub.modules.submission.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("submissions")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Long taskId;

    private Long datasetItemId;

    private Long labelerId;

    private Long templateVersionId;

    private Integer versionNo;

    private String answerJson;

    private String answerHash;

    private SubmissionStatus status;

    private Boolean isGolden;

    private LocalDateTime submittedAt;

    private LocalDateTime updatedAt;
}
