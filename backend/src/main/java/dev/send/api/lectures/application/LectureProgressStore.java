package dev.send.api.lectures.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dev.send.api.lectures.domain.LectureModels.LectureDefinition;
import dev.send.api.lectures.domain.LectureModels.LectureProgress;

public interface LectureProgressStore {
    LectureProgress load(LectureDefinition lecture, HttpServletRequest request);

    LectureProgress save(
            LectureDefinition lecture,
            LectureProgress progress,
            HttpServletRequest request,
            HttpServletResponse response);
}
