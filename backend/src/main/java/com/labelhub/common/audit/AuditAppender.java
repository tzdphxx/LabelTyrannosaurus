package com.labelhub.common.audit;

/**
 * 统一的只追加审计写入接口。
 *
 * <p>审计表由 BE-B 维护。BE-A 与 BE-B 都必须通过该接口写入审计，禁止更新或删除已有审计行。</p>
 */
public interface AuditAppender {

    /**
     * 追加一条审计日志并返回生成的主键。
     *
     * @param command 审计追加命令，traceId 必填
     * @return 生成的审计日志 id
     */
    Long append(AuditCommand command);
}
