package com.goaiplatform.backend.service;

import com.goaiplatform.backend.config.AppProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class SensitiveWordServiceTest {

    @Test
    void sanitizeShouldMaskSensitiveWords() {
        AppProperties properties = new AppProperties();
        List<String> words = new ArrayList<>();
        words.add("赌博");
        words.add("暴恐");
        properties.setSensitiveWords(words);
        SensitiveWordService service = new SensitiveWordService(properties);
        service.init(); // Manually call @PostConstruct

        String result = service.sanitize("这是赌博与暴恐相关内容");

        // Expected: masking Chinese characters - verify it masks the sensitive words
        Assertions.assertTrue(result.contains("**"));
        Assertions.assertFalse(result.contains("赌博"));
        Assertions.assertFalse(result.contains("暴恐"));
    }
}
