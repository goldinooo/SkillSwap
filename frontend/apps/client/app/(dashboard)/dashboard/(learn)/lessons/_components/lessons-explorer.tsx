"use client"

import { useState } from "react"
import { Button } from "@workspace/ui/components/button"
import { cn } from "@workspace/ui/lib/utils"

import { CreateLessonCard, LessonCard } from "./lesson-card"
import {
  LESSON_FILTERS,
  type Lesson,
  type LessonFilter,
  type LessonStatus,
} from "./lessons-data"
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@workspace/ui/components/empty"
import { PlusIcon } from "@phosphor-icons/react/dist/ssr"
import Link from "next/link"

const filterToStatus: Record<Exclude<LessonFilter, "All">, LessonStatus> = {
  Active: "active",
  Drafts: "draft",
  Paused: "paused",
}

export function LessonsExplorer({ lessons }: { lessons: Lesson[] }) {
  const [filter, setFilter] = useState<LessonFilter>("All")

  if (lessons.length === 0) {
    return <NoLessons />
  }

  const visible =
    filter === "All"
      ? lessons
      : lessons.filter((lesson) => lesson.status === filterToStatus[filter])

  return (
    <>
      <div className="flex flex-wrap gap-2">
        {LESSON_FILTERS.map((item) => {
          const active = filter === item
          return (
            <Button
              key={item}
              variant={active ? "default" : "outline"}
              className={cn(
                "h-9 rounded-full px-4",
                !active && "bg-transparent"
              )}
              onClick={() => setFilter(item)}
            >
              {item}
            </Button>
          )
        })}
      </div>

      {visible.length === 0 ? (
        <p className="rounded-xl border border-dashed p-6 text-muted-foreground">
          No lessons here yet.
        </p>
      ) : (
        <div className="grid items-start gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {filter === "All" && <CreateLessonCard />}
          {visible.map((lesson) => (
            <LessonCard key={lesson.id} lesson={lesson} />
          ))}
        </div>
      )}
    </>
  )
}

function NoLessons() {
  return (
    <Empty>
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <PlusIcon />
        </EmptyMedia>
        <EmptyTitle>Create your first lesson today.</EmptyTitle>
        <EmptyDescription>No lessons found</EmptyDescription>
      </EmptyHeader>
      <EmptyContent>
        <Button
          render={<Link href={"/dashboard/lessons/new"}>Create lesson</Link>}
        />
      </EmptyContent>
    </Empty>
  )
}
