import foundationsLecture from "./foundations/market-graph-basics/metadata";
import type {
  LectureCatalogPath,
  LectureCatalogResponse,
  LectureCategory,
  LectureDefinition,
  LecturePath,
} from "../../features/lectures/types";

const lecturePaths: LecturePath[] = [
  {
    slug: "logic",
    title: "Logic",
    description: "Reasoning-focused lessons and graph-building fundamentals.",
  },
  {
    slug: "economics",
    title: "Economics",
    description: "Market and economic intuition organized into guided lecture categories.",
  },
];

const lectureCategoriesByPath: Record<string, LectureCategory[]> = {
  logic: [
    {
      slug: "getting-started",
      title: "Getting Started",
    },
  ],
  economics: [
    {
      slug: "getting-started",
      title: "Getting Started",
    },
  ],
};

const lectureDefinitions: LectureDefinition[] = [foundationsLecture];

export function getLectureDefinitions(): LectureDefinition[] {
  return lectureDefinitions;
}

export function getLectureDefinition(
  pathSlug: string,
  categorySlug: string,
  lectureSlug: string
): LectureDefinition | undefined {
  return lectureDefinitions.find(
    (lecture) => lecture.pathSlug === pathSlug && lecture.categorySlug === categorySlug && lecture.slug === lectureSlug
  );
}

export function getLectureCatalog(): LectureCatalogResponse {
  const paths: LectureCatalogPath[] = lecturePaths.map((path) => ({
    ...path,
    categories: (lectureCategoriesByPath[path.slug] ?? []).map((category) => ({
      ...category,
      lectures: lectureDefinitions
        .filter((lecture) => lecture.pathSlug === path.slug && lecture.categorySlug === category.slug)
        .map((lecture) => ({
          id: lecture.id,
          slug: lecture.slug,
          pathSlug: lecture.pathSlug,
          categorySlug: lecture.categorySlug,
          title: lecture.title,
          summary: lecture.summary,
          estimatedMinutes: lecture.estimatedMinutes,
        })),
    })),
  }));

  return { paths };
}

export function getLectureCategory(pathSlug: string, slug: string): LectureCategory | undefined {
  return (lectureCategoriesByPath[pathSlug] ?? []).find((category) => category.slug === slug);
}

export function getLecturePath(slug: string): LecturePath | undefined {
  return lecturePaths.find((path) => path.slug === slug);
}
