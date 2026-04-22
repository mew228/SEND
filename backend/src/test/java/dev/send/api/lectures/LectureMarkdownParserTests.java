package dev.send.api.lectures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.send.api.lectures.application.LectureMarkdownParser;
import dev.send.api.lectures.domain.LectureModels.LectureDefinition;

class LectureMarkdownParserTests {
    @Test
    void parsesLectureMarkdownIntoStructuredLectureDefinition() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/lectures/foundations/market-graph-basics.md")) {
            assertNotNull(inputStream);
            String markdown = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            LectureMarkdownParser parser = new LectureMarkdownParser(new ObjectMapper());
            LectureDefinition lecture = parser.parse(markdown);

            assertEquals("logic--foundations--market-graph-basics", lecture.id());
            assertEquals("market-graph-basics", lecture.slug());
            assertEquals("Logic", lecture.path().title());
            assertEquals("logic", lecture.path().slug());
            assertEquals("Foundations", lecture.category().title());
            assertEquals(3, lecture.sublectures().size());
            assertEquals("Getting Started", lecture.sublectures().get(0).title());
            assertEquals(3, lecture.sublectures().get(0).headings().size());
            var checkpoint = lecture.sublectures().get(0).checkpointAfter();
            assertNotNull(checkpoint);
            assertEquals("checkpoint-place-buy", checkpoint.id());
        }
    }
}
