import Link from "next/link"
import {
  CalendarBlankIcon,
  CalendarCheckIcon,
  ChatCircleIcon,
  ClockIcon,
  EyeSlashIcon,
  PlusIcon,
  VideoCameraIcon,
} from "@phosphor-icons/react/ssr"
import { Badge } from "@workspace/ui/components/badge"
import { Button } from "@workspace/ui/components/button"
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@workspace/ui/components/card"
import { cn } from "@workspace/ui/lib/utils"

import type { Lesson, LessonStatus } from "./lessons-data"

const statusStyles: Record<LessonStatus, { label: string; dot: string }> = {
  active: { label: "Active", dot: "bg-accent" },
  draft: { label: "Draft", dot: "border border-muted-foreground" },
  paused: { label: "Paused", dot: "bg-muted-foreground" },
}

const noticeIcons = {
  booking: CalendarCheckIcon,
  request: ChatCircleIcon,
  unpublished: EyeSlashIcon,
}

export function LessonCard({ lesson }: { lesson: Lesson }) {
  const status = statusStyles[lesson.status]
  const NoticeIcon = noticeIcons[lesson.notice.kind]
  const isDraft = lesson.status === "draft"

  return (
    <Card>
      <CardHeader className="grid-cols-[1fr_auto] items-center gap-2">
        <Badge className="bg-primary/20 px-2.5 text-(--chart-3)">
          {lesson.category}
        </Badge>
        <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
          <span className={cn("size-2 rounded-full", status.dot)} />
          {status.label}
        </span>
        <CardTitle className="col-span-2 mt-3 text-xl font-semibold">
          {lesson.title}
        </CardTitle>
      </CardHeader>

      <CardContent className="gap-4">
        <p className="text-sm text-muted-foreground">{lesson.description}</p>

        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1.5">
            <ClockIcon className="size-3.5" />
            {lesson.duration}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <VideoCameraIcon className="size-3.5" />
            {lesson.medium}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <CalendarBlankIcon className="size-3.5" />
            {lesson.schedule}
          </span>
        </div>

        <p className="flex items-center gap-2 rounded-lg bg-muted px-3 py-2.5 text-sm">
          <NoticeIcon className="size-4 shrink-0 text-muted-foreground" />
          {lesson.notice.label}
        </p>
      </CardContent>

      <CardFooter className="gap-2 border-t border-border pt-(--card-spacing)">
        <span className="font-heading font-semibold">{lesson.points} pts</span>
        <Badge className="h-6 bg-accent px-2.5 text-accent-foreground">
          +{lesson.xp} XP
        </Badge>
        <Button
          variant={isDraft ? "default" : "outline"}
          className="ml-auto"
          aria-label={
            isDraft ? `Finish ${lesson.title}` : `Edit ${lesson.title}`
          }
        >
          {isDraft ? "Finish" : "Edit"}
        </Button>
      </CardFooter>
    </Card>
  )
}

export function CreateLessonCard() {
  return (
    <Link
      href="/dashboard/lessons/new"
      className="group flex min-h-64 flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-border bg-card/40 p-8 text-center transition-colors hover:border-primary/50 hover:bg-card"
    >
      <span className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground transition-colors group-hover:bg-primary/20 group-hover:text-(--chart-3)">
        <PlusIcon className="size-5" />
      </span>
      <span className="font-heading mt-1 text-lg font-semibold">
        Create a lesson
      </span>
      <span className="max-w-56 text-sm text-muted-foreground">
        Share a skill and earn points when someone books it.
      </span>
    </Link>
  )
}
