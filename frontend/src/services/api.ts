import { SANDBOX_STRATEGIES_API } from "../config/sandboxConfig";
import type { CategoryTheme, JsonScalar, NodeIoCatalog, NodeRuntimeResult } from "../components/nodes/NodeTypes";
import { fetchWithAuth, readJson, readJsonOrThrowApiError } from "./http";

export type { ApiError } from "./http";

export type StrategyTestResults = Record<string, NodeRuntimeResult>;

export type StrategyGraphNode = {
  id: string;
  type: string;
  position: {
    x: number;
    y: number;
  };
  data: Record<string, JsonScalar>;
};

export type StrategyGraphEdge = {
  id: string;
  source: string;
  target: string;
  sourceHandle?: string;
  targetHandle?: string;
};

export type StrategySummary = {
  id: string;
  name: string;
  kind: "template" | "user";
  updatedAt: string;
};

export type StoredStrategy = StrategySummary & {
  nodes: StrategyGraphNode[];
  edges: StrategyGraphEdge[];
};

export type StrategySimulationConfig = {
  startDate: string;
  endDate: string;
  initialCash: number;
  includeTrace: true;
};

export type StrategySimulationBounds = {
  hasPriceData: boolean;
  earliestPriceDate: string | null;
  latestPriceDate: string | null;
};

export type StrategySimulationTradeEvent = {
  nodeId: string;
  action: string;
  ticker: string;
  sizeMode: string;
  requestedAmount: number;
  filledShares: number;
  fillPrice: number;
  cashBefore: number;
  cashAfter: number;
  realizedPnl: number;
};

export type StrategySimulationBalanceSnapshot = {
  cash: number;
  marketValue: number;
  equity: number;
  realizedPnl: number;
  unrealizedPnl: number;
};

export type StrategySimulationPosition = {
  ticker: string;
  quantity: number;
  averageCost: number;
  marketPrice?: number | null;
  marketValue: number;
  unrealizedPnl: number;
};

export type StrategySimulationNodeChange = {
  nodeId: string;
  outputs: NodeRuntimeResult;
};

export type StrategySimulationTraceDay = {
  date: string;
  warnings: string[];
  errors: string[];
  trades: StrategySimulationTradeEvent[];
  balanceSnapshot: StrategySimulationBalanceSnapshot;
  nodeChanges: StrategySimulationNodeChange[];
};

export type StrategySimulationSummary = {
  startDate: string;
  endDate: string;
  executedDays: number;
  initialCash: number;
  finalCash: number;
  marketValue: number;
  finalEquity: number;
  realizedPnl: number;
  unrealizedPnl: number;
  tradeCount: number;
};

export type StrategySimulationResult = {
  summary: StrategySimulationSummary;
  portfolio: {
    cash: number;
    positions: StrategySimulationPosition[];
  };
  finalNodeValues: Record<string, NodeRuntimeResult>;
  trace: StrategySimulationTraceDay[];
  warnings: string[];
};

export async function fetchStrategySummaries(signal?: AbortSignal): Promise<StrategySummary[]> {
  const response = await fetchWithAuth(SANDBOX_STRATEGIES_API.listStrategiesUrl, {
    method: "GET",
    authMode: "optional",
    signal,
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch strategies (${response.status})`);
  }

  const payload = (await readJson<unknown>(response));
  if (!Array.isArray(payload)) {
    throw new Error("Malformed strategy summary response.");
  }

  return payload.flatMap((strategy) => (isStrategySummary(strategy) ? [strategy] : []));
}

export async function fetchStoredStrategy(strategyId: string, signal?: AbortSignal): Promise<StoredStrategy> {
  const response = await fetchWithAuth(SANDBOX_STRATEGIES_API.strategyByIdUrl(strategyId), {
    method: "GET",
    authMode: "optional",
    signal,
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch strategy (${response.status})`);
  }

  const payload = (await readJson<unknown>(response));
  if (!isStoredStrategy(payload)) {
    throw new Error("Malformed strategy data.");
  }
  return payload;
}

