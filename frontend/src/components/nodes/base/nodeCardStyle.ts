export const UI_CANVAS = "#0A0A0F";
export const UI_APP_SHELL = "#111118";
export const UI_PANEL = "#1A1A24";
export const UI_CARD = "#22222F";
export const UI_ELEVATED = "#2C2C3E";
export const UI_BORDER_SUBTLE = "#333344";
export const UI_BORDER_STRONG = "#444466";
export const UI_TEXT_PRIMARY = "#F0F0F8";
export const UI_TEXT_SECONDARY = "#9999BB";
export const UI_TEXT_TERTIARY = "#555577";
export const UI_TEXT_DISABLED = "#3A3A55";
export const UI_ACCENT = "#AFA9EC";

export const NODE_CARD_RADIUS = 20;
export const NODE_CARD_BORDER = `1px solid ${UI_BORDER_SUBTLE}`;
export const NODE_TITLE_PADDING = "14px 16px";
export const NODE_BODY_PADDING = "14px 16px";
export const NODE_TITLE_ALPHA = 0.2;

export const NODE_HANDLE_STYLE: React.CSSProperties = {
  width: 14,
  height: 14,
  borderRadius: 999,
  background: UI_BORDER_STRONG,
  border: `2px solid ${UI_CARD}`,
};

export function withAlpha(color: string, alpha: number): string {
  const normalized = color.trim();
  const clamped = Math.max(0, Math.min(1, alpha));

  if (normalized.startsWith("#")) {
    const hex = normalized.slice(1);
    const expanded =
      hex.length === 3
        ? `${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}`
        : hex.length === 6
        ? hex
        : null;

    if (expanded) {
      const r = parseInt(expanded.slice(0, 2), 16);
      const g = parseInt(expanded.slice(2, 4), 16);
      const b = parseInt(expanded.slice(4, 6), 16);
      return `rgba(${r}, ${g}, ${b}, ${clamped})`;
    }
  }

  if (normalized.startsWith("rgb(")) {
    const values = normalized
      .slice(4, -1)
      .split(",")
      .map((value) => value.trim());
    if (values.length === 3) {
      return `rgba(${values[0]}, ${values[1]}, ${values[2]}, ${clamped})`;
    }
  }

  if (normalized.startsWith("rgba(")) {
    const values = normalized
      .slice(5, -1)
      .split(",")
      .map((value) => value.trim());
    if (values.length >= 3) {
      return `rgba(${values[0]}, ${values[1]}, ${values[2]}, ${clamped})`;
    }
  }

  return normalized;
}
