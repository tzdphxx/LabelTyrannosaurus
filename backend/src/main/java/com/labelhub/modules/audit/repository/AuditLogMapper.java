package com.labelhub.modules.audit.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.audit.domain.AuditLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {

    /**
     * 查询单个业务对象的审计时间线。
     *
     * <p>审计日志只追加，因此时间线顺序由 {@code created_at} 和 {@code id} 决定；
     * 该 Mapper 不提供任何更新审计行的能力。</p>
     */
    @Select("""
            select * from audit_logs
            where biz_type = #{bizType} and biz_id = #{bizId}
            order by created_at asc, id asc
            """)
    List<AuditLogEntity> selectByBiz(String bizType, Long bizId);
}