export async function createStoredStrategy(
  payload: Pick<StoredStrategy, "name" | "nodes" | "edges">,
  signal?: AbortSignal
): Promise<StoredStrategy> {
  const response = await fetchWithAuth(SANDBOX_STRATEGIES_API.listStrategiesUrl, {
    method: "POST",
    authMode: "required",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
    signal,
  });

  const json = await readJsonOrThrowApiError<unknown>(response);
  if (!isStoredStrategy(json)) {
    throw new Error("Malformed stored strategy response.");
  }
  return json;
}

export async function updateStoredStrategy(
  strategyId: string,
  payload: Pick<StoredStrategy, "name" | "nodes" | "edges">,
  signal?: AbortSignal
): Promise<StoredStrategy> {
  const response = await fetchWithAuth(SANDBOX_STRATEGIES_API.strategyByIdUrl(strategyId), {
    method: "PUT",
    authMode: "required",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
    signal,
  });

  const json = await readJsonOrThrowApiError<unknown>(response);
  if (!isStoredStrategy(json)) {
    throw new Error("Malformed stored strategy response.");
  }
  return json;
}

export async function fetchNodeIoCatalog(signal?: AbortSignal): Promise<NodeIoCatalog> {
  const response = await fetchWithAuth(SANDBOX_STRATEGIES_API.nodeIoUrl, {
    method: "GET",
    authMode: "public",
    signal,
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch node catalog (${response.status})`);
  }

  const payload = (await readJson<unknown>(response));
  if (!isNodeIoCatalog(payload)) {
    throw new Error("Malformed node catalog payload.");
  }
  return payload;
}

export async function fetchSimulationBounds(signal?: AbortSignal): Promise<StrategySimulationBounds> {
  const response = await fetchWithAuth(SANDBOX_STRATEGIES_API.simulationBoundsUrl, {
    method: "GET",
    authMode: "public",
    signal,
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch simulation bounds (${response.status})`);
  }

  const payload = (await readJson<unknown>(response));
  if (!isStrategySimulationBounds(payload)) {
    throw new Error("Malformed simulation bounds payload.");
  }
  return payload;
}

export async function testStrategy(
  payload: unknown,
  signal?: AbortSignal
): Promise<StrategyTestResults> {
  const response = await fetchWithAuth(SANDBOX_STRATEGIES_API.testStrategyUrl, {
    method: "POST",
    authMode: "public",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
    signal,
  });

  const json = await readJsonOrThrowApiError<unknown>(response);
  if (!isStrategyTestResults(json)) {
    throw new Error("Malformed strategy test response.");
  }
  return json;
}

export async function simulateStrategy(
  payload: unknown,
  signal?: AbortSignal
): Promise<StrategySimulationResult> {
  const response = await fetchWithAuth(SANDBOX_STRATEGIES_API.simulateStrategyUrl, {
    method: "POST",
    authMode: "public",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
    signal,
  });

  const json = await readJsonOrThrowApiError<unknown>(response);
  if (!isStrategySimulationResult(json)) {
    throw new Error("Malformed strategy simulation response.");
  }
  return json;
}

