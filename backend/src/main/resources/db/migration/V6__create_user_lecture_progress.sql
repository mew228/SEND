CREATE TABLE user_lecture_progress (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL,
  lecture_id VARCHAR(255) NOT NULL,
  highest_unlocked_sublecture_index INTEGER NOT NULL,
  completed_checkpoint_ids_json TEXT NOT NULL,
  active_checkpoint_state_json TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uk_user_lecture_progress_user_lecture UNIQUE (user_id, lecture_id)
);

CREATE INDEX idx_user_lecture_progress_user_id
  ON user_lecture_progress (user_id);
