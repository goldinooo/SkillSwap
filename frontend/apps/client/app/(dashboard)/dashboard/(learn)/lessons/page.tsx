import Link from "next/link"
import { PlusIcon } from "@phosphor-icons/react/ssr"
import { Button } from "@workspace/ui/components/button"

import { LessonsExplorer } from "./_components/lessons-explorer"
import { MOCK_LESSONS, MOCK_WEEKLY_CAPACITY } from "./_components/lessons-data"
import { WeeklyCapacityCard } from "./_components/weekly-capacity-card"

export default function LessonsPage() {
  return (
    <div className="flex flex-1 flex-col gap-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-bold tracking-tight">My lessons</h1>
          <p className="text-muted-foreground">
            Lessons you offer to other learners. Sessions you attend are in
            History.
          </p>
        </div>
      </div>

      {/* <WeeklyCapacityCard capacity={MOCK_WEEKLY_CAPACITY} /> */}

      {/* <LessonsExplorer lessons={[]} /> */}
      <LessonsExplorer lessons={MOCK_LESSONS} />
    </div>
  )
}