function isNodeIoCatalog(payload: unknown): payload is NodeIoCatalog {
  if (!isRecord(payload)) return false;
  if (!Array.isArray(payload.nodes)) return false;

  return payload.nodes.every((node) => {
    if (!isRecord(node)) return false;
    if (typeof node.nodeType !== "string") return false;
    if (typeof node.displayName !== "string") return false;
    if (typeof node.nodeClass !== "string") return false;
    if (!isCategoryTheme(node.theme)) return false;
    if (!Array.isArray(node.inputs) || !Array.isArray(node.outputs) || !Array.isArray(node.dataFields)) {
      return false;
    }

    return (
      [...node.inputs, ...node.outputs].every((port) => {
        if (!isRecord(port)) return false;
        return (
          typeof port.index === "number" &&
          typeof port.name === "string" &&
          typeof port.arity === "string" &&
          typeof port.valueType === "string" &&
          typeof port.valueTypeClass === "string"
        );
      }) &&
      node.dataFields.every((field) => {
        if (!isRecord(field)) return false;
        const { defaultValue } = field;
        const rawOptions = field.options;
        const optionsValid =
          rawOptions == null ||
          (Array.isArray(rawOptions) && rawOptions.every((option) => typeof option === "string"));
        const optionList =
          Array.isArray(rawOptions) && rawOptions.every((option) => typeof option === "string")
            ? rawOptions
            : undefined;
        const readableOptionsValid =
          field.readableOptions == null ||
          (Array.isArray(field.readableOptions) &&
            field.readableOptions.every((option) => typeof option === "string") &&
            (optionList === undefined || field.readableOptions.length === optionList.length));
        const optionFilterValid =
          field.optionFilter == null ||
          (isRecord(field.optionFilter) &&
            typeof field.optionFilter.field === "string" &&
            isRecord(field.optionFilter.groups) &&
            Object.values(field.optionFilter.groups).every((group) => {
              if (!isRecord(group) || !Array.isArray(group.options)) return false;
              if (!group.options.every((option) => typeof option === "string")) return false;
              if (group.readableOptions === undefined) return true;
              return (
                Array.isArray(group.readableOptions) &&
                group.readableOptions.every((option) => typeof option === "string") &&
                group.readableOptions.length === group.options.length
              );
            }));
        const visibleWhenValid =
          field.visibleWhen == null ||
          (isRecord(field.visibleWhen) &&
            typeof field.visibleWhen.field === "string" &&
            Array.isArray(field.visibleWhen.values) &&
            field.visibleWhen.values.every((value) => typeof value === "string"));
        return (
          typeof field.name === "string" &&
          (field.label == null || typeof field.label === "string") &&
          typeof field.valueType === "string" &&
          typeof field.valueTypeClass === "string" &&
          typeof field.required === "boolean" &&
          (defaultValue === undefined || isJsonScalar(defaultValue)) &&
          optionsValid &&
          readableOptionsValid &&
          optionFilterValid &&
          visibleWhenValid
        );
      })
    );
  });
}

function isStrategySummary(payload: unknown): payload is StrategySummary {
  return (
    isRecord(payload) &&
    typeof payload.id === "string" &&
    typeof payload.name === "string" &&
    typeof payload.kind === "string" &&
    typeof payload.updatedAt === "string" &&
    (payload.kind === "template" || payload.kind === "user")
  );
}

function isStrategyGraphNode(value: unknown): value is StrategyGraphNode {
  return (
    isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.type === "string" &&
    isRecord(value.position) &&
    typeof value.position.x === "number" &&
    typeof value.position.y === "number" &&
    isRecord(value.data) &&
    Object.values(value.data).every(isJsonScalar)
  );
}

function isStrategyGraphEdge(value: unknown): value is StrategyGraphEdge {
  return (
    isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.source === "string" &&
    typeof value.target === "string" &&
    (value.sourceHandle === undefined || typeof value.sourceHandle === "string") &&
    (value.targetHandle === undefined || typeof value.targetHandle === "string")
  );
}

function isStoredStrategy(payload: unknown): payload is StoredStrategy {
  if (!isRecord(payload) || !isStrategySummary(payload)) {
    return false;
  }
  const { nodes, edges } = payload as Record<string, unknown>;
  return (
    Array.isArray(nodes) &&
    nodes.every(isStrategyGraphNode) &&
    Array.isArray(edges) &&
    edges.every(isStrategyGraphEdge)
  );
}

function isStrategyTestResults(payload: unknown): payload is StrategyTestResults {
  if (!isRecord(payload)) return false;

  return Object.values(payload).every((nodeResult) => {
    if (!isRecord(nodeResult)) return false;
    return Object.values(nodeResult).every((value) => isJsonScalar(value));
  });
}

function isStrategySimulationResult(payload: unknown): payload is StrategySimulationResult {
  if (!isRecord(payload)) return false;

  return (
    isStrategySimulationSummary(payload.summary) &&
    isStrategySimulationPortfolio(payload.portfolio) &&
    isRuntimeResultRecord(payload.finalNodeValues) &&
    Array.isArray(payload.trace) &&
    payload.trace.every(isStrategySimulationTraceDay) &&
    Array.isArray(payload.warnings) &&
    payload.warnings.every((warning) => typeof warning === "string")
  );
}

