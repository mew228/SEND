import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import ReactFlow, {
  Background,
  Controls,
  ReactFlowProvider,
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  type Connection,
  type Edge,
  type EdgeChange,
  type Node,
  type NodeChange,
  type XYPosition,
  useReactFlow,
} from "reactflow";
import "reactflow/dist/style.css";
import {
  createEmptyNodeRegistry,
  createNodeRegistry,
  getPreviewHandleStyle,
  isFieldVisible,
  type NodeData,
  type NodeIoCatalog,
  type NodePaletteItem,
} from "../nodes/NodeTypes";
import {
  UI_ACCENT,
  UI_BORDER_SUBTLE,
  UI_CANVAS,
  UI_CARD,
  UI_TEXT_PRIMARY,
  UI_TEXT_SECONDARY,
} from "../nodes/base/nodeCardStyle";
import { SANDBOX_NODE_WIDTH } from "../../config/sandboxConfig";
import type {
  CheckpointDefinition,
  LectureCheckpointRequirement,
  LectureCheckpointSubmission,
} from "../../features/lectures/types";
import { fetchNodeIoCatalog } from "../../services/api";

type LectureMiniSandboxProps = {
  checkpoint: CheckpointDefinition;
  onVerify: (submission: LectureCheckpointSubmission) => Promise<void>;
  verificationFeedback?: string;
  isVerifying: boolean;
  isCompleted?: boolean;
};

type PaletteDragState = {
  nodeType: string;
  pointerId: number;
  clientX: number;
  clientY: number;
  overCanvas: boolean;
  flowPosition: XYPosition | null;
  canvasZoom: number;
};

