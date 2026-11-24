// Mock data for the my lessons page. Swap for API data later.

export const LESSON_FILTERS = ["All", "Active", "Drafts", "Paused"] as const

export type LessonFilter = (typeof LESSON_FILTERS)[number]

export type LessonStatus = "active" | "draft" | "paused"

export type LessonMedium = "Video" | "Chat" | "In person"

export type LessonNotice =
  | { kind: "booking"; label: string }
  | { kind: "request"; label: string }
  | { kind: "unpublished"; label: string }

export interface Lesson {
  id: string
  title: string
  description: string
  category: string
  status: LessonStatus
  duration: string
  medium: LessonMedium
  schedule: string
  points: number
  xp: number
  notice: LessonNotice
}

export const MOCK_LESSONS: Lesson[] = [
  {
    id: "lesson-1",
    title: "React mentoring",
    description:
      "Components, state and hooks, taught through a small app.",
    category: "Frontend",
    status: "active",
    duration: "1 h",
    medium: "Video",
    schedule: "Tue, Thu",
    points: 10,
    xp: 40,
    notice: { kind: "booking", label: "Next booking: Tue 10:00" },
  },
  {
    id: "lesson-2",
    title: "Next.js Q&A",
    description:
      "Bring your questions on routing, data fetching and deployment.",
    category: "Frontend",
    status: "active",
    duration: "1 h",
    medium: "Video",
    schedule: "Fri",
    points: 10,
    xp: 40,
    notice: { kind: "booking", label: "Next booking: Today 19:00" },
  },
  {
    id: "lesson-3",
    title: "CSS Grid",
    description: "Build responsive layouts with grid, one step at a time.",
    category: "Frontend",
    status: "active",
    duration: "30 min",
    medium: "Chat",
    schedule: "Wed",
    points: 5,
    xp: 40,
    notice: { kind: "request", label: "1 request waiting for your answer" },
  },
  {
    id: "lesson-4",
    title: "Intro to TypeScript",
    description:
      "Types, interfaces and generics, building a small typed utility library.",
    category: "Frontend",
    status: "draft",
    duration: "1 h",
    medium: "Video",
    schedule: "Tue, Thu",
    points: 10,
    xp: 40,
    notice: { kind: "unpublished", label: "Not published yet" },
  },
]

export const MOCK_WEEKLY_CAPACITY = {
  used: 3,
  cap: 5,
  nextLevel: 8,
  nextCap: 6,
  xpToNextLevel: 660,
  learningXp: 60,
  teachingXp: 40,
}
