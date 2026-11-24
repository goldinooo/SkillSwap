import Link from "next/link"
import { ArrowRightIcon } from "@phosphor-icons/react/ssr"
import { Card, CardContent, CardHeader } from "@workspace/ui/components/card"

import type { HomeStats } from "./home-data"

function StatLabel({ children }: { children: React.ReactNode }) {
  return (
    <p className="text-[11px] font-medium tracking-[0.18em] text-(--chart-3) uppercase">
      {children}
    </p>
  )
}

function StatLink({ href, children }: { href: string; children: string }) {
  return (
    <Link
      href={href}
      className="inline-flex w-fit items-center gap-1 font-medium text-(--chart-3) hover:underline"
    >
      {children}
      <ArrowRightIcon className="size-3" />
    </Link>
  )
}

function LevelRing({
  level,
  xp,
  nextLevelXp,
}: {
  level: number
  xp: number
  nextLevelXp: number
}) {
  const size = 88
  const stroke = 8
  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  const progress = Math.min(1, xp / nextLevelXp)

  return (
    <div
      role="img"
      aria-label={`Level ${level}, ${xp} of ${nextLevelXp} XP`}
      className="relative size-22 shrink-0"
    >
      <svg viewBox={`0 0 ${size} ${size}`} className="size-full -rotate-90">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          strokeWidth={stroke}
          className="stroke-muted"
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={`${circumference * progress} ${circumference}`}
          className="stroke-accent"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-[8px] tracking-[0.15em] text-muted-foreground uppercase">
          Level
        </span>
        <span className="font-heading text-2xl leading-none font-semibold">
          {level}
        </span>
      </div>
    </div>
  )
}

export function StatCards({ stats }: { stats: HomeStats }) {
  const { points, level, rank } = stats
  const xpRemaining = Math.max(0, level.nextLevelXp - level.xp)

  return (
    <div className="grid gap-4 md:grid-cols-3">
      <Card>
        <CardHeader className="gap-3">
          <StatLabel>Points balance</StatLabel>
          <p className="font-heading text-3xl font-semibold">
            {points.balance}
          </p>
        </CardHeader>
        <CardContent className="gap-2 text-xs">
          <p className="text-muted-foreground">
            <span className="text-(--chart-3)">+{points.earned} earned</span> ·{" "}
            <span>−{points.spent} spent</span> this month
          </p>
          <StatLink href="/dashboard/history">Open history</StatLink>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="gap-3">
          <StatLabel>Your level</StatLabel>
        </CardHeader>
        <CardContent className="flex flex-row items-center gap-4 text-xs">
          <LevelRing
            level={level.current}
            xp={level.xp}
            nextLevelXp={level.nextLevelXp}
          />
          <div className="flex flex-col gap-1.5">
            <p className="text-base font-medium">
              {level.xp.toLocaleString("en-US")} XP
            </p>
            <p className="text-muted-foreground">
              {xpRemaining} XP to Level {level.current + 1}
            </p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="gap-3">
          <StatLabel>Weekly rank</StatLabel>
          <p className="font-heading text-3xl font-semibold">
            #{rank.position}
          </p>
        </CardHeader>
        <CardContent className="gap-2 text-xs">
          <p className="text-muted-foreground">{rank.weeklyXp} XP this week</p>
          <StatLink href="/dashboard/achievements">View leaderboard</StatLink>
        </CardContent>
      </Card>
    </div>
  )
}
