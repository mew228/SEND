package dev.send.api.lectures.application;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.send.api.lectures.application.LectureSupport.LectureNotFoundException;
import dev.send.api.lectures.domain.LectureModels.LectureDefinition;
import dev.send.api.lectures.infra.LectureMarkdownLoader;

@Service
public class LectureService {
    private final List<LectureDefinition> lectures;

    public LectureService(LectureMarkdownLoader lectureMarkdownLoader) {
        this.lectures = lectureMarkdownLoader.loadAll();
    }

    public List<LectureDefinition> findAll() {
        return lectures;
    }

    public LectureDefinition getById(String lectureId) {
        return lectures.stream()
                .filter(lecture -> lecture.id().equals(lectureId))
                .findFirst()
                .orElseThrow(() -> new LectureNotFoundException("Lecture not found."));
    }

    public LectureDefinition getBySlug(String pathSlug, String categorySlug, String lectureSlug) {
        return lectures.stream()
                .filter(lecture -> lecture.path().slug().equals(pathSlug)
                        && lecture.category().slug().equals(categorySlug)
                        && lecture.slug().equals(lectureSlug))
                .findFirst()
                .orElseThrow(() -> new LectureNotFoundException("Lecture not found."));
    }
}
