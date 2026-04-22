CREATE TABLE user_strategies (
  strategy_id VARCHAR(100) PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  graph_json TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_strategies_user_id_updated_at
  ON user_strategies (user_id, updated_at DESC);
