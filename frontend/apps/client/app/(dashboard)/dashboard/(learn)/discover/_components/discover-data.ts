// Mock data for the discover page. Swap for API data later.

export const MOCK_AVAILABLE_POINTS = 128

export const CATEGORIES = [
  "All",
  "Frontend",
  "Backend",
  "Design",
  "Languages",
] as const

export type Category = Exclude<(typeof CATEGORIES)[number], "All">

export interface TeacherSkill {
  id: string
  name: string
  initials: string
  level: number
  category: Category
  title: string
  description: string
  duration: string
  medium: "Video" | "In person"
  schedule: string
  points: number
  xp: number
}

export const MOCK_TEACHERS: TeacherSkill[] = [
  {
    id: "t-1",
    name: "Sara K.",
    initials: "SK",
    level: 9,
    category: "Backend",
    title: "Python for beginners",
    description:
      "Variables, loops and your first scripts, learned through small projects.",
    duration: "1 h",
    medium: "Video",
    schedule: "Thu 20:30",
    points: 10,
    xp: 30,
  },
  {
    id: "t-2",
    name: "Karim D.",
    initials: "KD",
    level: 6,
    category: "Design",
    title: "Figma basics",
    description: "Frames, auto layout and components, building a simple app screen.",
    duration: "1 h",
    medium: "Video",
    schedule: "Mon 19:00",
    points: 8,
    xp: 30,
  },
  {
    id: "t-3",
    name: "Lina M.",
    initials: "LM",
    level: 8,
    category: "Backend",
    title: "Docker basics",
    description: "Images, containers and compose, packaging a small web app.",
    duration: "1 h",
    medium: "Video",
    schedule: "Sat 10:00",
    points: 5,
    xp: 30,
  },
  {
    id: "t-4",
    name: "Nadia T.",
    initials: "NT",
    level: 12,
    category: "Backend",
    title: "System design",
    description: "Design a scalable service step by step: load, caching and databases.",
    duration: "1 h",
    medium: "Video",
    schedule: "Wed 18:00",
    points: 6,
    xp: 30,
  },
  {
    id: "t-5",
    name: "Reda L.",
    initials: "RL",
    level: 5,
    category: "Backend",
    title: "SQL fundamentals",
    description: "Queries, joins and filters, practised on a real dataset.",
    duration: "1 h",
    medium: "Video",
    schedule: "Fri 17:00",
    points: 4,
    xp: 30,
  },
  {
    id: "t-6",
    name: "Hind A.",
    initials: "HA",
    level: 7,
    category: "Languages",
    title: "Technical writing",
    description: "Write clear docs and READMEs that other developers actually read.",
    duration: "1 h",
    medium: "Video",
    schedule: "Sun 11:00",
    points: 6,
    xp: 30,
  },
]
