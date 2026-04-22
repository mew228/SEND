import gettingStarted from "./01-getting-started.md?raw";
import conditionsAndFlow from "./02-conditions-and-flow.md?raw";
import readingTheMap from "./03-reading-the-map.md?raw";
import type { LectureDefinition } from "../../../../features/lectures/types";

const lecture: LectureDefinition = {
  id: "logic--foundations--market-graph-basics",
  slug: "market-graph-basics",
  pathSlug: "logic",
  categorySlug: "foundations",
  title: "Building Your First Decision Graph",
  summary: "Learn how SEND lectures unlock in stages while you build a tiny graph between each sublecture.",
  estimatedMinutes: 12,
  sublectures: [
    {
      id: "getting-started",
      title: "Getting Started",
      contentSource: gettingStarted,
      headings: [
        { id: "getting-started", title: "Getting Started", level: 1 },
        { id: "what-you-are-building", title: "What you are building", level: 2 },
        { id: "why-the-structure-matters", title: "Why the structure matters", level: 2 },
        { id: "before-you-continue", title: "Before you continue", level: 2 },
      ],
      checkpointAfter: {
        id: "checkpoint-place-buy",
        title: "Checkpoint 1: Add Your First Action",
        instructions: [
          "Use the palette to add a Buy node to the mini-sandbox.",
          "Run verification once the required node is on the canvas.",
        ],
        tasks: [
          {
            id: "task-place-buy",
            label: "Add a Buy node",
            description: "Place a Buy node anywhere inside the mini-sandbox canvas.",
          },
        ],
        sandboxPreset: {
          allowedNodeTypes: ["buy", "fetch_price", "const_number"],
          starterNodes: [
            { id: "starter-price", type: "fetch_price", position: { x: 60, y: 120 } },
            { id: "starter-number", type: "const_number", position: { x: 60, y: 250 } },
          ],
          starterEdges: [],
          requirements: [{ type: "node_present", nodeType: "buy" }],
        },
      },
    },
    {
      id: "conditions-and-flow",
      title: "Conditions and Flow",
      contentSource: conditionsAndFlow,
      headings: [
        { id: "conditions-and-flow", title: "Conditions and Flow", level: 1 },
        { id: "decision-nodes", title: "Decision nodes", level: 2 },
        { id: "how-this-affects-lecture-pacing", title: "How this affects lecture pacing", level: 2 },
        { id: "your-next-checkpoint", title: "Your next checkpoint", level: 2 },
      ],
      checkpointAfter: {
        id: "checkpoint-wire-if",
        title: "Checkpoint 2: Connect Logic to Action",
        instructions: [
          "Add an If node if you do not have one yet.",
          "Connect the If node to the Buy node to form a decision path.",
        ],
        tasks: [
          {
            id: "task-place-if",
            label: "Add an If node",
            description: "Place an If node so the graph has a logic gate.",
          },
          {
            id: "task-connect-if-buy",
            label: "Connect If to Buy",
            description: "Create a connection from the If node to the Buy node.",
          },
        ],
        sandboxPreset: {
          allowedNodeTypes: ["buy", "if", "sell", "fetch_price"],
          starterNodes: [{ id: "starter-buy", type: "buy", position: { x: 420, y: 160 } }],
          starterEdges: [],
          requirements: [
            { type: "node_present", nodeType: "if" },
            { type: "connection_present", sourceType: "if", targetType: "buy" },
          ],
        },
      },
    },
    {
      id: "reading-the-map",
      title: "Reading the Map",
      contentSource: readingTheMap,
      headings: [
        { id: "reading-the-map", title: "Reading the Map", level: 1 },
        { id: "what-this-lecture-demonstrated", title: "What this lecture demonstrated", level: 2 },
        { id: "where-this-goes-next", title: "Where this goes next", level: 2 },
        { id: "help-and-review", title: "Help and review", level: 2 },
      ],
    },
  ],
};

export default lecture;
