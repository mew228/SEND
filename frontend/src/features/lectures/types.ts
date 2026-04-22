export type LecturePath = {
  slug: string;
  title: string;
  description?: string;
};

export type LectureCategory = {
  slug: string;
  title: string;
  description?: string | null;
  hero?: string;
};

export type LectureSandboxNodeType = string;
export type LectureNodeFieldValue = string | number | boolean | null;

export type LectureSandboxNode = {
  id: string;
  type: LectureSandboxNodeType;
  label?: string;
  position: {
    x: number;
    y: number;
  };
  data?: Record<string, LectureNodeFieldValue>;
};

export type LectureSandboxEdge = {
  id: string;
  source: string;
  target: string;
  sourceHandle?: string | null;
  targetHandle?: string | null;
};

export type LectureCheckpointTask = {
  id: string;
  label: string;
  description: string;
};

export type LectureHeading = {
  id: string;
  title: string;
  level: number;
};

export type LectureCheckpointRequirement =
  | {
      type: "node_present";
      nodeType: string;
    }
  | {
      type: "connection_present";
      sourceType: string;
      targetType: string;
    };

export type LectureSandboxPreset = {
  allowedNodeTypes: LectureSandboxNodeType[];
  starterNodes: LectureSandboxNode[];
  starterEdges: LectureSandboxEdge[];
  requirements?: LectureCheckpointRequirement[];
};

export type CheckpointDefinition = {
  id: string;
  title: string;
  instructions: string[];
  tasks: LectureCheckpointTask[];
  sandboxPreset: LectureSandboxPreset;
  simulationConfig?: {
    initialCash: number;
    includeTrace?: boolean | null;
  };
};

export type SublectureDefinition = {
  id: string;
  title: string;
  contentSource: string | null;
  headings: LectureHeading[];
  checkpointAfter?: CheckpointDefinition;
  unlocked?: boolean;
};

export type LectureDefinition = {
  id: string;
  slug: string;
  pathSlug: string;
  categorySlug: string;
  title: string;
  summary: string;
  estimatedMinutes: number;
  sublectures: SublectureDefinition[];
};

export type LectureDetailResponse = LectureDefinition & {
  path: LecturePath;
  category: LectureCategory;
  progress: LectureProgress;
};

export type LectureCatalogItem = Pick<
  LectureDefinition,
  "id" | "slug" | "pathSlug" | "categorySlug" | "title" | "summary" | "estimatedMinutes"
>;

export type LectureCatalogCategory = LectureCategory & {
  lectures: LectureCatalogItem[];
};

export type LectureCatalogPath = LecturePath & {
  categories: LectureCatalogCategory[];
};

export type LectureCatalogResponse = {
  paths: LectureCatalogPath[];
};

export type LectureCheckpointSubmission = {
  nodes: LectureSandboxNode[];
  edges: LectureSandboxEdge[];
  simulation?: CheckpointDefinition["simulationConfig"];
};

export type LectureCheckpointVerificationResult = {
  passed: boolean;
  feedback: string;
  newlyUnlockedSublectureIndex: number;
  completedCheckpointIds: string[];
  newlyUnlockedSublecture?: SublectureDefinition | null;
};

export type LectureProgress = {
  lectureId: string;
  highestUnlockedSublectureIndex: number;
  completedCheckpointIds: string[];
  activeCheckpointState: Record<
    string,
    {
      lastFeedback?: string;
      lastAttemptedAt?: string;
      passed: boolean;
    }
  >;
};

export type LectureSectionNavItem = {
  id: string;
  title: string;
  index: number;
  locked: boolean;
  completed: boolean;
  current: boolean;
};
