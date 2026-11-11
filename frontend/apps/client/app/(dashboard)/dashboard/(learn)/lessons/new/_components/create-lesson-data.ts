// Mock data for the create lesson page. Swap for API data later.

import type { SessionTool } from "./lesson-schema"

export const MOCK_TEACHER_PROFILE = {
  name: "Amina",
  initials: "AM",
  level: 7,
}

export const MOCK_LESSON_PRICING = {
  points: 10,
  xp: 60,
}

export const MOCK_AVAILABILITY = [
  { day: "Tue", from: "18:00", to: "21:00" },
  { day: "Thu", from: "18:00", to: "21:00" },
  { day: "Sat", from: "10:00", to: "13:00" },
]

export const MOCK_WEEKLY_POINTS = {
  earned: 30,
  cap: 50,
}

export const SESSION_TOOL_OPTIONS: Record<
  SessionTool,
  { label: string; description: string }
> = {
  video: { label: "Video call", description: "Talk face to face" },
  chat: { label: "Chat", description: "Messages and links" },
  code: { label: "Code editor", description: "Write and run code together" },
  canvas: {
    label: "Collaborative canvas",
    description: "Sketch and diagram together",
  },
}
