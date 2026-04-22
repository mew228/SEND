package dev.send.api.lectures.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;

import dev.send.api.lectures.domain.LectureModels.LectureDefinition;
import dev.send.api.lectures.domain.LectureModels.LectureProgress;

@Service
public class LectureProgressService {
    private final LectureProgressStore lectureProgressStore;
    private final LectureProgressPolicy lectureProgressPolicy;

    public LectureProgressService(
            LectureProgressStore lectureProgressStore,
            LectureProgressPolicy lectureProgressPolicy) {
        this.lectureProgressStore = lectureProgressStore;
        this.lectureProgressPolicy = lectureProgressPolicy;
    }

    public LectureProgress getProgress(LectureDefinition lecture, HttpServletRequest request) {
        return lectureProgressStore.load(lecture, request);
    }

    public LectureProgress saveClientProgress(
            LectureDefinition lecture,
            LectureProgress progress,
            HttpServletRequest request,
            HttpServletResponse response) {
        LectureProgress currentProgress = lectureProgressStore.load(lecture, request);
        LectureProgress constrainedProgress = lectureProgressPolicy.constrainClientUpdate(
                lecture,
                currentProgress,
                progress);
        return lectureProgressStore.save(lecture, constrainedProgress, request, response);
    }

    public LectureProgress saveTrustedProgress(
            LectureDefinition lecture,
            LectureProgress progress,
            HttpServletRequest request,
            HttpServletResponse response) {
        return lectureProgressStore.save(lecture, progress, request, response);
    }
}
