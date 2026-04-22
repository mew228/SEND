import { useEffect } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Landing from "./pages/Landing";
import Library from "./pages/Library";
import Sandbox from './pages/Sandbox';
import LecturePage from "./pages/LecturePage";

const UMAMI_SCRIPT_SELECTOR = 'script[data-umami-analytics="true"]';
const UMAMI_LOG_PREFIX = "[umami]";
const ANALYTICS_CONFIG_URL = "/api/analytics/config";

type AnalyticsConfig = {
  enabled: boolean;
  scriptUrl: string;
  collectUrl: string;
  websiteId: string;
};

function App() {
  useEffect(() => {
    const existingScript = document.head.querySelector<HTMLScriptElement>(UMAMI_SCRIPT_SELECTOR);

    if (existingScript) {
      console.info(`${UMAMI_LOG_PREFIX} analytics script already present`);
      return;
    }

    const abortController = new AbortController();

    const bootstrapAnalytics = async () => {
      try {
        const response = await fetch(ANALYTICS_CONFIG_URL, {
          credentials: "same-origin",
          signal: abortController.signal,
        });

        if (!response.ok) {
          if (response.status === 404) {
            console.warn(`${UMAMI_LOG_PREFIX} analytics disabled: backend analytics endpoint unavailable`);
            return;
          }
          throw new Error(`analytics config request failed with status ${response.status}`);
        }

        const config = (await response.json()) as Partial<AnalyticsConfig>;
        const enabled = config.enabled === true;
        const scriptUrl = config.scriptUrl?.trim() ?? "";
        const collectUrl = config.collectUrl?.trim() ?? "";
        const websiteId = config.websiteId?.trim() ?? "";
        const hostUrl = collectUrl ? new URL(collectUrl, window.location.origin).origin : "";

        if (!enabled) {
          console.warn(`${UMAMI_LOG_PREFIX} analytics disabled: backend configuration incomplete`);
          return;
        }

        if (!scriptUrl || !collectUrl || !websiteId || !hostUrl) {
          throw new Error("analytics config response was missing required fields");
        }

        const injectedScript = document.head.querySelector<HTMLScriptElement>(UMAMI_SCRIPT_SELECTOR);
        if (injectedScript) {
          console.info(`${UMAMI_LOG_PREFIX} analytics script already present`);
          return;
        }

        const script = document.createElement("script");
        script.defer = true;
        script.src = scriptUrl;
        script.dataset.hostUrl = hostUrl;
        script.dataset.websiteId = websiteId;
        script.dataset.umamiAnalytics = "true";
        script.addEventListener("load", () => {
          console.info(`${UMAMI_LOG_PREFIX} analytics script loaded successfully`, {
            hostUrl,
            scriptUrl,
            collectUrl,
            websiteId,
          });
        });
        script.addEventListener("error", () => {
          console.error(`${UMAMI_LOG_PREFIX} failed to load analytics script`, {
            hostUrl,
            scriptUrl,
            collectUrl,
            websiteId,
          });
        });

        console.info(`${UMAMI_LOG_PREFIX} injecting analytics script`, {
          hostUrl,
          scriptUrl,
          collectUrl,
          websiteId,
        });
        document.head.appendChild(script);
      } catch (error) {
        if (abortController.signal.aborted) {
          return;
        }

        const message = error instanceof Error ? error.message : String(error);
        console.info(`${UMAMI_LOG_PREFIX} analytics disabled: config bootstrap failed`, {
          error: message,
        });
      }
    };

    void bootstrapAnalytics();

    return () => {
      abortController.abort();
    };
  }, []);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/library" element={<Library />} />
        <Route path="/library/:pathSlug/:categorySlug/:lectureSlug" element={<LecturePage />} />
        <Route path="/sandbox" element={<Sandbox />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
