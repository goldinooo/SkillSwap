"use client"

import Link from "next/link"
import { CheckIcon } from "@phosphor-icons/react"
import { Button } from "@workspace/ui/components/button"
import { Card } from "@workspace/ui/components/card"
import { cn } from "@workspace/ui/lib/utils"

import type { TeacherSkill } from "../../../discover/_components/discover-data"
import { TeacherCard } from "../../../discover/_components/teacher-card"
import {
  MOCK_AVAILABILITY,
  MOCK_LESSON_PRICING,
  MOCK_TEACHER_PROFILE,
} from "./create-lesson-data"
import type { LessonFormInput } from "./lesson-schema"

function CardLabel({ children }: { children: React.ReactNode }) {
  return (
    <p className="text-[11px] font-medium tracking-[0.18em] text-(--chart-3) uppercase">
      {children}
    </p>
  )
}

export function LessonPreview({ values }: { values: LessonFormInput }) {
  const firstSlot = MOCK_AVAILABILITY[0]

  // Shape the draft like a Discover entry so the real card can render it.
  const preview: TeacherSkill = {
    id: "preview",
    name: MOCK_TEACHER_PROFILE.name,
    initials: MOCK_TEACHER_PROFILE.initials,
    level: MOCK_TEACHER_PROFILE.level,
    category: values.category ?? "Frontend",
    title: values.title?.trim() || "Your lesson title",
    description:
      values.description?.trim() || "A short description of your lesson.",
    duration: "Open time",
    medium: "Video",
    schedule: firstSlot ? `${firstSlot.day} ${firstSlot.from}` : "No slots yet",
    points: MOCK_LESSON_PRICING.points,
    xp: MOCK_LESSON_PRICING.xp,
  }

  return (
    <Card className="gap-4 px-(--card-spacing)">
      <div className="flex flex-col gap-1">
        <CardLabel>Preview</CardLabel>
        <p className="text-sm text-muted-foreground">
          How learners will see your lesson in Discover.
        </p>
      </div>
      {/* inert: it's only a preview, its Request button must not be usable */}
      <div inert>
        <TeacherCard teacher={preview} />
      </div>
    </Card>
  )
}

export function PublishChecklist({
  steps,
  isSubmitting,
  onSaveDraft,
}: {
  steps: { id: string; label: string; done: boolean }[]
  isSubmitting: boolean
  onSaveDraft: () => void
}) {
  const done = steps.filter((step) => step.done).length
  const ready = done === steps.length
  const firstMissing = steps.find((step) => !step.done)

  return (
    <Card className="gap-4 px-(--card-spacing)">
      <div className="flex items-center justify-between">
        <h2 className="font-medium">Ready to publish?</h2>
        <span className="font-heading text-sm text-muted-foreground">
          {done} of {steps.length}
        </span>
      </div>

      <div
        className="h-1.5 overflow-hidden rounded-full bg-muted"
        role="progressbar"
        aria-label="Publish progress"
        aria-valuemin={0}
        aria-valuemax={steps.length}
        aria-valuenow={done}
      >
        <div
          className="h-full rounded-full bg-primary transition-[width]"
          style={{ width: `${(done / steps.length) * 100}%` }}
        />
      </div>

      <ul className="flex flex-col gap-2.5">
        {steps.map((step) => (
          <li
            key={step.id}
            className={cn(
              "flex items-center gap-2.5 text-sm",
              !step.done && "text-muted-foreground"
            )}
          >
            <span
              className={cn(
                "flex size-4 shrink-0 items-center justify-center rounded-full",
                step.done
                  ? "bg-primary text-primary-foreground"
                  : "border border-muted-foreground"
              )}
            >
              {step.done && <CheckIcon className="size-2.5" weight="bold" />}
            </span>
            {step.label}
            <span className="sr-only">{step.done ? "(done)" : "(missing)"}</span>
          </li>
        ))}
      </ul>

      <div className="flex flex-col gap-2">
        <Button
          type="submit"
          size="lg"
          disabled={!ready || isSubmitting}
          className="h-10"
        >
          {isSubmitting ? "Publishing…" : "Publish lesson"}
        </Button>
        <Button
          type="button"
          variant="outline"
          size="lg"
          className="h-10 bg-transparent"
          onClick={onSaveDraft}
          disabled={isSubmitting}
        >
          Save draft
        </Button>
      </div>

      <p className="text-xs text-muted-foreground" aria-live="polite">
        {ready
          ? "Everything looks good. You can publish now."
          : `Complete "${firstMissing?.label}" to publish.`}
      </p>
    </Card>
  )
}

export function AvailabilityList() {
  return (
    <div className="overflow-hidden rounded-lg border border-border">
      <ul className="divide-y divide-border">
        {MOCK_AVAILABILITY.map((slot) => (
          <li
            key={slot.day}
            className="flex items-center gap-4 px-4 py-3 text-sm"
          >
            <span className="w-8 font-heading font-medium">{slot.day}</span>
            <span className="text-muted-foreground">
              {slot.from} to {slot.to}
            </span>
          </li>
        ))}
      </ul>
      <div className="flex flex-wrap items-center justify-between gap-3 bg-muted/50 px-4 py-3">
        <p className="text-sm text-muted-foreground">
          Learners can book you in these slots. Sessions have no fixed length.
        </p>
        <Button
          variant="outline"
          render={<Link href="/dashboard/calendar" />}
          nativeButton={false}
        >
          Edit in Calendar
        </Button>
      </div>
    </div>
  )
}

export function SectionHeader({
  step,
  title,
  description,
}: {
  step?: number
  title: string
  description: string
}) {
  return (
    <div className="flex flex-col gap-1">
      <h2 className="flex items-center gap-2.5 font-heading text-base font-semibold">
        {step !== undefined && (
          <span className="flex size-6 items-center justify-center rounded-full bg-muted text-xs">
            {step}
          </span>
        )}
        {title}
      </h2>
      <p className="text-sm text-muted-foreground">{description}</p>
    </div>
  )
}
