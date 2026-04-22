import { useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import { AUTH_PROVIDER_CONFIG } from "../../auth/providerConfig";
import { useAuth } from "../../auth/AuthContext";

type AuthMode = "sign-in" | "sign-up" | "reset" | "update-password";

type AuthPanelProps = {
  compact?: boolean;
};

function formatAuthError(error: unknown): string {
  if (error instanceof Error && error.message.length > 0) {
    return error.message;
  }
  return "That request could not be completed.";
}

export default function AuthPanel({ compact = false }: AuthPanelProps) {
  const {
    isConfigured,
    isLoading,
    isRecoveryMode,
    user,
    signIn,
    signUp,
    signOut,
    requestPasswordReset,
    updatePassword,
  } = useAuth();

  const wrapperRef = useRef<HTMLDivElement | null>(null);
  const dropdownRef = useRef<HTMLDivElement | null>(null);
  const [isOpen, setIsOpen] = useState(!compact);
  const [mode, setMode] = useState<AuthMode>("sign-in");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const resolvedMode: AuthMode = isRecoveryMode ? "update-password" : mode;

  const resetMessages = () => {
    setStatusMessage(null);
    setErrorMessage(null);
  };

  useEffect(() => {
    if (!compact || !isOpen) return;

    const handlePointerDown = (event: PointerEvent) => {
      if (!wrapperRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    };

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [compact, isOpen]);

  const handleSubmit = async () => {
    resetMessages();
    setIsSubmitting(true);

    try {
      if (resolvedMode === "sign-in") {
        await signIn(email.trim(), password);
        setStatusMessage("You are signed in.");
        setPassword("");
      } else if (resolvedMode === "sign-up") {
        const nextMessage = await signUp(email.trim(), password);
        setStatusMessage(nextMessage ?? "Account created and signed in.");
        setPassword("");
        setConfirmPassword("");
      } else if (resolvedMode === "reset") {
        await requestPasswordReset(email.trim());
        setStatusMessage("Password reset instructions have been sent.");
      } else {
        if (password !== confirmPassword) {
          throw new Error("Passwords do not match.");
        }
        await updatePassword(password);
        setStatusMessage("Your password has been updated.");
        setPassword("");
        setConfirmPassword("");
        setMode("sign-in");
      }
    } catch (error) {
      setErrorMessage(formatAuthError(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  const onModeChange = (nextMode: AuthMode) => {
    resetMessages();
    setMode(nextMode);
  };

  const renderBody = () => {
    if (!isConfigured) {
      return <div style={bodyTextStyle}>Supabase auth is not configured for this frontend build.</div>;
    }

    if (isLoading) {
      return <div style={bodyTextStyle}>Checking your session...</div>;
    }

    if (user) {
      return (
        <div style={{ display: "grid", gap: 10 }}>
          <div style={bodyTextStyle}>Signed in as {user.email ?? user.id}</div>
          {isRecoveryMode && (
            <>
              <div style={labelStyle}>New password</div>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                style={inputStyle}
              />
              <div style={labelStyle}>Confirm password</div>
              <input
                type="password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                style={inputStyle}
              />
              <button type="button" onClick={() => void handleSubmit()} disabled={isSubmitting} style={primaryButtonStyle}>
                {isSubmitting ? "Updating..." : "Update password"}
              </button>
            </>
          )}
          {!isRecoveryMode && (
            <button
              type="button"
              onClick={() => void signOut().catch((error) => setErrorMessage(formatAuthError(error)))}
              style={secondaryButtonStyle}
            >
              Sign out
            </button>
          )}
        </div>
      );
    }

    return (
      <div style={{ display: "grid", gap: 10 }}>
        {resolvedMode !== "update-password" && (
          <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
            <button type="button" onClick={() => onModeChange("sign-in")} style={mode === "sign-in" ? activeModeButtonStyle : modeButtonStyle}>
              Sign in
            </button>
            <button type="button" onClick={() => onModeChange("sign-up")} style={mode === "sign-up" ? activeModeButtonStyle : modeButtonStyle}>
              Create account
            </button>
          </div>
        )}

        <div style={labelStyle}>Email</div>
        <input
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          style={inputStyle}
          autoComplete="email"
        />

        {resolvedMode !== "reset" && (
          <>
            <div style={labelStyle}>{resolvedMode === "update-password" ? "New password" : "Password"}</div>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              style={inputStyle}
              autoComplete={resolvedMode === "sign-in" ? "current-password" : "new-password"}
            />
            {resolvedMode === "sign-in" && (
              <button
                type="button"
                onClick={() => onModeChange("reset")}
                style={forgotPasswordStyle}
              >
                Forgot password?
              </button>
            )}
          </>
        )}

        {resolvedMode === "sign-up" && (
          <>
            <div style={labelStyle}>Confirm password</div>
            <input
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              style={inputStyle}
              autoComplete="new-password"
            />
          </>
        )}

        <button type="button" onClick={() => void handleSubmit()} disabled={isSubmitting} style={primaryButtonStyle}>
          {isSubmitting
            ? "Working..."
            : resolvedMode === "sign-in"
              ? "Sign in"
              : resolvedMode === "sign-up"
                ? "Create account"
                : resolvedMode === "reset"
                  ? "Send reset email"
                  : "Update password"}
        </button>

        {AUTH_PROVIDER_CONFIG.oauthProviders.length > 0 && (
          <div style={{ display: "grid", gap: 6 }}>
            {AUTH_PROVIDER_CONFIG.oauthProviders.map((provider) => (
              <button key={provider.id} type="button" disabled style={secondaryButtonStyle}>
                {provider.label} coming soon
              </button>
            ))}
          </div>
        )}
      </div>
    );
  };

  const dropdownId = useMemo(() => "auth-panel-dropdown", []);

  const panelContent = (
    <div style={{ display: "grid", gap: 10 }}>
      {renderBody()}
      {statusMessage && <div style={{ ...bodyTextStyle, color: "#8FD4A8" }}>{statusMessage}</div>}
      {errorMessage && <div style={{ ...bodyTextStyle, color: "#F58C8C" }}>{errorMessage}</div>}
    </div>
  );

  if (compact) {
    return (
      <div ref={wrapperRef} style={{ position: "relative", minWidth: 220 }}>
        <div style={{ display: "flex", justifyContent: "flex-end" }}>
          <button
            type="button"
            onClick={() => setIsOpen((current) => !current)}
            style={triggerButtonStyle}
            aria-expanded={isOpen}
            aria-controls={dropdownId}
          >
            {user ? "Account" : "Sign in"}
          </button>
        </div>

        <div
          id={dropdownId}
          ref={dropdownRef}
          style={{
            position: "absolute",
            top: "calc(100% + 22px)",
            right: -16,
            zIndex: 20,
            minWidth: 220,
            padding: "14px 16px",
            borderRadius: 16,
            border: "1px solid rgba(144, 151, 169, 0.25)",
            background: "rgba(12, 16, 24, 0.94)",
            color: "#F2F4F8",
            boxShadow: "0 12px 36px rgba(0, 0, 0, 0.35)",
            opacity: isOpen ? 1 : 0,
            pointerEvents: isOpen ? "auto" : "none",
            transform: isOpen ? "translateY(0)" : "translateY(-6px)",
            transition: "opacity 150ms ease, transform 150ms ease",
          }}
        >
          {panelContent}
        </div>
      </div>
    );
  }

  return (
    <div
      ref={wrapperRef}
      style={{
        minWidth: 220,
        padding: "14px 16px",
        borderRadius: 16,
        border: "1px solid rgba(144, 151, 169, 0.25)",
        background: "rgba(12, 16, 24, 0.88)",
        color: "#F2F4F8",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 10 }}>
        <div>
          <div style={{ fontSize: 11, letterSpacing: "0.08em", textTransform: "uppercase", color: "#9BA8BC" }}>
            Account
          </div>
          <div style={{ fontSize: 13, fontWeight: 600 }}>
            {user ? user.email ?? "Signed in" : "Sign in to save your work"}
          </div>
        </div>
      </div>

      <div style={{ display: "grid", gap: 10, marginTop: 12 }}>
        {panelContent}
      </div>
    </div>
  );
}

const bodyTextStyle: CSSProperties = {
  fontSize: 12,
  lineHeight: 1.5,
  color: "#C5CEDB",
};

const labelStyle: CSSProperties = {
  fontSize: 11,
  fontWeight: 700,
  letterSpacing: "0.08em",
  textTransform: "uppercase",
  color: "#9BA8BC",
};

const inputStyle: CSSProperties = {
  width: "100%",
  boxSizing: "border-box",
  borderRadius: 10,
  border: "1px solid rgba(144, 151, 169, 0.25)",
  background: "rgba(19, 24, 36, 0.95)",
  color: "#F2F4F8",
  padding: "10px 12px",
  fontSize: 13,
};

const baseButtonStyle: CSSProperties = {
  borderRadius: 10,
  padding: "9px 12px",
  fontSize: 12,
  fontWeight: 700,
  border: "1px solid rgba(144, 151, 169, 0.25)",
  cursor: "pointer",
};

const primaryButtonStyle: CSSProperties = {
  ...baseButtonStyle,
  background: "#E7B95B",
  color: "#171A22",
};

const secondaryButtonStyle: CSSProperties = {
  ...baseButtonStyle,
  background: "rgba(19, 24, 36, 0.95)",
  color: "#F2F4F8",
};

const modeButtonStyle: CSSProperties = {
  ...secondaryButtonStyle,
  padding: "6px 10px",
};

const activeModeButtonStyle: CSSProperties = {
  ...modeButtonStyle,
  borderColor: "rgba(231, 185, 91, 0.75)",
  color: "#E7B95B",
};

const triggerButtonStyle: CSSProperties = {
  ...baseButtonStyle,
  padding: "8px 14px",
  background: "#E7B95B",
  color: "#171A22",
  borderColor: "rgba(231, 185, 91, 0.75)",
  boxShadow: "0 8px 18px rgba(0,0,0,0.2)",
};

const forgotPasswordStyle: CSSProperties = {
  alignSelf: "flex-start",
  marginTop: 6,
  marginBottom: 0,
  padding: 0,
  background: "transparent",
  border: "none",
  color: "#E7B95B",
  fontSize: 11,
  fontWeight: 700,
  letterSpacing: "0.04em",
  lineHeight: 1.4,
  cursor: "pointer",
};
