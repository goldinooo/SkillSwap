import { z } from "zod"

import { CATEGORIES } from "../../../discover/_components/discover-data"

export const LESSON_CATEGORIES = CATEGORIES.filter(
  (category) => category !== "All"
) as Exclude<(typeof CATEGORIES)[number], "All">[]

export const LESSON_LEVELS = ["Beginner", "Intermediate", "Advanced"] as const

export const SESSION_TOOLS = ["video", "chat", "code", "canvas"] as const

export type SessionTool = (typeof SESSION_TOOLS)[number]

export const MAX_OUTCOMES = 4
export const DESCRIPTION_MAX = 160

const titleSchema = z
  .string()
  .trim()
  .min(3, "Give your lesson a title of at least 3 characters.")
  .max(60, "Keep the title under 60 characters.")

const categorySchema = z.enum(LESSON_CATEGORIES, {
  error: "Pick a category.",
})

const descriptionSchema = z
  .string()
  .trim()
  .min(20, "Describe the lesson in at least 20 characters.")
  .max(DESCRIPTION_MAX, `Keep it under ${DESCRIPTION_MAX} characters.`)

const levelSchema = z.enum(LESSON_LEVELS, { error: "Pick a level." })

const outcomesSchema = z
  .array(
    z.object({
      text: z.string().trim().max(80, "Keep outcomes under 80 characters."),
    })
  )
  .min(1)
  .max(MAX_OUTCOMES, `Add up to ${MAX_OUTCOMES} outcomes.`)
  .refine((outcomes) => (outcomes[0]?.text.trim().length ?? 0) > 0, {
    path: [0, "text"],
    message: "Add at least one learning outcome.",
  })

const toolsSchema = z
  .array(z.enum(SESSION_TOOLS))
  .min(1, "Turn on at least one session tool.")

export const lessonSchema = z.object({
  title: titleSchema,
  category: categorySchema,
  description: descriptionSchema,
  level: levelSchema,
  outcomes: outcomesSchema,
  prerequisites: z
    .string()
    .trim()
    .max(120, "Keep prerequisites under 120 characters."),
  tools: toolsSchema,
})

/** Raw form state (before trimming) */
export type LessonFormInput = z.input<typeof lessonSchema>
/** Validated, trimmed values ready to send to the API */
export type LessonFormValues = z.output<typeof lessonSchema>

/** Per-step checks that drive the "Ready to publish?" checklist. */
export const PUBLISH_STEPS = [
  {
    id: "basics",
    label: "Title and category",
    schema: z.object({ title: titleSchema, category: categorySchema }),
  },
  {
    id: "details",
    label: "Description and level",
    schema: z.object({ description: descriptionSchema, level: levelSchema }),
  },
  {
    id: "schedule",
    label: "Schedule and format",
    schema: z.object({ tools: toolsSchema }),
  },
  {
    id: "outcomes",
    label: "At least one learning outcome",
    schema: z.object({ outcomes: outcomesSchema }),
  },
] as const
