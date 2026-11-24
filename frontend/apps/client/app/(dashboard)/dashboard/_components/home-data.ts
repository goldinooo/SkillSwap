// Mock data for the dashboard home page. Swap these out for API data later.

export type SessionKind = "teaching" | "learning"

export interface NextSession {
  title: string
  startTime: string
  endTime: string
  partner: { name: string; firstName: string }
  medium: string
  kind: SessionKind
  points: number
  xp: number
}

export interface AttentionItem {
  id: string
  type: "request" | "confirmation"
  person: string
  title: string
  detail: string
}

export interface HomeStats {
  points: { balance: number; earned: number; spent: number }
  level: { current: number; xp: number; nextLevelXp: number; streakDays: number }
  rank: { position: number; weeklyXp: number }
}

export interface UpcomingSession {
  id: string
  day: string
  time: string
  title: string
  partner: string
  kind: SessionKind
  points: number
}

export interface Recommendation {
  id: string
  title: string
  teacher: string
  initials: string
  cost: number
}

export const MOCK_USER = { firstName: "Amina" }

export const MOCK_NEXT_SESSION: NextSession = {
  title: "Git workflow",
  startTime: "16:00",
  endTime: "17:00",
  partner: { name: "Sara K.", firstName: "Sara" },
  medium: "video call",
  kind: "learning",
  points: 10,
  xp: 30,
}

export const MOCK_ATTENTION: AttentionItem[] = [
  {
    id: "req-1",
    type: "request",
    person: "Omar R.",
    title: "wants to learn CSS Grid",
    detail: "Wed, Nov 11 · 14:00 · you earn 5 pts",
  },
  {
    id: "conf-1",
    type: "confirmation",
    person: "Hind A.",
    title: "Confirm your session with",
    detail: "Confirming releases +5 pts",
  },
]

export const MOCK_STATS: HomeStats = {
  points: { balance: 128, earned: 45, spent: 30 },
  level: { current: 7, xp: 2340, nextLevelXp: 3000, streakDays: 5 },
  rank: { position: 14, weeklyXp: 540 },
}

export const MOCK_UPCOMING: UpcomingSession[] = [
  {
    id: "s-1",
    day: "Fri",
    time: "15:00",
    title: "Next.js Q&A",
    partner: "Youssef B.",
    kind: "teaching",
    points: 10,
  },
  {
    id: "s-2",
    day: "Tue",
    time: "10:00",
    title: "React mentoring",
    partner: "Reda L.",
    kind: "teaching",
    points: 10,
  },
  {
    id: "s-3",
    day: "Thu",
    time: "14:00",
    title: "TypeScript study",
    partner: "Karim D.",
    kind: "learning",
    points: 10,
  },
]

export const MOCK_RECOMMENDATIONS: Recommendation[] = [
  {
    id: "r-1",
    title: "Docker basics",
    teacher: "Lina M.",
    initials: "LM",
    cost: 5,
  },
  {
    id: "r-2",
    title: "System design",
    teacher: "Nadia T.",
    initials: "NT",
    cost: 6,
  },
]