const FALLBACK_NODE_CATALOG: NodeIoCatalog = {
  nodes: [
    {
      nodeType: "fetch_price",
      displayName: "Fetch Price Data",
      nodeClass: "fetch",
      theme: "market",
      inputs: [],
      outputs: [{ index: 0, name: "value", arity: "ONE", valueType: "NumVal", valueTypeClass: "number" }],
      dataFields: [
        {
          name: "ticker",
          label: "ticker",
          valueType: "StrVal",
          valueTypeClass: "string",
          required: true,
          defaultValue: "SPY",
        },
        {
          name: "field",
          label: "field",
          valueType: "StrVal",
          valueTypeClass: "string",
          required: true,
          defaultValue: "close",
          options: ["open", "high", "low", "close", "adj_close"],
          readableOptions: ["Open", "High", "Low", "Close", "Adjusted Close"],
        },
        {
          name: "dateBindingMode",
          label: "date binding mode",
          valueType: "StrVal",
          valueTypeClass: "string",
          required: true,
          defaultValue: "simulation_day",
          options: ["simulation_day", "simulation_day_offset", "explicit_date"],
          readableOptions: ["Simulation Day", "Simulation Day Offset", "Set Date"],
        },
        {
          name: "dayOffset",
          label: "day offset",
          valueType: "NumVal",
          valueTypeClass: "number",
          required: false,
          defaultValue: 0,
          visibleWhen: {
            field: "dateBindingMode",
            values: ["simulation_day_offset"],
          },
        },
        {
          name: "date",
          label: "date",
          valueType: "StrVal",
          valueTypeClass: "string",
          required: true,
          defaultValue: "2020-04-27",
          visibleWhen: {
            field: "dateBindingMode",
            values: ["explicit_date"],
          },
        },
      ],
    },
    {
      nodeType: "const_number",
      displayName: "Constant Number",
      nodeClass: "primitive",
      theme: "const",
      inputs: [],
      outputs: [{ index: 0, name: "value", arity: "ONE", valueType: "NumVal", valueTypeClass: "number" }],
      dataFields: [
        {
          name: "value",
          label: "value",
          valueType: "NumVal",
          valueTypeClass: "number",
          required: true,
          defaultValue: 0,
        },
      ],
    },
    {
      nodeType: "buy",
      displayName: "Buy",
      nodeClass: "primitive",
      theme: "market",
      inputs: [{ index: 0, name: "trigger", arity: "ONE", valueType: "BoolVal", valueTypeClass: "boolean" }],
      outputs: [
        { index: 0, name: "executed", arity: "ONE", valueType: "BoolVal", valueTypeClass: "boolean" },
        { index: 1, name: "filled_shares", arity: "ONE", valueType: "NumVal", valueTypeClass: "number" },
      ],
      dataFields: [
        {
          name: "ticker",
          label: "ticker",
          valueType: "StrVal",
          valueTypeClass: "string",
          required: true,
          defaultValue: "AAPL",
        },
        {
          name: "sizeMode",
          label: "size mode",
          valueType: "StrVal",
          valueTypeClass: "string",
          required: true,
          defaultValue: "shares",
          options: ["shares", "dollars"],
          readableOptions: ["Shares", "Dollars"],
        },
        {
          name: "shareQuantity",
          label: "share quantity",
          valueType: "NumVal",
          valueTypeClass: "number",
          required: false,
          defaultValue: 1,
          visibleWhen: {
            field: "sizeMode",
            values: ["shares"],
          },
        },
        {
          name: "dollarAmount",
          label: "dollar amount",
          valueType: "NumVal",
          valueTypeClass: "number",
          required: false,
          defaultValue: 1000,
          visibleWhen: {
            field: "sizeMode",
            values: ["dollars"],
          },
        },
        {
          name: "priceField",
          label: "price field",
          valueType: "StrVal",
          valueTypeClass: "string",
          required: true,
          defaultValue: "close",
          options: ["open", "close", "adj_close"],
          readableOptions: ["Open", "Close", "Adjusted Close"],
        },
      ],
    },
    {
      nodeType: "sell",
      displayName: "Sell",
      nodeClass: "primitive",
      theme: "market",
      inputs: [{ index: 0, name: "trigger", arity: "ONE", valueType: "BoolVal", valueTypeClass: "boolean" }],
      outputs: [
        { index: 0, name: "executed", arity: "ONE", valueType: "BoolVal", valueTypeClass: "boolean" },
        { index: 1, name: "filled_shares", arity: "ONE", valueType: "NumVal", valueTypeClass: "number" },
      ],
      dataFields: [
        {
          name: "ticker",
          label: "ticker",
          valueType: "StrVal",
          valueTypeClass: "string",
          required: true,
          defaultValue: "AAPL",
        },
        {
          name: "sizeMode",
          label: "size mode",
          valueType: "StrVal",
          valueTypeClass: "string",
          required: true,
          defaultValue: "shares",
          options: ["shares", "dollars"],
          readableOptions: ["Shares", "Dollars"],
        },
        {
          name: "shareQuantity",
          label: "share quantity",
          valueType: "NumVal",
          valueTypeClass: "number",
          required: false,
          defaultValue: 1,
          visibleWhen: {
            field: "sizeMode",
            values: ["shares"],
          },
        },
        {
          name: "dollarAmount",
          label: "dollar amount",
          valueType: "NumVal",
          valueTypeClass: "number",
          required: false,
          defaultValue: 1000,
          visibleWhen: {
            field: "sizeMode",
            values: ["dollars"],
          },
        },
        {
          name: "priceField",
          label: "price field",
          valueType: "StrVal",
          valueTypeClass: "string",
          required: true,
          defaultValue: "close",
          options: ["open", "close", "adj_close"],
          readableOptions: ["Open", "Close", "Adjusted Close"],
        },
      ],
    },
    {
      nodeType: "if",
      displayName: "If",
      nodeClass: "primitive",
      theme: "flow",
      inputs: [
        { index: 0, name: "condition", arity: "ONE", valueType: "BoolVal", valueTypeClass: "boolean" },
        { index: 1, name: "when_true", arity: "ONE", valueType: "Value", valueTypeClass: "any" },
        { index: 2, name: "when_false", arity: "ONE", valueType: "Value", valueTypeClass: "any" },
      ],
      outputs: [{ index: 0, name: "result", arity: "ONE", valueType: "Value", valueTypeClass: "any" }],
      dataFields: [],
    },
  ],
};

function buildLectureNode(nodeType: string, position: XYPosition, getDefaultNodeData: (type: string) => NodeData | undefined): Node<NodeData> | null {
  const nodeData = getDefaultNodeData(nodeType);
  if (!nodeData) {
    return null;
  }

  return {
    id: `lecture-node-${crypto.randomUUID()}`,
    type: nodeType,
    position,
    data: nodeData,
    style: { width: SANDBOX_NODE_WIDTH, color: UI_TEXT_PRIMARY },
  };
}

function requirementSatisfied(requirement: LectureCheckpointRequirement, nodes: Node[], edges: Edge[]): boolean {
  if (requirement.type === "node_present") {
    return nodes.some((node) => node.type === requirement.nodeType);
  }

  return edges.some((edge) => {
    const sourceNode = nodes.find((node) => node.id === edge.source);
    const targetNode = nodes.find((node) => node.id === edge.target);
    return sourceNode?.type === requirement.sourceType && targetNode?.type === requirement.targetType;
  });
}

