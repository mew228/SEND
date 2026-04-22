package dev.send.api.lectures.infra;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import dev.send.api.lectures.application.LectureMarkdownParser;
import dev.send.api.lectures.domain.LectureModels.LectureDefinition;

@Component
public class LectureMarkdownLoader {
    private final ResourcePatternResolver resourcePatternResolver;
    private final LectureMarkdownParser lectureMarkdownParser;

    public LectureMarkdownLoader(
            ResourcePatternResolver resourcePatternResolver,
            LectureMarkdownParser lectureMarkdownParser) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.lectureMarkdownParser = lectureMarkdownParser;
    }

    public List<LectureDefinition> loadAll() {
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath*:lectures/**/*.md");
            return Arrays.stream(resources)
                    .sorted(Comparator.comparing(resource -> resource.getFilename() == null ? "" : resource.getFilename()))
                    .map(this::loadLecture)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load lecture markdown resources.", exception);
        }
    }

    private LectureDefinition loadLecture(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            String markdown = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return lectureMarkdownParser.parse(markdown);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read lecture markdown resource.", exception);
        }
    }
}
