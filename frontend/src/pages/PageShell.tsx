import { Link, useLocation } from "react-router-dom";
import AuthPanel from "../components/auth/AuthPanel";
import {
  UI_ACCENT,
  UI_APP_SHELL,
  UI_BORDER_STRONG,
  UI_BORDER_SUBTLE,
  UI_CARD,
  UI_ELEVATED,
  UI_PANEL,
  UI_TEXT_PRIMARY,
  UI_TEXT_SECONDARY,
  withAlpha,
} from "../components/nodes/base/nodeCardStyle";

type PageShellProps = {
  title: string;
  eyebrow?: string;
  description?: string;
  children: React.ReactNode;
  maxWidth?: number;
};

const navItems = [
  { to: "/", label: "Home" },
  { to: "/library", label: "Learning" },
  { to: "/sandbox", label: "Sandbox" },
];

export default function PageShell({ title, eyebrow, description, children, maxWidth = 1120 }: PageShellProps) {
  const location = useLocation();

  return (
    <div
      style={{
        minHeight: "100vh",
        background: `radial-gradient(circle at top, ${withAlpha(UI_ACCENT, 0.14)}, transparent 32%), ${UI_APP_SHELL}`,
        color: UI_TEXT_PRIMARY,
      }}
    >
      <div
        style={{
          maxWidth,
          margin: "0 auto",
          padding: "24px 20px 64px",
          boxSizing: "border-box",
        }}
      >
        <header
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: 20,
            marginBottom: 40,
            padding: "14px 18px",
            border: `1px solid ${UI_BORDER_SUBTLE}`,
            borderRadius: 18,
            background: withAlpha(UI_PANEL, 0.92),
            backdropFilter: "blur(12px)",
            flexWrap: "wrap",
          }}
        >
          <div>
            <div style={{ fontSize: 12, letterSpacing: "0.14em", textTransform: "uppercase", color: UI_TEXT_SECONDARY }}>
              SEND
            </div>
            <div style={{ fontSize: 14, color: UI_TEXT_PRIMARY, marginTop: 2 }}>
              Strategic Economic Node Development System
            </div>
          </div>

          <nav style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            {navItems.map((item) => {
              const isActive = location.pathname === item.to;
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  style={{
                    padding: "9px 14px",
                    borderRadius: 999,
                    border: `1px solid ${isActive ? UI_BORDER_STRONG : UI_BORDER_SUBTLE}`,
                    background: isActive ? UI_ELEVATED : UI_CARD,
                    color: isActive ? UI_TEXT_PRIMARY : UI_TEXT_SECONDARY,
                    textDecoration: "none",
                    fontSize: 13,
                    fontWeight: 600,
                  }}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>

          <div style={{ position: "relative" }}>
            <AuthPanel compact />
          </div>
        </header>

        <section style={{ marginBottom: 36 }}>
          {eyebrow && (
            <div style={{ fontSize: 12, letterSpacing: "0.14em", textTransform: "uppercase", color: UI_ACCENT, marginBottom: 10 }}>
              {eyebrow}
            </div>
          )}
          <h1
            style={{
              margin: 0,
              fontSize: "clamp(2.5rem, 7vw, 4.5rem)",
              lineHeight: 0.95,
              color: UI_TEXT_PRIMARY,
            }}
          >
            {title}
          </h1>
          {description && (
            <p
              style={{
                margin: "16px 0 0",
                maxWidth: 760,
                color: UI_TEXT_SECONDARY,
                fontSize: 18,
                lineHeight: 1.6,
              }}
            >
              {description}
            </p>
          )}
        </section>

        {children}
      </div>
    </div>
  );
}
