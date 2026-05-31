package com.labelhub.infrastructure.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(CosProperties.class)
public class CosStorageConfig {

    @Bean
    public COSClient cosClient(CosProperties properties) {
        COSCredentials credentials = new BasicCOSCredentials(properties.secretId(), properties.secretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(properties.region()));
        if (StringUtils.hasText(properties.endpoint())) {
            clientConfig.setEndPointSuffix(properties.endpoint());
        }
        return new COSClient(credentials, clientConfig);
    }
}
