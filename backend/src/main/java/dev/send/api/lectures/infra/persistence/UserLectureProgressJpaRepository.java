package dev.send.api.lectures.infra.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLectureProgressJpaRepository extends JpaRepository<UserLectureProgressEntity, Long> {
    Optional<UserLectureProgressEntity> findByUserIdAndLectureId(String userId, String lectureId);
}
