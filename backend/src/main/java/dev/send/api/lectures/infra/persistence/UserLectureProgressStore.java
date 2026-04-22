package dev.send.api.lectures.infra.persistence;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.send.api.lectures.domain.LectureModels.LectureCheckpointState;
import dev.send.api.lectures.domain.LectureModels.LectureProgress;

@Component
public class UserLectureProgressStore {
    private final UserLectureProgressJpaRepository repository;
    private final ObjectMapper objectMapper;

    public UserLectureProgressStore(
            UserLectureProgressJpaRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Optional<LectureProgress> findByUserIdAndLectureId(String userId, String lectureId) {
        return repository.findByUserIdAndLectureId(userId, lectureId).map(this::toDomain);
    }

    public LectureProgress save(String userId, LectureProgress progress) {
        Instant now = Instant.now();
        UserLectureProgressEntity existing = repository.findByUserIdAndLectureId(userId, progress.lectureId()).orElse(null);
        UserLectureProgressEntity entity = new UserLectureProgressEntity(
                existing == null ? null : existing.getId(),
                userId,
                progress.lectureId(),
                progress.highestUnlockedSublectureIndex(),
                serialize(progress.completedCheckpointIds()),
                serialize(progress.activeCheckpointState()),
                existing == null ? now : existing.getCreatedAt(),
                now);
        repository.save(entity);
        return toDomain(entity);
    }

    private LectureProgress toDomain(UserLectureProgressEntity entity) {
        return new LectureProgress(
                entity.getLectureId(),
                entity.getHighestUnlockedSublectureIndex(),
                deserializeCompletedCheckpointIds(entity.getCompletedCheckpointIdsJson()),
                deserializeCheckpointState(entity.getActiveCheckpointStateJson()));
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not serialize lecture progress.", exception);
        }
    }

    private List<String> deserializeCompletedCheckpointIds(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (IOException exception) {
            throw new IllegalStateException("Could not deserialize lecture checkpoint ids.", exception);
        }
    }

    private Map<String, LectureCheckpointState> deserializeCheckpointState(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, LectureCheckpointState>>() {});
        } catch (IOException exception) {
            throw new IllegalStateException("Could not deserialize lecture checkpoint state.", exception);
        }
    }
}