function NodeTemplatePreview({
  node,
  getDefaultNodeData,
  getNodeVisual,
}: {
  node: NodePaletteItem;
  getDefaultNodeData: (type: string) => NodeData | undefined;
  getNodeVisual: (type: string) => {
    background: string;
    border: string;
    title: string;
    sub: string;
    handle: string;
    color: string;
    category: string;
    borderWidth: number;
  } | undefined;
}) {
  const previewData = getDefaultNodeData(node.type);
  if (!previewData) return null;
  const visual = getNodeVisual(node.type) ?? {
    background: UI_CARD,
    border: UI_ACCENT,
    title: UI_TEXT_PRIMARY,
    sub: UI_TEXT_SECONDARY,
    handle: UI_ACCENT,
    color: UI_CARD,
    category: "node",
    borderWidth: 1.5,
  };
  const visiblePreviewFields = previewData.dataFields.filter((field) =>
    isFieldVisible(previewData.nodeType, field, previewData.fieldValues)
  );
  const hasBody = visiblePreviewFields.length > 0;

  return (
    <div
      style={{
        width: "100%",
        padding: 0,
        border: `${visual.borderWidth}px solid ${visual.border}`,
        borderRadius: 10,
        background: visual.background,
        fontFamily: "monospace",
        display: "flex",
        flexDirection: "column",
        position: "relative",
        boxSizing: "border-box",
        overflow: "hidden",
        color: UI_TEXT_PRIMARY,
        boxShadow: "0 8px 18px rgba(0, 0, 0, 0.2)",
        minWidth: 160,
      }}
    >
      {previewData.inputs.length > 0 && (
        <div style={getPreviewHandleStyle("left", visual.handle, previewData.inputs[0])} />
      )}
      <div style={{ padding: "10px 14px 8px" }}>
        <div style={{ fontSize: 12, fontWeight: 500, color: visual.title }}>{previewData.displayName}</div>
        <div
          style={{
            fontSize: 9,
            textTransform: "uppercase",
            letterSpacing: "0.06em",
            color: visual.sub,
            marginTop: 1,
          }}
        >
          {visual.category}
        </div>
      </div>
      {hasBody && (
        <div style={{ padding: "0 10px 10px", display: "flex", flexDirection: "column", gap: 8 }}>
          {visiblePreviewFields.slice(0, 2).map((field) => (
            <div key={field.name} style={{ display: "flex", flexDirection: "column", gap: 4 }}>
              <span
                style={{
                  fontSize: 9,
                  textTransform: "uppercase",
                  letterSpacing: "0.06em",
                  color: visual.sub,
                }}
              >
                {field.label ?? field.name}
              </span>
              <div
                style={{
                  border: "1px solid #2a2a3a",
                  borderRadius: 6,
                  padding: "6px 9px",
                  fontSize: 11,
                  background: UI_CANVAS,
                  color: UI_TEXT_SECONDARY,
                }}
              >
                {String(previewData.fieldValues[field.name] ?? "")}
              </div>
            </div>
          ))}
        </div>
      )}
      {previewData.outputs.length > 0 && (
        <div style={getPreviewHandleStyle("right", visual.handle, previewData.outputs[0])} />
      )}
    </div>
  );
}

