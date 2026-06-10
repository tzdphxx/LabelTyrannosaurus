package com.labelhub.modules.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.ai.domain.LlmProvider;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmProviderMapper extends BaseMapper<LlmProvider> {
}
