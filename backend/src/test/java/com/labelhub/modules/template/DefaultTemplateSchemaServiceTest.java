package com.labelhub.modules.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import com.labelhub.modules.template.service.DefaultTemplateSchemaService;
import com.labelhub.modules.template.service.TemplateSchemaSnapshot;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
class DefaultTemplateSchemaServiceTest {

    @Mock private TemplateVersionMapper templateVersionMapper;
    @Mock private RedissonClient redissonClient;
    @Mock private RBucket<TemplateSchemaSnapshot> bucket;

    @Test
    void returnsCachedTemplateSchemaWhenPresent() {
        TemplateSchemaSnapshot cached = new TemplateSchemaSnapshot(10L, "{\"cached\":true}");
        when(redissonClient.<TemplateSchemaSnapshot>getBucket("cache:template:schema:10"))
                .thenReturn(bucket);
        when(bucket.get()).thenReturn(cached);

        TemplateSchemaSnapshot result = service().getTemplateSchema(10L);

        assertThat(result).isEqualTo(cached);
        verify(templateVersionMapper, never()).selectById(any());
    }

    @Test
    void cachesTemplateSchemaLoadedFromDatabase() {
        TemplateVersion version = new TemplateVersion();
        version.setId(10L);
        version.setSchemaJson("{\"fields\":[]}");
        when(redissonClient.<TemplateSchemaSnapshot>getBucket("cache:template:schema:10"))
                .thenReturn(bucket);
        when(bucket.get()).thenReturn(null);
        when(templateVersionMapper.selectById(10L)).thenReturn(version);

        TemplateSchemaSnapshot result = service().getTemplateSchema(10L);

        assertThat(result.schemaJson()).isEqualTo("{\"fields\":[]}");
        verify(bucket).set(eq(result), eq(6L), eq(TimeUnit.HOURS));
    }

    private DefaultTemplateSchemaService service() {
        return new DefaultTemplateSchemaService(templateVersionMapper, redissonClient);
    }
}