function isStrategySimulationSummary(value: unknown): value is StrategySimulationSummary {
  return (
    isRecord(value) &&
    typeof value.startDate === "string" &&
    typeof value.endDate === "string" &&
    typeof value.executedDays === "number" &&
    typeof value.initialCash === "number" &&
    typeof value.finalCash === "number" &&
    typeof value.marketValue === "number" &&
    typeof value.finalEquity === "number" &&
    typeof value.realizedPnl === "number" &&
    typeof value.unrealizedPnl === "number" &&
    typeof value.tradeCount === "number"
  );
}

function isStrategySimulationBounds(value: unknown): value is StrategySimulationBounds {
  return (
    isRecord(value) &&
    typeof value.hasPriceData === "boolean" &&
    (value.earliestPriceDate === null || typeof value.earliestPriceDate === "string") &&
    (value.latestPriceDate === null || typeof value.latestPriceDate === "string")
  );
}

function isStrategySimulationPortfolio(
  value: unknown
): value is StrategySimulationResult["portfolio"] {
  return (
    isRecord(value) &&
    typeof value.cash === "number" &&
    Array.isArray(value.positions) &&
    value.positions.every(isStrategySimulationPosition)
  );
}

function isStrategySimulationPosition(value: unknown): value is StrategySimulationPosition {
  return (
    isRecord(value) &&
    typeof value.ticker === "string" &&
    typeof value.quantity === "number" &&
    typeof value.averageCost === "number" &&
    typeof value.marketValue === "number" &&
    typeof value.unrealizedPnl === "number" &&
    (value.marketPrice === undefined ||
      value.marketPrice === null ||
      typeof value.marketPrice === "number")
  );
}

function isStrategySimulationTraceDay(value: unknown): value is StrategySimulationTraceDay {
  return (
    isRecord(value) &&
    typeof value.date === "string" &&
    Array.isArray(value.warnings) &&
    value.warnings.every((warning) => typeof warning === "string") &&
    Array.isArray(value.errors) &&
    value.errors.every((error) => typeof error === "string") &&
    Array.isArray(value.trades) &&
    value.trades.every(isStrategySimulationTradeEvent) &&
    isStrategySimulationBalanceSnapshot(value.balanceSnapshot) &&
    Array.isArray(value.nodeChanges) &&
    value.nodeChanges.every(isStrategySimulationNodeChange)
  );
}

function isStrategySimulationTradeEvent(value: unknown): value is StrategySimulationTradeEvent {
  return (
    isRecord(value) &&
    typeof value.nodeId === "string" &&
    typeof value.action === "string" &&
    typeof value.ticker === "string" &&
    typeof value.sizeMode === "string" &&
    typeof value.requestedAmount === "number" &&
    typeof value.filledShares === "number" &&
    typeof value.fillPrice === "number" &&
    typeof value.cashBefore === "number" &&
    typeof value.cashAfter === "number" &&
    typeof value.realizedPnl === "number"
  );
}

function isStrategySimulationBalanceSnapshot(
  value: unknown
): value is StrategySimulationBalanceSnapshot {
  return (
    isRecord(value) &&
    typeof value.cash === "number" &&
    typeof value.marketValue === "number" &&
    typeof value.equity === "number" &&
    typeof value.realizedPnl === "number" &&
    typeof value.unrealizedPnl === "number"
  );
}

function isStrategySimulationNodeChange(value: unknown): value is StrategySimulationNodeChange {
  return (
    isRecord(value) &&
    typeof value.nodeId === "string" &&
    isRuntimeResult(value.outputs)
  );
}

function isRuntimeResultRecord(value: unknown): value is Record<string, NodeRuntimeResult> {
  if (!isRecord(value)) return false;
  return Object.values(value).every(isRuntimeResult);
}

function isRuntimeResult(value: unknown): value is NodeRuntimeResult {
  if (!isRecord(value)) return false;
  return Object.values(value).every((entry) => isJsonScalar(entry));
}

function isJsonScalar(value: unknown): value is JsonScalar {
  return value === null || ["string", "number", "boolean"].includes(typeof value);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isCategoryTheme(value: unknown): value is CategoryTheme {
  return (
    typeof value === "string" &&
    ["market", "const", "math", "compare", "logic", "convert", "derived", "flow"].includes(value)
  );
}
