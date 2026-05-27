package com.labelhub.modules.audit.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.audit.domain.AuditLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {

    /**
     * Queries an audit timeline for one business object.
     *
     * <p>Audit logs are append-only, so timeline order is derived from
     * {@code created_at} and {@code id}; this mapper never updates audit rows.</p>
     */
    @Select("""
            select * from audit_logs
            where biz_type = #{bizType} and biz_id = #{bizId}
            order by created_at asc, id asc
            """)
    List<AuditLogEntity> selectByBiz(String bizType, Long bizId);
}
