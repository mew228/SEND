package dev.send.api.lectures.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import dev.send.api.auth.CurrentUser;
import dev.send.api.auth.CurrentUserAccessor;
import dev.send.api.lectures.domain.LectureModels.LectureDefinition;
import dev.send.api.lectures.domain.LectureModels.LectureProgress;
import dev.send.api.lectures.infra.LectureProgressCookieStore;
import dev.send.api.lectures.infra.persistence.UserLectureProgressStore;

@Component
public class HybridLectureProgressStore implements LectureProgressStore {
    private final LectureProgressCookieStore lectureProgressCookieStore;
    private final UserLectureProgressStore userLectureProgressStore;
    private final CurrentUserAccessor currentUserAccessor;
    private final LectureProgressPolicy lectureProgressPolicy;

    public HybridLectureProgressStore(
            LectureProgressCookieStore lectureProgressCookieStore,
            UserLectureProgressStore userLectureProgressStore,
            CurrentUserAccessor currentUserAccessor,
            LectureProgressPolicy lectureProgressPolicy) {
        this.lectureProgressCookieStore = lectureProgressCookieStore;
        this.userLectureProgressStore = userLectureProgressStore;
        this.currentUserAccessor = currentUserAccessor;
        this.lectureProgressPolicy = lectureProgressPolicy;
    }

    @Override
    public LectureProgress load(LectureDefinition lecture, HttpServletRequest request) {
        Optional<CurrentUser> currentUser = currentUserAccessor.findCurrentUser();
        if (currentUser.isPresent()) {
            return loadAuthenticatedProgress(lecture, request, currentUser.get());
        }

        Map<String, LectureProgress> storedProgress = lectureProgressCookieStore.readProgressMap(request);
        return lectureProgressPolicy.normalizeProgress(
                lecture,
                storedProgress.getOrDefault(lecture.id(), lectureProgressPolicy.defaultProgress(lecture.id())));
    }

    @Override
    public LectureProgress save(
            LectureDefinition lecture,
            LectureProgress progress,
            HttpServletRequest request,
            HttpServletResponse response) {
        LectureProgress normalizedProgress = lectureProgressPolicy.normalizeProgress(lecture, progress);
        Optional<CurrentUser> currentUser = currentUserAccessor.findCurrentUser();
        if (currentUser.isPresent()) {
            return userLectureProgressStore.save(currentUser.get().id(), normalizedProgress);
        }

        Map<String, LectureProgress> storedProgress = new LinkedHashMap<>(lectureProgressCookieStore.readProgressMap(request));
        storedProgress.put(lecture.id(), normalizedProgress);
        lectureProgressCookieStore.writeProgressMap(response, storedProgress);
        return normalizedProgress;
    }

    private LectureProgress loadAuthenticatedProgress(
            LectureDefinition lecture,
            HttpServletRequest request,
            CurrentUser currentUser) {
        Optional<LectureProgress> storedProgress = userLectureProgressStore.findByUserIdAndLectureId(currentUser.id(), lecture.id());
        if (storedProgress.isPresent()) {
            return lectureProgressPolicy.normalizeProgress(lecture, storedProgress.get());
        }

        Map<String, LectureProgress> cookieProgressMap = lectureProgressCookieStore.readProgressMap(request);
        LectureProgress importedProgress = lectureProgressPolicy.normalizeProgress(
                lecture,
                cookieProgressMap.getOrDefault(lecture.id(), lectureProgressPolicy.defaultProgress(lecture.id())));
        return userLectureProgressStore.save(currentUser.id(), importedProgress);
    }
}
