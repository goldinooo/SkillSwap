import Link from "next/link"
import { Button } from "@workspace/ui/components/button"
import { Card } from "@workspace/ui/components/card"
import { cn } from "@workspace/ui/lib/utils"

import type { MOCK_WEEKLY_CAPACITY } from "./lessons-data"

function CardLabel({ children }: { children: React.ReactNode }) {
  return (
    <p className="text-[11px] font-medium tracking-[0.18em] text-(--chart-3) uppercase">
      {children}
    </p>
  )
}

export function WeeklyCapacityCard({
  capacity,
}: {
  capacity: typeof MOCK_WEEKLY_CAPACITY
}) {
  const { used, cap, nextLevel, nextCap, xpToNextLevel } = capacity

  return (
    <Card className="gap-0 overflow-hidden py-0 md:grid md:grid-cols-[1.15fr_1fr]">
      <div className="flex flex-col gap-3 p-6">
        <CardLabel>Weekly capacity</CardLabel>
        <p className="font-heading text-4xl leading-none font-semibold">
          {used}{" "}
          <span className="text-2xl font-normal text-muted-foreground">
            / {cap}
          </span>
        </p>
        <p className="text-sm text-muted-foreground">
          Paid sessions used this week · resets Monday
        </p>
        <div
          className="mt-1 grid gap-2"
          style={{ gridTemplateColumns: `repeat(${cap}, minmax(0, 1fr))` }}
          role="img"
          aria-label={`${used} of ${cap} paid sessions used this week`}
        >
          {Array.from({ length: cap }, (_, index) => (
            <span
              key={index}
              className={cn(
                "h-2 rounded-full",
                index < used ? "bg-primary" : "bg-muted"
              )}
            />
          ))}
        </div>
      </div>

      <div className="flex flex-col items-start gap-3 border-t border-border p-6 md:border-t-0 md:border-l">
        <CardLabel>Raise your cap</CardLabel>
        <p className="font-heading text-lg font-semibold">
          Level {nextLevel} unlocks {nextCap} paid sessions a week.
        </p>
        <p className="text-sm text-muted-foreground">
          {xpToNextLevel} XP to go. Learning a session earns +
          {capacity.learningXp} XP, teaching earns +{capacity.teachingXp} XP.
        </p>
        <Button render={<Link href="/dashboard/discover" />} className="mt-1">
          Find a skill to learn
        </Button>
      </div>
    </Card>
  )
}
