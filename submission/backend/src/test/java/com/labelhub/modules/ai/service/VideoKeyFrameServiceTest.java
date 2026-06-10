package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VideoKeyFrameServiceTest {

    @Test
    void generatesSnapshotUrlsForProvidedVideoUrl() {
        VideoKeyFrameService service = serviceWithDefaultConfig();

        List<String> urls = service.generateKeyFrameUrls("http://vjs.zencdn.net/v/oceans.mp4", 12);

        assertThat(urls).isEmpty();
    }

    @Test
    void rewritesCosVideoHostToCiHostBeforeSnapshot() {
        VideoKeyFrameService service = serviceWithDefaultConfig();

        List<String> urls = service.generateKeyFrameUrls(
                "https://bucket-123.cos.ap-guangzhou.myqcloud.com/videos/oceans.mp4", 5);

        assertThat(urls).containsExactly(
                "https://bucket-123.ci.ap-guangzhou.myqcloud.com/videos/oceans.mp4?ci-process=snapshot&format=jpg&width=1280&time=5"
        );
    }

    @Test
    void appendsSnapshotParametersAfterExistingQuery() {
        VideoKeyFrameService service = serviceWithDefaultConfig();

        List<String> urls = service.generateKeyFrameUrls(
                "https://bucket-123.cos.ap-guangzhou.myqcloud.com/videos/oceans.mp4?sign=abc", 5);

        assertThat(urls).containsExactly(
                "https://bucket-123.ci.ap-guangzhou.myqcloud.com/videos/oceans.mp4?sign=abc&ci-process=snapshot&format=jpg&width=1280&time=5"
        );
    }

    private VideoKeyFrameService serviceWithDefaultConfig() {
        VideoKeyFrameService service = new VideoKeyFrameService();
        ReflectionTestUtils.setField(service, "maxFrames", 5);
        ReflectionTestUtils.setField(service, "intervalSeconds", 5);
        return service;
    }
}
