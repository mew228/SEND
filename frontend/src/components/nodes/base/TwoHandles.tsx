import { Handle, Position } from "reactflow";
import {
  NODE_BODY_PADDING,
  NODE_CARD_BORDER,
  NODE_CARD_RADIUS,
  NODE_HANDLE_STYLE,
  NODE_TITLE_ALPHA,
  NODE_TITLE_PADDING,
  withAlpha,
} from "./nodeCardStyle";

export type NodeData<TExtra = Record<string, unknown>> = {
  label: string;
  extra?: TExtra;
};

export default function BaseNode({
  data,
  accent,
  color = "white",
  children,
}: {
  data: NodeData;
  accent: string;
  color?: string;
  children?: React.ReactNode;
}) {
  const hasBody = children !== undefined && children !== null;

  return (
    <div
      style={{
        width: "100%",
        border: NODE_CARD_BORDER,
        borderRadius: NODE_CARD_RADIUS,
        background: color,
        fontFamily: "system-ui, sans-serif",
        display: "flex",
        flexDirection: "column",
        position: "relative",
        overflow: "hidden",
      }}
    >
      <Handle
        type="target"
        position={Position.Left}
        style={{ ...NODE_HANDLE_STYLE, left: -8 }}
      />

      <div
        style={{
          padding: NODE_TITLE_PADDING,
          background: withAlpha(accent, NODE_TITLE_ALPHA),
          borderBottom: hasBody ? "1px solid #e5e7eb" : undefined,
        }}
      >
        <div style={{ fontSize: 14, fontWeight: 700, lineHeight: 1.1 }}>
          {data.label}
        </div>
      </div>

      {hasBody && (
        <div style={{ padding: NODE_BODY_PADDING, display: "flex", alignItems: "center" }}>
          {children}
        </div>
      )}

      <Handle
        type="source"
        position={Position.Right}
        style={{ ...NODE_HANDLE_STYLE, right: -8 }}
      />
    </div>
  );
}
