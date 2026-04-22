import { Link } from "react-router-dom";
import PageShell from "./PageShell";
import {
  UI_ACCENT,
  UI_BORDER_SUBTLE,
  UI_CARD,
  UI_TEXT_PRIMARY,
  UI_TEXT_SECONDARY,
  withAlpha,
} from "../components/nodes/base/nodeCardStyle";

const highlights = [
  {
    title: "Visual strategy building",
    description: "Compose market logic with nodes instead of wiring everything by hand in code.",
  },
  {
    title: "Automate With Data",
    description: "The sandbox contains real-world historical data to automate historical daily data.",
  },
  {
    title: "Simulated Environment",
    description: "The environment is custom-built and simulated end-to-end.",
  },
];

export default function Landing() {
  return (
    <PageShell
      title="Build and learn trading logic."
      //description="SEND combines a node-based sandbox with a future learning path for financial concepts. The editor is already live, and the learning experience is scaffolded here as a polished placeholder for deeper implementation later."
      maxWidth={1320}
    >
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: 96,
        }}
      >
        <section
          style={{
            display: "flex",
            gap: 14,
            flexWrap: "wrap",
            alignItems: "center",
          }}
        >
          <Link
            to="/library/logic/getting-started/what-is-send#sublecture-0-what-send-means"
            style={{
              padding: "12px 18px",
              borderRadius: 14,
              background: UI_ACCENT,
              color: "#111118",
              fontWeight: 700,
              textDecoration: "none",
            }}
          >
            What is SEND
          </Link>
          <Link
            to="/sandbox?highlight=test-strategy"
            style={{
              padding: "12px 18px",
              borderRadius: 14,
              border: `1px solid ${UI_BORDER_SUBTLE}`,
              background: UI_CARD,
              color: UI_TEXT_PRIMARY,
              fontWeight: 700,
              textDecoration: "none",
            }}
          >
            Try a strategy
          </Link>
        </section>

        <section
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
            gap: 24,
            rowGap: 40,
          }}
        >
          {highlights.map((item) => (
            <article
              key={item.title}
              style={{
                padding: 20,
                borderRadius: 18,
                border: `1px solid ${UI_BORDER_SUBTLE}`,
                background: UI_CARD,
                boxShadow: `0 10px 28px ${withAlpha("#000000", 0.18)}`,
              }}
            >
              <h2 style={{ margin: 0, fontSize: 20, color: UI_TEXT_PRIMARY }}>{item.title}</h2>
              <p style={{ margin: "10px 0 0", color: UI_TEXT_SECONDARY, lineHeight: 1.6 }}>
                {item.description}
              </p>
            </article>
          ))}
        </section>
      </div>
    </PageShell>
  );
}
