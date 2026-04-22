import { useEffect, useState } from "react";
import { Link, Navigate, useParams } from "react-router-dom";
import AuthPanel from "../components/auth/AuthPanel";
import LectureMiniSandbox from "../components/lectures/LectureMiniSandbox";
import MarkdownContent from "../components/lectures/MarkdownContent";
import { slugifyMarkdownHeading } from "../components/lectures/markdown-utils";
import type { LectureDetailResponse, LectureProgress } from "../features/lectures/types";
import { fetchLectureBySlug, verifyLectureCheckpoint } from "../services/lectureApi";
import "./LecturePage.css";

type TocItem = {
  id: string;
  title: string;
  locked: boolean;
  completed: boolean;
  current: boolean;
  kind: "sublecture" | "heading";
};

function getSublectureSectionId(index: number, title: string): string {
  return `sublecture-${index}-${slugifyMarkdownHeading(title)}`;
}

function getHashAnchorId(): string | null {
  const rawHash = window.location.hash;
  if (!rawHash || rawHash === "#") {
    return null;
  }

  const anchorId = decodeURIComponent(rawHash.slice(1)).trim();
  return anchorId.length > 0 ? anchorId : null;
}

export default function LecturePage() {
  const { pathSlug = "", categorySlug = "", lectureSlug = "" } = useParams();
  const [lectureDetail, setLectureDetail] = useState<LectureDetailResponse | null>(null);
  const [progress, setProgress] = useState<LectureProgress | null>(null);
  const [isLectureLoading, setIsLectureLoading] = useState(true);
  const [lectureError, setLectureError] = useState<string | null>(null);
  const [isVerifyingCheckpoint, setIsVerifyingCheckpoint] = useState(false);
  const [activeAnchorId, setActiveAnchorId] = useState<string | null>(null);
  const [recentlyUnlockedIndex, setRecentlyUnlockedIndex] = useState<number | null>(null);

  useEffect(() => {
    if (!pathSlug || !categorySlug || !lectureSlug) {
      return;
    }

    let isActive = true;
    setIsLectureLoading(true);
    setLectureError(null);
    setLectureDetail(null);
    setProgress(null);
    setRecentlyUnlockedIndex(null);

    void fetchLectureBySlug(pathSlug, categorySlug, lectureSlug)
      .then((detail) => {
        if (!isActive) {
          return;
        }

        setLectureDetail(detail);
        setProgress(detail.progress);
        setActiveAnchorId(getSublectureSectionId(0, detail.sublectures[0]?.title ?? "section"));
      })
      .catch((error: unknown) => {
        if (!isActive) {
          return;
        }

        setLectureError(error instanceof Error ? error.message : "The lecture could not be loaded.");
      })
      .finally(() => {
        if (isActive) {
          setIsLectureLoading(false);
        }
      });

    return () => {
      isActive = false;
    };
  }, [pathSlug, categorySlug, lectureSlug]);

  useEffect(() => {
    const anchorElements = Array.from(document.querySelectorAll<HTMLElement>("[data-lecture-anchor='true']"));
    if (anchorElements.length === 0) {
      return;
    }

    let frameId: number | null = null;

    const updateActiveAnchor = () => {
      frameId = null;

      const topOffset = Math.min(180, Math.max(96, window.innerHeight * 0.18));
      let nextActiveElement = anchorElements[0] ?? null;

      for (const element of anchorElements) {
        const rect = element.getBoundingClientRect();
        if (rect.top - topOffset <= 0) {
          nextActiveElement = element;
          continue;
        }
        break;
      }

      if (nextActiveElement?.id) {
        setActiveAnchorId((current) => (current === nextActiveElement.id ? current : nextActiveElement.id));
      }
    };

    const scheduleUpdate = () => {
      if (frameId !== null) {
        return;
      }
      frameId = window.requestAnimationFrame(updateActiveAnchor);
    };

    scheduleUpdate();
    window.addEventListener("scroll", scheduleUpdate, { passive: true });
    window.addEventListener("resize", scheduleUpdate);

    return () => {
      if (frameId !== null) {
        window.cancelAnimationFrame(frameId);
      }
      window.removeEventListener("scroll", scheduleUpdate);
      window.removeEventListener("resize", scheduleUpdate);
    };
  }, [lectureDetail, progress?.highestUnlockedSublectureIndex]);

  useEffect(() => {
    if (!lectureDetail || !progress) {
      return;
    }

    const requestedAnchorId = getHashAnchorId();
    if (!requestedAnchorId) {
      return;
    }

    const frameId = window.requestAnimationFrame(() => {
      const element = document.getElementById(requestedAnchorId);
      if (!element) {
        return;
      }

      setActiveAnchorId(requestedAnchorId);
      element.scrollIntoView({ behavior: "auto", block: "start" });
    });

    return () => {
      window.cancelAnimationFrame(frameId);
    };
  }, [lectureDetail, progress]);

  if (!pathSlug || !categorySlug || !lectureSlug) {
    return <Navigate to="/library" replace />;
  }

  if (!isLectureLoading && (!lectureDetail || !progress)) {
    return <Navigate to="/library" replace />;
  }

  if (!lectureDetail || !progress) {
    return (
      <div className="lecture-page">
        <div className="lecture-page__container">
          <div className="lecture-inline-warning">{lectureError ?? "Loading lecture..."}</div>
        </div>
      </div>
    );
  }

  const highestUnlockedIndex = Math.min(
    Math.max(progress.highestUnlockedSublectureIndex, 0),
    lectureDetail.sublectures.length - 1
  );
  const unlockedSublectures = lectureDetail.sublectures.slice(0, highestUnlockedIndex + 1);
  const totalMilestones =
    lectureDetail.sublectures.length + lectureDetail.sublectures.filter((item) => item.checkpointAfter).length;
  const completedMilestones =
    Math.min(highestUnlockedIndex + 1, lectureDetail.sublectures.length) + progress.completedCheckpointIds.length;
  const progressPercent = Math.round((completedMilestones / totalMilestones) * 100);

  const tocItems: TocItem[] = lectureDetail.sublectures.flatMap((sublecture, index) => {
    const sectionId = getSublectureSectionId(index, sublecture.title);
    const locked = index > highestUnlockedIndex;
    const completed =
      index < highestUnlockedIndex || progress.completedCheckpointIds.includes(sublecture.checkpointAfter?.id ?? "");
    const mainItem: TocItem = {
      id: sectionId,
      title: sublecture.title,
      locked,
      completed,
      current: activeAnchorId === sectionId || Boolean(activeAnchorId?.startsWith(`${sectionId}--`)),
      kind: "sublecture",
    };

    const headingItems = sublecture.headings.map((heading) => ({
      id: heading.id,
      title: heading.title,
      locked,
      completed,
      current: activeAnchorId === heading.id,
      kind: "heading" as const,
    }));

    return [mainItem, ...headingItems];
  });

  const scrollToAnchor = (anchorId: string, locked: boolean) => {
    if (locked) {
      return;
    }

    const element = document.getElementById(anchorId);
    if (!element) {
      return;
    }

    setActiveAnchorId(anchorId);
    element.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  const handleVerifyCheckpoint = async (
    checkpointId: string,
    submission: {
      nodes: { id: string; type: string; label?: string; position: { x: number; y: number } }[];
      edges: { id: string; source: string; target: string }[];
    }
  ) => {
    setIsVerifyingCheckpoint(true);
    try {
      const previousHighestUnlockedIndex = progress.highestUnlockedSublectureIndex;
      const result = await verifyLectureCheckpoint(lectureDetail.id, checkpointId, submission);
      const refreshedLectureDetail = await fetchLectureBySlug(pathSlug, categorySlug, lectureSlug);

      setLectureDetail(refreshedLectureDetail);
      setProgress(refreshedLectureDetail.progress);
      if (result.passed && result.newlyUnlockedSublectureIndex > previousHighestUnlockedIndex) {
        setRecentlyUnlockedIndex(result.newlyUnlockedSublectureIndex);
      }
    } catch (error: unknown) {
      setLectureError(error instanceof Error ? error.message : "Checkpoint verification failed.");
    } finally {
      setIsVerifyingCheckpoint(false);
    }
  };

  return (
    <div className="lecture-page">
      <div className="lecture-page__container">
        <div className="lecture-topbar">
          <Link to={`/library?path=${lectureDetail.pathSlug}`} className="lecture-back-button">
            <span aria-hidden="true">&lt;-</span>
            <span>Back</span>
          </Link>

          <div style={{ minWidth: 220 }}>
            <AuthPanel compact />
          </div>

          <div className="lecture-progress-shell">
            <div className="lecture-progress-shell__eyebrow">
              {lectureDetail.path.title} / {lectureDetail.category.title}
            </div>
            <div className="lecture-progress-shell__header">
              <div>
                <h1 className="lecture-progress-shell__title">{lectureDetail.title}</h1>
                <div className="lecture-progress-shell__meta">
                  {lectureDetail.estimatedMinutes} min lecture | {completedMilestones}/{totalMilestones} milestones
                  completed
                </div>
              </div>
              <div className="lecture-badge">{progressPercent}% unlocked</div>
            </div>

            <div className="lecture-progress-track" aria-hidden="true">
              <div className="lecture-progress-fill" style={{ width: `${progressPercent}%` }} />
            </div>
            <div className="lecture-progress-caption">
              <span>{lectureDetail.category.description ?? lectureDetail.summary}</span>
              <span>{isLectureLoading ? "Restoring saved progress..." : "Saved progress restored automatically"}</span>
            </div>
          </div>
        </div>

        <div className="lecture-mobile-nav">
          <select
            value={activeAnchorId ?? tocItems[0]?.id ?? ""}
            onChange={(event) => {
              const selectedItem = tocItems.find((item) => item.id === event.target.value);
              if (!selectedItem) return;
              scrollToAnchor(selectedItem.id, selectedItem.locked);
            }}
          >
            {tocItems.map((item) => (
              <option key={item.id} value={item.id} disabled={item.locked}>
                {item.locked ? "Locked | " : ""}
                {item.kind === "heading" ? "> " : ""}
                {item.title}
              </option>
            ))}
          </select>
        </div>

        <div className="lecture-layout">
          <aside className="lecture-sidebar">
            <div className="lecture-sidebar-card">
              <h2 className="lecture-sidebar-title">Table of contents</h2>
              {tocItems.map((item, index) => (
                <button
                  key={item.id}
                  type="button"
                  className={`lecture-sidebar-item${item.current ? " is-current" : ""}${item.locked ? " is-locked" : ""}${
                    item.kind === "heading" ? " is-heading" : ""
                  }`}
                  disabled={item.locked}
                  onClick={() => scrollToAnchor(item.id, item.locked)}
                >
                  <span className="lecture-sidebar-index">
                    {item.kind === "heading"
                      ? "."
                      : index + 1 - tocItems.slice(0, index).filter((entry) => entry.kind === "heading").length}
                  </span>
                  <span className="lecture-sidebar-label">{item.title}</span>
                  <span className="lecture-sidebar-status">
                    {item.locked ? "Locked" : item.current ? "Open" : item.completed ? "Done" : "Ready"}
                  </span>
                </button>
              ))}
            </div>
          </aside>

          <main className="lecture-content-column">
            <section className="lecture-content-card lecture-content-card--summary">
              <div className="lecture-content-meta">
                <span>{lectureDetail.summary}</span>
                <span>
                  {unlockedSublectures.length} of {lectureDetail.sublectures.length} sublectures visible
                </span>
              </div>

              {lectureError && <div className="lecture-inline-warning">{lectureError}</div>}
            </section>

            <div className="lecture-sublecture-stack">
              {unlockedSublectures.map((sublecture, index) => {
                const sectionId = getSublectureSectionId(index, sublecture.title);
                const checkpoint = sublecture.checkpointAfter;
                const checkpointCompleted = checkpoint ? progress.completedCheckpointIds.includes(checkpoint.id) : false;
                const checkpointFeedback = checkpoint ? progress.activeCheckpointState[checkpoint.id]?.lastFeedback : undefined;
                const shouldAnimateUnlock = recentlyUnlockedIndex === index;

                return (
                  <div key={sublecture.id} className="lecture-flow-block">
                    <section
                      id={sectionId}
                      data-lecture-anchor="true"
                      className={`lecture-content-card lecture-sublecture-card lecture-anchor-target${
                        shouldAnimateUnlock ? " is-newly-unlocked" : ""
                      }`}
                    >
                      <div className="lecture-sublecture-meta">
                        <div className="lecture-badge">Sublecture {index + 1}</div>
                        <div className="lecture-sublecture-title">{sublecture.title}</div>
                      </div>

                      <MarkdownContent source={sublecture.contentSource ?? ""} sectionId={sectionId} />
                    </section>

                    {checkpoint && (
                      <section className="lecture-content-card lecture-practice-card">
                        <LectureMiniSandbox
                          checkpoint={checkpoint}
                          onVerify={(submission) => handleVerifyCheckpoint(checkpoint.id, submission)}
                          verificationFeedback={checkpointFeedback}
                          isVerifying={isVerifyingCheckpoint}
                          isCompleted={checkpointCompleted}
                        />
                      </section>
                    )}
                  </div>
                );
              })}
            </div>
          </main>
        </div>
      </div>
    </div>
  );
}
