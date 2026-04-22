const API_URL = (import.meta.env.VITE_API_URL?.trim() || "").replace(/\/+$/, "");

export const SANDBOX_NODE_WIDTH = 240;
export const SANDBOX_LIBRARY_NODE_WIDTH = 180;
export const SANDBOX_NOTIFICATION_TIMEOUT_MS = 4200;
export const SANDBOX_DEFAULT_UNTITLED_STRATEGY_NAME = "Untitled Strategy";

const SANDBOX_STRATEGIES_API_BASE = API_URL;

export const SANDBOX_STRATEGIES_API = {
  baseUrl: SANDBOX_STRATEGIES_API_BASE,
  listStrategiesUrl: `${SANDBOX_STRATEGIES_API_BASE}/api/strategies`,
  strategyByIdUrl: (strategyId: string) =>
    `${SANDBOX_STRATEGIES_API_BASE}/api/strategies/${encodeURIComponent(strategyId)}`,
  nodeIoUrl: `${SANDBOX_STRATEGIES_API_BASE}/api/strategies/node-io`,
  simulationBoundsUrl: `${SANDBOX_STRATEGIES_API_BASE}/api/strategies/simulation-bounds`,
  testStrategyUrl: `${SANDBOX_STRATEGIES_API_BASE}/api/strategies/test`,
  simulateStrategyUrl: `${SANDBOX_STRATEGIES_API_BASE}/api/strategies/simulate`,
};
