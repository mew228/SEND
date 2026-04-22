package dev.send.api.lectures.infra.persistence;

import java.time.Instant;
import java.util.Objects;

import javax.annotation.Nullable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "user_lecture_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_lecture_progress_user_lecture", columnNames = {"user_id", "lecture_id"}))
public class UserLectureProgressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Nullable
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId = "";

    @Column(name = "lecture_id", nullable = false, length = 255)
    private String lectureId = "";

    @Column(name = "highest_unlocked_sublecture_index", nullable = false)
    private int highestUnlockedSublectureIndex;

    @Column(name = "completed_checkpoint_ids_json", nullable = false, columnDefinition = "TEXT")
    private String completedCheckpointIdsJson = "[]";

    @Column(name = "active_checkpoint_state_json", nullable = false, columnDefinition = "TEXT")
    private String activeCheckpointStateJson = "{}";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    protected UserLectureProgressEntity() {}

    public UserLectureProgressEntity(
            @Nullable Long id,
            String userId,
            String lectureId,
            int highestUnlockedSublectureIndex,
            String completedCheckpointIdsJson,
            String activeCheckpointStateJson,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.lectureId = Objects.requireNonNull(lectureId, "lectureId must not be null");
        this.highestUnlockedSublectureIndex = highestUnlockedSublectureIndex;
        this.completedCheckpointIdsJson = Objects.requireNonNull(completedCheckpointIdsJson, "completedCheckpointIdsJson must not be null");
        this.activeCheckpointStateJson = Objects.requireNonNull(activeCheckpointStateJson, "activeCheckpointStateJson must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public @Nullable Long getId() {
        return id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
    }

    public String getLectureId() {
        return lectureId;
    }

    public void setLectureId(String lectureId) {
        this.lectureId = Objects.requireNonNull(lectureId, "lectureId must not be null");
    }

    public int getHighestUnlockedSublectureIndex() {
        return highestUnlockedSublectureIndex;
    }

    public void setHighestUnlockedSublectureIndex(int highestUnlockedSublectureIndex) {
        this.highestUnlockedSublectureIndex = highestUnlockedSublectureIndex;
    }

    public String getCompletedCheckpointIdsJson() {
        return completedCheckpointIdsJson;
    }

    public void setCompletedCheckpointIdsJson(String completedCheckpointIdsJson) {
        this.completedCheckpointIdsJson = Objects.requireNonNull(completedCheckpointIdsJson, "completedCheckpointIdsJson must not be null");
    }

    public String getActiveCheckpointStateJson() {
        return activeCheckpointStateJson;
    }

    public void setActiveCheckpointStateJson(String activeCheckpointStateJson) {
        this.activeCheckpointStateJson = Objects.requireNonNull(activeCheckpointStateJson, "activeCheckpointStateJson must not be null");
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
