package com.goaiplatform.backend.service;

import com.goaiplatform.backend.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SensitiveWordService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordService.class);

    private final AppProperties appProperties;
    private Pattern sensitivePattern;

    public SensitiveWordService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        List<String> words = appProperties.getSensitiveWords();
        if (words == null || words.isEmpty()) {
            log.info("No sensitive words configured");
            this.sensitivePattern = Pattern.compile("(?!)", Pattern.CASE_INSENSITIVE);
            return;
        }

        // Sort by length (longest first) to handle overlapping words
        words.sort(Comparator.comparingInt(String::length).reversed());

        // Escape special regex characters and join with OR
        String regex = words.stream()
                .map(Pattern::quote)
                .collect(java.util.stream.Collectors.joining("|"));

        this.sensitivePattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        log.info("Sensitive word filter initialized with {} words", words.size());
    }

    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        if (sensitivePattern == null) {
            return input;
        }

        return sensitivePattern.matcher(input).replaceAll(match -> "*".repeat(match.group().length()));
    }
}
