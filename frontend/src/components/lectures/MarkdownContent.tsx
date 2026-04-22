import type { CSSProperties, ReactNode } from "react";
import { UI_ACCENT, UI_TEXT_PRIMARY, UI_TEXT_SECONDARY } from "../nodes/base/nodeCardStyle";
import { parseMarkdown, slugifyMarkdownHeading } from "./markdown-utils";

type InlineToken =
  | { type: "text"; value: string }
  | { type: "code"; value: string }
  | { type: "strong"; value: string };

function tokenizeInlineText(text: string): InlineToken[] {
  const tokens: InlineToken[] = [];
  let cursor = 0;

  while (cursor < text.length) {
    const codeStart = text.indexOf("`", cursor);
    const boldStart = text.indexOf("**", cursor);

    let nextStart = -1;
    let nextType: InlineToken["type"] | null = null;

    if (codeStart !== -1 && (boldStart === -1 || codeStart < boldStart)) {
      nextStart = codeStart;
      nextType = "code";
    } else if (boldStart !== -1) {
      nextStart = boldStart;
      nextType = "strong";
    }

    if (nextStart === -1 || nextType === null) {
      tokens.push({ type: "text", value: text.slice(cursor) });
      break;
    }

    if (nextStart > cursor) {
      tokens.push({ type: "text", value: text.slice(cursor, nextStart) });
    }

    if (nextType === "code") {
      const codeEnd = text.indexOf("`", nextStart + 1);
      if (codeEnd === -1) {
        tokens.push({ type: "text", value: text.slice(nextStart) });
        break;
      }

      tokens.push({ type: "code", value: text.slice(nextStart + 1, codeEnd) });
      cursor = codeEnd + 1;
      continue;
    }

    const boldEnd = text.indexOf("**", nextStart + 2);
    if (boldEnd === -1) {
      tokens.push({ type: "text", value: text.slice(nextStart) });
      break;
    }

    tokens.push({ type: "strong", value: text.slice(nextStart + 2, boldEnd) });
    cursor = boldEnd + 2;
  }

  return tokens.filter((token) => token.value.length > 0);
}

function renderInlineText(text: string): ReactNode[] {
  const tokens = tokenizeInlineText(text);

  return tokens.map((token, index) => {
    if (token.type === "code") {
      return (
        <code
          key={`${token.type}-${token.value}-${index}`}
          style={{
            padding: "2px 6px",
            borderRadius: 7,
            background: "rgba(175, 169, 236, 0.1)",
            color: UI_ACCENT,
            fontSize: "0.92em",
          }}
        >
          {token.value}
        </code>
      );
    }

    if (token.type === "strong") {
      return (
        <strong key={`${token.type}-${token.value}-${index}`} style={{ color: UI_TEXT_PRIMARY, fontWeight: 700 }}>
          {token.value}
        </strong>
      );
    }

    return <span key={`${token.type}-${token.value}-${index}`}>{token.value}</span>;
  });
}

const paragraphStyle: CSSProperties = {
  margin: "0 0 18px",
  color: UI_TEXT_SECONDARY,
  lineHeight: 1.8,
  fontSize: 17,
};

export default function MarkdownContent({
  source,
  sectionId,
}: {
  source: string;
  sectionId: string;
}) {
  const blocks = parseMarkdown(source);

  return (
    <div>
      {blocks.map((block, index) => {
        if (block.type === "heading1") {
          return (
            <h1
              key={`${block.type}-${index}`}
              style={{
                margin: "0 0 18px",
                fontSize: "clamp(2.6rem, 6vw, 4.8rem)",
                lineHeight: 0.98,
                color: UI_TEXT_PRIMARY,
              }}
            >
              {renderInlineText(block.text)}
            </h1>
          );
        }

        if (block.type === "heading2") {
          const headingId = `${sectionId}--${slugifyMarkdownHeading(block.text)}`;
          return (
            <h2
              key={`${block.type}-${index}`}
              id={headingId}
              data-lecture-anchor="true"
              className="lecture-anchor-target"
              style={{
                margin: "28px 0 12px",
                fontSize: 30,
                lineHeight: 1.1,
                color: UI_TEXT_PRIMARY,
              }}
            >
              {renderInlineText(block.text)}
            </h2>
          );
        }

        if (block.type === "unordered") {
          return (
            <ul
              key={`${block.type}-${index}`}
              style={{
                margin: "0 0 18px 20px",
                color: UI_TEXT_SECONDARY,
                lineHeight: 1.8,
                fontSize: 17,
                padding: 0,
              }}
            >
              {block.items.map((item) => (
                <li key={item} style={{ marginBottom: 8 }}>
                  {renderInlineText(item)}
                </li>
              ))}
            </ul>
          );
        }

        if (block.type === "ordered") {
          return (
            <ol
              key={`${block.type}-${index}`}
              style={{
                margin: "0 0 18px 20px",
                color: UI_TEXT_SECONDARY,
                lineHeight: 1.8,
                fontSize: 17,
                padding: 0,
              }}
            >
              {block.items.map((item) => (
                <li key={item} style={{ marginBottom: 8 }}>
                  {renderInlineText(item)}
                </li>
              ))}
            </ol>
          );
        }

        return (
          <p key={`${block.type}-${index}`} style={paragraphStyle}>
            {renderInlineText(block.text)}
          </p>
        );
      })}
    </div>
  );
}