function SandboxInner({
  checkpoint,
  onVerify,
  verificationFeedback,
  isVerifying,
  isCompleted = false,
}: LectureMiniSandboxProps) {
  const { fitView, getZoom, screenToFlowPosition } = useReactFlow();
  const [nodeRegistry, setNodeRegistry] = useState(() => createEmptyNodeRegistry());
  const [isNodeCatalogLoading, setIsNodeCatalogLoading] = useState(true);
  const [paletteDrag, setPaletteDrag] = useState<PaletteDragState | null>(null);
  const [catalogMessage, setCatalogMessage] = useState<string | null>(null);
  const [localMessage, setLocalMessage] = useState<string | null>(null);
  const canvasWrapperRef = useRef<HTMLDivElement | null>(null);
  const initializedGraphSignatureRef = useRef<string | null>(null);

  const { nodePalette, nodeTypes, getDefaultNodeData, getNodeVisual, isSupportedNodeType } = nodeRegistry;

  useEffect(() => {
    const abortController = new AbortController();

    const loadCatalog = async () => {
      await Promise.resolve();
      setIsNodeCatalogLoading(true);
      setCatalogMessage(null);

      try {
        const catalog = await fetchNodeIoCatalog(abortController.signal);
        setNodeRegistry(createNodeRegistry(catalog));
      } catch {
        setNodeRegistry(createNodeRegistry(FALLBACK_NODE_CATALOG));
        setCatalogMessage("Using the built-in node catalog for this lecture until the backend catalog is available.");
      } finally {
        if (!abortController.signal.aborted) {
          setIsNodeCatalogLoading(false);
        }
      }
    };

    void loadCatalog();

    return () => {
      abortController.abort();
    };
  }, []);

  const allowedNodePalette = useMemo(
    () => nodePalette.filter((item) => checkpoint.sandboxPreset.allowedNodeTypes.includes(item.type)),
    [checkpoint.sandboxPreset.allowedNodeTypes, nodePalette]
  );

  const initialNodes = useMemo(() => {
    return checkpoint.sandboxPreset.starterNodes
      .map((starterNode) => {
        const node = buildLectureNode(starterNode.type, starterNode.position, getDefaultNodeData);
        if (!node) {
          return null;
        }

        return {
          ...node,
          id: starterNode.id,
        };
      })
      .filter((node): node is Node<NodeData> => node !== null);
  }, [checkpoint.sandboxPreset.starterNodes, getDefaultNodeData]);

  const initialEdges = useMemo(
    () =>
      checkpoint.sandboxPreset.starterEdges.map((edge) => ({
        ...edge,
        type: "smoothstep",
        animated: true,
      })),
    [checkpoint.sandboxPreset.starterEdges]
  );
  const starterGraphSignature = JSON.stringify({
    checkpointId: checkpoint.id,
    starterNodes: checkpoint.sandboxPreset.starterNodes,
    starterEdges: checkpoint.sandboxPreset.starterEdges,
  });

  const [nodes, setNodes] = useState<Node<NodeData>[]>(initialNodes);
  const [edges, setEdges] = useState<Edge[]>(initialEdges);

  useEffect(() => {
    if (isNodeCatalogLoading) {
      return;
    }

    if (initializedGraphSignatureRef.current === starterGraphSignature) {
      return;
    }

    void Promise.resolve().then(() => {
      setNodes(initialNodes);
      setEdges(initialEdges);
      setLocalMessage(null);
      initializedGraphSignatureRef.current = starterGraphSignature;
      window.requestAnimationFrame(() => {
        void fitView({ padding: 0.2, duration: 280 });
      });
    });
  }, [fitView, initialEdges, initialNodes, isNodeCatalogLoading, starterGraphSignature]);

  const taskState = useMemo(
    () =>
      checkpoint.tasks.map((task) => {
        if (isCompleted) {
          return {
            ...task,
            complete: true,
          };
        }

        const taskKey = task.id.replace(/^task-/, "");
        const matchingRequirement = checkpoint.sandboxPreset.requirements?.find((requirement) => {
          if (requirement.type === "node_present") {
            return taskKey.includes(requirement.nodeType.replace(/_/g, "-"));
          }

          return taskKey.includes(
            `${requirement.sourceType.replace(/_/g, "-")}-${requirement.targetType.replace(/_/g, "-")}`
          );
        });

        return {
          ...task,
          complete: matchingRequirement ? requirementSatisfied(matchingRequirement, nodes, edges) : false,
        };
      }),
    [checkpoint.sandboxPreset.requirements, checkpoint.tasks, edges, isCompleted, nodes]
  );
  const allTasksComplete = taskState.length > 0 && taskState.every((task) => task.complete);
  const resolvedFeedback = isCompleted
    ? "Checkpoint complete."
    : verificationFeedback === "Checkpoint verified. The next sublecture is now unlocked."
      ? null
      : verificationFeedback;

  const onNodesChange = useCallback((changes: NodeChange[]) => {
    setNodes((current) => applyNodeChanges(changes, current));
  }, []);

  const onEdgesChange = useCallback((changes: EdgeChange[]) => {
    setEdges((current) => applyEdgeChanges(changes, current));
  }, []);

  const onConnect = useCallback((params: Edge | Connection) => {
    setEdges((current) => addEdge({ ...params, type: "smoothstep", animated: true }, current));
  }, []);

  const tryCreateNode = useCallback(
    (type: string, position: XYPosition) => {
      if (!isSupportedNodeType(type)) {
        setLocalMessage("That node type is not available in this lecture.");
        return false;
      }

      const node = buildLectureNode(type, position, getDefaultNodeData);
      if (!node) {
        setLocalMessage("This node could not be created from the current catalog.");
        return false;
      }

      setNodes((current) => current.concat(node));
      setLocalMessage(`Added ${node.data.displayName}.`);
      return true;
    },
    [getDefaultNodeData, isSupportedNodeType]
  );

  const resolveCanvasDragState = useCallback(
    (nodeType: string, pointerId: number, clientX: number, clientY: number): PaletteDragState => {
      const canvasBounds = canvasWrapperRef.current?.getBoundingClientRect();
      const overCanvas = Boolean(
        canvasBounds &&
          clientX >= canvasBounds.left &&
          clientX <= canvasBounds.right &&
          clientY >= canvasBounds.top &&
          clientY <= canvasBounds.bottom
      );

      return {
        nodeType,
        pointerId,
        clientX,
        clientY,
        overCanvas,
        flowPosition: overCanvas ? screenToFlowPosition({ x: clientX, y: clientY }) : null,
        canvasZoom: overCanvas ? getZoom() : 1,
      };
    },
    [getZoom, screenToFlowPosition]
  );

  const onTemplatePointerDown = useCallback(
    (event: React.PointerEvent, nodeType: string) => {
      if (event.button !== 0) return;
      event.preventDefault();
      setPaletteDrag(resolveCanvasDragState(nodeType, event.pointerId, event.clientX, event.clientY));
    },
    [resolveCanvasDragState]
  );

  useEffect(() => {
    if (!paletteDrag) return;

    const handlePointerMove = (event: PointerEvent) => {
      if (event.pointerId !== paletteDrag.pointerId) return;
      event.preventDefault();
      setPaletteDrag(resolveCanvasDragState(paletteDrag.nodeType, event.pointerId, event.clientX, event.clientY));
    };

    const finishDrag = (event: PointerEvent) => {
      if (event.pointerId !== paletteDrag.pointerId) return;
      event.preventDefault();

      const finalDragState = resolveCanvasDragState(
        paletteDrag.nodeType,
        event.pointerId,
        event.clientX,
        event.clientY
      );

      if (finalDragState.overCanvas && finalDragState.flowPosition) {
        tryCreateNode(finalDragState.nodeType, finalDragState.flowPosition);
      }

      setPaletteDrag(null);
    };

    const cancelDrag = () => {
      setPaletteDrag(null);
    };

    window.addEventListener("pointermove", handlePointerMove, { passive: false });
    window.addEventListener("pointerup", finishDrag, { passive: false });
    window.addEventListener("pointercancel", cancelDrag);

    return () => {
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", finishDrag);
      window.removeEventListener("pointercancel", cancelDrag);
    };
  }, [paletteDrag, resolveCanvasDragState, tryCreateNode]);

  const handleVerify = async () => {
    setLocalMessage(null);
    await onVerify({
      nodes: nodes.map((node) => ({
        id: node.id,
        type: typeof node.type === "string" ? node.type : "unknown",
        label: node.data.displayName,
        position: node.position,
        data: node.data.fieldValues,
      })),
      edges: edges.map((edge) => ({
        id: edge.id,
        source: edge.source,
        target: edge.target,
        sourceHandle: edge.sourceHandle,
        targetHandle: edge.targetHandle,
      })),
      simulation: checkpoint.simulationConfig,
    });
  };

  const activeDraggedNode = paletteDrag
    ? allowedNodePalette.find((node) => node.type === paletteDrag.nodeType)
    : undefined;
  const paletteGhostScale = paletteDrag ? (paletteDrag.overCanvas ? paletteDrag.canvasZoom : 0.78) : 1;

  return (
    <div className="lecture-checkpoint-shell">
      <div className="lecture-checkpoint-topline">
        <div>
          <div className="lecture-checkpoint-label">Mini-Sandbox</div>
          <h3 style={{ margin: "6px 0 0", color: UI_TEXT_PRIMARY, fontSize: 24 }}>{checkpoint.title}</h3>
        </div>
        <div className="lecture-checkpoint-status">Uses the same node system as the main editor</div>
      </div>

      <div className="lecture-checkpoint-instructions">
        {checkpoint.instructions.map((instruction) => (
          <div key={instruction} className="lecture-checkpoint-instruction">
            {instruction}
          </div>
        ))}
      </div>

      <div className="lecture-mini-sandbox-grid">
        <div className="lecture-mini-palette lecture-mini-palette--sandbox">
          <div className="lecture-mini-panel-title">Node palette</div>
          <div className="lecture-mini-palette-buttons lecture-mini-palette-buttons--stacked">
            {allowedNodePalette.map((node) => (
              <div key={node.type} className="lecture-mini-palette-card">
                <div
                  onPointerDown={(event) => onTemplatePointerDown(event, node.type)}
                  className="lecture-mini-palette-draggable"
                  style={{
                    opacity: paletteDrag?.nodeType === node.type ? 0.35 : 1,
                    cursor: "grab",
                  }}
                >
                  <NodeTemplatePreview
                    node={node}
                    getDefaultNodeData={getDefaultNodeData}
                    getNodeVisual={getNodeVisual}
                  />
                </div>
              </div>
            ))}
          </div>

          <div className="lecture-mini-panel-title" style={{ marginTop: 18 }}>
            Tasks
          </div>
          <div className="lecture-mini-task-list">
            {taskState.map((task) => (
              <label key={task.id} className={`lecture-mini-task ${task.complete ? "is-complete" : ""}`}>
                <input type="checkbox" checked={task.complete} readOnly className="lecture-mini-task-checkbox" />
                <div className="lecture-mini-task-copy">
                  <div className="lecture-mini-task-title">{task.label}</div>
                  <div className="lecture-mini-task-description">{task.description}</div>
                </div>
              </label>
            ))}
          </div>

          <div className="lecture-mini-catalog-status">
            {isNodeCatalogLoading ? "Loading node catalog..." : catalogMessage ?? "Drag a node onto the canvas."}
          </div>
        </div>

        <div className="lecture-mini-canvas-card">
          <div ref={canvasWrapperRef} className="lecture-mini-canvas-frame lecture-mini-canvas-frame--editor">
            <ReactFlow
              nodeTypes={nodeTypes}
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              onConnect={onConnect}
              nodeOrigin={[0.5, 0.5]}
              fitView
            >
              <Background color={UI_BORDER_SUBTLE} gap={24} size={1.2} />
              <Controls />
            </ReactFlow>
          </div>

          <div className="lecture-mini-canvas-footer">
            <div className="lecture-mini-feedback">
              {resolvedFeedback ??
                (allTasksComplete
                  ? "All checklist items are complete. Run verification to unlock the next sublecture."
                  : localMessage ?? "Drag real sandbox nodes into the lecture canvas and connect them.")}
            </div>
            <div className="lecture-mini-actions">
              <button
                type="button"
                onClick={() => {
                  setNodes(initialNodes);
                  setEdges(initialEdges);
                  setLocalMessage("Checkpoint sandbox reset to its starting graph.");
                }}
                className="lecture-mini-secondary"
                disabled={isCompleted}
              >
                Reset
              </button>
              <button
                type="button"
                onClick={() => void handleVerify()}
                className="lecture-mini-primary"
                disabled={isVerifying || isCompleted}
              >
                {isCompleted ? "Verified" : isVerifying ? "Verifying..." : "Run verification"}
              </button>
            </div>
          </div>
        </div>
      </div>

      {paletteDrag && activeDraggedNode && (
        <div
          style={{
            position: "fixed",
            left: 0,
            top: 0,
            width: SANDBOX_NODE_WIDTH,
            transform: `translate(${paletteDrag.clientX}px, ${paletteDrag.clientY}px) translate(-50%, -50%) scale(${paletteGhostScale})`,
            transformOrigin: "center center",
            pointerEvents: "none",
            zIndex: 40,
            opacity: paletteDrag.overCanvas ? 0.94 : 0.82,
            transition: "width 140ms ease, opacity 140ms ease, transform 40ms linear",
            filter: paletteDrag.overCanvas
              ? "drop-shadow(0 18px 30px rgba(0, 0, 0, 0.34))"
              : "drop-shadow(0 10px 22px rgba(0, 0, 0, 0.22))",
          }}
        >
          <NodeTemplatePreview
            node={activeDraggedNode}
            getDefaultNodeData={getDefaultNodeData}
            getNodeVisual={getNodeVisual}
          />
        </div>
      )}
    </div>
  );
}

export default function LectureMiniSandbox(props: LectureMiniSandboxProps) {
  return (
    <ReactFlowProvider>
      <SandboxInner {...props} />
    </ReactFlowProvider>
  );
}
