package com.labelhub.modules.ai.service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 使用腾讯云 COS 数据万象（CI）截取视频关键帧。
 * 不下载视频、不调用 FFmpeg，直接拼 CI 截帧 URL。
 */
@Service
public class VideoKeyFrameService {

    private static final Logger log = LoggerFactory.getLogger(VideoKeyFrameService.class);

    /** CI 截帧参数模板：每5秒一帧，JPG格式，宽1280 */
    private static final String CI_SNAPSHOT = "?ci-process=snapshot&format=jpg&width=1280&time=%d";

    @Value("${labelhub.media.keyframe.max-frames:5}")
    private int maxFrames;

    @Value("${labelhub.media.keyframe.interval-seconds:5}")
    private int intervalSeconds;

    /**
     * 为视频 URL 生成关键帧截帧 URL 列表。
     *
     * @param cosUrl          原始 COS 视频 URL
     * @param durationSeconds 视频时长（秒），null 时默认为 60
     * @return 关键帧截帧 URL 列表，长度不超过 maxFrames
     */
    public List<String> generateKeyFrameUrls(String cosUrl, Integer durationSeconds) {
        if (cosUrl == null || cosUrl.isBlank()) {
            log.debug("Cannot generate key frames: COS URL is blank");
            return List.of();
        }
        if (durationSeconds == null || durationSeconds <= 0) {
            durationSeconds = 60;
        }

        String ciUrl = cosUrl.replaceFirst("\\.cos\\.", ".ci.");

        List<String> urls = new ArrayList<>();
        int effectiveInterval = Math.max(1, intervalSeconds);
        int effectiveMax = Math.max(1, maxFrames);

        for (int t = effectiveInterval; t <= durationSeconds && urls.size() < effectiveMax; t += effectiveInterval) {
            urls.add(ciUrl + String.format(CI_SNAPSHOT, t));
        }

        log.debug("Generated {} key frames for video (duration={}s)", urls.size(), durationSeconds);
        return urls;
    }
}
