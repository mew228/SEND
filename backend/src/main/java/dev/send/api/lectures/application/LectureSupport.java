package dev.send.api.lectures.application;

public final class LectureSupport {
    private LectureSupport() {}

    public static final class LectureNotFoundException extends RuntimeException {
        public LectureNotFoundException(String message) {
            super(message);
        }
    }

    public static final class LectureValidationException extends RuntimeException {
        public LectureValidationException(String message) {
            super(message);
        }
    }

    public record LectureRuleEvaluationResult(
            boolean passed,
            String feedback) {}
}
