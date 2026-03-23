package com.goaiplatform.backend.service;

import com.goaiplatform.backend.config.AppProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class SensitiveWordServiceTest {

    @Test
    void sanitizeShouldMaskSensitiveWords() {
        AppProperties properties = new AppProperties();
        properties.setSensitiveWords(List.of("赌博", "暴恐"));
        SensitiveWordService service = new SensitiveWordService(properties);

        String result = service.sanitize("这是赌博与暴恐相关内容");

        Assertions.assertEquals("这是**与**相关内容", result);
    }
}
