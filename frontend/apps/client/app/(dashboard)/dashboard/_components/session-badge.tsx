import { Badge } from "@workspace/ui/components/badge"
import { cn } from "@workspace/ui/lib/utils"

import type { SessionKind } from "./home-data"

// Teaching earns points (muted purple), learning spends them (solid lime).
const kindStyles: Record<SessionKind, string> = {
  teaching: "bg-(--chart-1)/20 text-(--chart-3)",
  learning: "bg-accent text-accent-foreground",
}

export function SessionBadge({
  kind,
  points,
  showUnit = false,
  className,
}: {
  kind: SessionKind
  points: number
  showUnit?: boolean
  className?: string
}) {
  const label = kind === "teaching" ? "Teaching" : "Learning"
  const sign = kind === "teaching" ? "+" : "−"

  return (
    <Badge className={cn("h-6 px-2.5", kindStyles[kind], className)}>
      {label} {sign}
      {points}
      {showUnit ? " pts" : null}
    </Badge>
  )
}

export function XpBadge({ xp }: { xp: number }) {
  return (
    <Badge className="h-6 bg-accent px-2.5 text-accent-foreground">
      +{xp} XP
    </Badge>
  )
}
