import type { Metadata } from "next"
import Link from "next/link"
import { CaretLeftIcon } from "@phosphor-icons/react/ssr"

import { CreateLessonForm } from "./_components/create-lesson-form"

export const metadata: Metadata = {
  title: "Create a lesson",
}

export default function NewLessonPage() {
  return (
    <div className="flex flex-1 flex-col gap-6">
      <div className="flex flex-col gap-4">
        <Link
          href="/dashboard/lessons"
          className="inline-flex w-fit items-center gap-1 text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          <CaretLeftIcon className="size-3.5" />
          My lessons
        </Link>
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-bold tracking-tight">Create a lesson</h1>
          <p className="text-muted-foreground">
            Share a skill and earn points when others learn from you.
          </p>
        </div>
      </div>
      <CreateLessonForm />
    </div>
  )
}
