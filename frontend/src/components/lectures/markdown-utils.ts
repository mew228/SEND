type MarkdownBlock =
  | { type: "heading1"; text: string }
  | { type: "heading2"; text: string }
  | { type: "ordered"; items: string[] }
  | { type: "unordered"; items: string[] }
  | { type: "paragraph"; text: string };

export type MarkdownHeading = {
  id: string;
  title: string;
  level: 2;
};

export function slugifyMarkdownHeading(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, "")
    .trim()
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-");
}

export function parseMarkdown(source: string): MarkdownBlock[] {
  const lines = source.split(/\r?\n/);
  const blocks: MarkdownBlock[] = [];
  let index = 0;

  while (index < lines.length) {
    const line = lines[index]?.trim() ?? "";
    if (line.length === 0) {
      index += 1;
      continue;
    }

    if (line.startsWith("# ")) {
      blocks.push({ type: "heading1", text: line.slice(2).trim() });
      index += 1;
      continue;
    }

    if (line.startsWith("## ")) {
      blocks.push({ type: "heading2", text: line.slice(3).trim() });
      index += 1;
      continue;
    }

    if (line.startsWith("- ")) {
      const items: string[] = [];
      while (index < lines.length && (lines[index]?.trim() ?? "").startsWith("- ")) {
        items.push((lines[index] ?? "").trim().slice(2).trim());
        index += 1;
      }
      blocks.push({ type: "unordered", items });
      continue;
    }

    if (/^\d+\.\s/.test(line)) {
      const items: string[] = [];
      while (index < lines.length && /^\d+\.\s/.test((lines[index]?.trim() ?? ""))) {
        items.push((lines[index] ?? "").trim().replace(/^\d+\.\s/, ""));
        index += 1;
      }
      blocks.push({ type: "ordered", items });
      continue;
    }

    const paragraphLines = [line];
    index += 1;
    while (index < lines.length) {
      const nextLine = lines[index]?.trim() ?? "";
      if (
        nextLine.length === 0 ||
        nextLine.startsWith("# ") ||
        nextLine.startsWith("## ") ||
        nextLine.startsWith("- ") ||
        /^\d+\.\s/.test(nextLine)
      ) {
        break;
      }
      paragraphLines.push(nextLine);
      index += 1;
    }
    blocks.push({ type: "paragraph", text: paragraphLines.join(" ") });
  }

  return blocks;
}

export function extractMarkdownHeadings(source: string, sectionId: string): MarkdownHeading[] {
  const blocks = parseMarkdown(source);

  return blocks.flatMap((block) => {
    if (block.type !== "heading2") {
      return [];
    }

    return [
      {
        id: `${sectionId}--${slugifyMarkdownHeading(block.text)}`,
        title: block.text,
        level: 2 as const,
      },
    ];
  });
}
