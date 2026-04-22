import type {
  LectureCatalogResponse,
  LectureCheckpointSubmission,
  LectureCheckpointVerificationResult,
  LectureDetailResponse,
  LectureProgress,
} from "../features/lectures/types";
import { fetchWithAuth, readJsonOrThrowError } from "./http";

const API_URL = import.meta.env.VITE_API_URL?.trim() || "";

type LectureProgressStatePayload = {
  lastFeedback?: string | null;
  lastAttemptedAt?: string | null;
  passed: boolean;
};

type LectureProgressPayload = {
  lectureId: string;
  highestUnlockedSublectureIndex: number;
  completedCheckpointIds: string[];
  activeCheckpointState: Record<string, LectureProgressStatePayload>;
};

function toLectureProgress(payload: LectureProgressPayload): LectureProgress {
  return {
    lectureId: payload.lectureId,
    highestUnlockedSublectureIndex: payload.highestUnlockedSublectureIndex,
    completedCheckpointIds: payload.completedCheckpointIds,
    activeCheckpointState: Object.fromEntries(
      Object.entries(payload.activeCheckpointState ?? {}).map(([checkpointId, state]) => [
        checkpointId,
        {
          lastFeedback: state.lastFeedback ?? undefined,
          lastAttemptedAt: state.lastAttemptedAt ?? undefined,
          passed: state.passed,
        },
      ])
    ),
  };
}

export async function fetchLectureCatalog(): Promise<LectureCatalogResponse> {
  const response = await fetchWithAuth(`${API_URL}/api/lectures`, {
    method: "GET",
    authMode: "optional",
    credentials: "include",
  });

  return readJsonOrThrowError<LectureCatalogResponse>(response, `Lecture request failed (${response.status})`);
}

export async function fetchLecture(lectureId: string): Promise<LectureDetailResponse> {
  const [pathSlug, categorySlug, ...lectureSlugParts] = lectureId.split("--");
  const lectureSlug = lectureSlugParts.join("--");
  if (!pathSlug || !categorySlug || !lectureSlug) {
    throw new Error("Lecture id is malformed.");
  }

  return fetchLectureBySlug(pathSlug, categorySlug, lectureSlug);
}

export async function fetchLectureBySlug(
  pathSlug: string,
  categorySlug: string,
  lectureSlug: string
): Promise<LectureDetailResponse> {
  const response = await fetchWithAuth(
    `${API_URL}/api/lectures/${encodeURIComponent(pathSlug)}/${encodeURIComponent(categorySlug)}/${encodeURIComponent(lectureSlug)}`,
    {
      method: "GET",
      authMode: "optional",
      credentials: "include",
    }
  );

  const payload = await readJsonOrThrowError<LectureDetailResponse & { progress: LectureProgressPayload }>(
    response,
    `Lecture request failed (${response.status})`
  );
  return {
    ...payload,
    progress: toLectureProgress(payload.progress),
  };
}

export async function fetchLectureProgress(lectureId: string): Promise<LectureProgress> {
  const response = await fetchWithAuth(`${API_URL}/api/lectures/${encodeURIComponent(lectureId)}/progress`, {
    method: "GET",
    authMode: "optional",
    credentials: "include",
  });

  return toLectureProgress(
    await readJsonOrThrowError<LectureProgressPayload>(response, `Lecture request failed (${response.status})`)
  );
}

export async function saveLectureProgress(progress: LectureProgress): Promise<LectureProgress> {
  const response = await fetchWithAuth(`${API_URL}/api/lectures/${encodeURIComponent(progress.lectureId)}/progress`, {
    method: "POST",
    authMode: "optional",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(progress),
  });

  return toLectureProgress(
    await readJsonOrThrowError<LectureProgressPayload>(response, `Lecture request failed (${response.status})`)
  );
}

export async function verifyLectureCheckpoint(
  lectureId: string,
  checkpointId: string,
  submission: LectureCheckpointSubmission
): Promise<LectureCheckpointVerificationResult> {
  const response = await fetchWithAuth(
    `${API_URL}/api/lectures/${encodeURIComponent(lectureId)}/checkpoints/${encodeURIComponent(checkpointId)}/verify`,
    {
      method: "POST",
      authMode: "optional",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(submission),
    }
  );

  return readJsonOrThrowError<LectureCheckpointVerificationResult>(
    response,
    `Lecture request failed (${response.status})`
  );
}
