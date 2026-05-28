package com.labelhub.modules.dataset.service;

import com.labelhub.modules.dataset.domain.DatasetFileFormat;
import com.labelhub.modules.dataset.domain.DatasetType;

import java.io.InputStream;

/**
 * 数据集文件解析器接口。
 *
 * <p>每种文件格式实现一个解析器，导入服务通过 {@link #format()} 自动选择对应实现。</p>
 */
public interface DatasetParser {

    /**
     * 当前解析器支持的文件格式。
     */
    DatasetFileFormat format();

    /**
     * 将源文件输入流解析为标准导入行和行级错误集合。
     *
     * <p>解析器不负责关闭输入流，输入流生命周期由调用方统一管理。</p>
     */
    DatasetParseResult parse(InputStream inputStream, DatasetType datasetType);
}
