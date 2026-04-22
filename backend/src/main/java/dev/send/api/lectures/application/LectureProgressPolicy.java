package dev.send.api.lectures.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;

import dev.send.api.lectures.domain.LectureModels.LectureCheckpointState;
import dev.send.api.lectures.domain.LectureModels.LectureDefinition;
import dev.send.api.lectures.domain.LectureModels.LectureProgress;

@Component
public class LectureProgressPolicy {
    public LectureProgress defaultProgress(String lectureId) {
        return new LectureProgress(lectureId, 0, List.of(), Map.of());
    }

    public LectureProgress normalizeProgress(LectureDefinition lecture, LectureProgress progress) {
        int maxUnlockedIndex = Math.max(0, lecture.sublectures().size() - 1);
        int normalizedUnlockedIndex = Math.min(Math.max(progress.highestUnlockedSublectureIndex(), 0), maxUnlockedIndex);

        List<String> knownCheckpointIds = lecture.sublectures().stream()
                .map(sublecture -> sublecture.checkpointAfter() == null ? null : sublecture.checkpointAfter().id())
                .filter(java.util.Objects::nonNull)
                .toList();

        List<String> completedCheckpointIds = progress.completedCheckpointIds().stream()
                .filter(knownCheckpointIds::contains)
                .distinct()
                .toList();

        int highestIndexFromCompletedCheckpoints = lecture.sublectures().stream()
                .filter(sublecture -> sublecture.checkpointAfter() != null)
                .filter(sublecture -> {
                    var checkpoint = Objects.requireNonNull(sublecture.checkpointAfter());
                    return completedCheckpointIds.contains(checkpoint.id());
                })
                .mapToInt(sublecture -> lecture.sublectures().indexOf(sublecture) + 1)
                .max()
                .orElse(0);

        int finalUnlockedIndex = Math.min(
                Math.max(normalizedUnlockedIndex, highestIndexFromCompletedCheckpoints),
                maxUnlockedIndex);
        finalUnlockedIndex = expandThroughUngatedSublectures(lecture, finalUnlockedIndex);

        Map<String, LectureCheckpointState> activeCheckpointState = progress.activeCheckpointState().entrySet().stream()
                .filter(entry -> knownCheckpointIds.contains(entry.getKey()))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> right,
                        LinkedHashMap::new));

        return new LectureProgress(
                lecture.id(),
                finalUnlockedIndex,
                completedCheckpointIds,
                Map.copyOf(activeCheckpointState));
    }

    private int expandThroughUngatedSublectures(LectureDefinition lecture, int unlockedIndex) {
        int expandedIndex = unlockedIndex;
        while (expandedIndex < lecture.sublectures().size() - 1
                && lecture.sublectures().get(expandedIndex).checkpointAfter() == null) {
            expandedIndex++;
        }
        return expandedIndex;
    }

    public LectureProgress constrainClientUpdate(
            LectureDefinition lecture,
            LectureProgress currentProgress,
            LectureProgress requestedProgress) {
        LectureProgress normalizedCurrent = normalizeProgress(lecture, currentProgress);
        normalizeProgress(lecture, requestedProgress);
        return normalizedCurrent;
    }
}
