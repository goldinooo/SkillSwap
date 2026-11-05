"use client"

import { Label, PolarRadiusAxis, RadialBar, RadialBarChart } from "recharts"
import { SparkleIcon } from "@phosphor-icons/react"

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@workspace/ui/components/card"
import {
  type ChartConfig,
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from "@workspace/ui/components/chart"

export interface LevelProgressData {
  currentLevel: number
  currentXp: number
  nextLevelXp: number
  title?: string
  xpToday?: number
}

export const MOCK_LEVEL_PROGRESS: LevelProgressData = {
  currentLevel: 12,
  currentXp: 2850,
  nextLevelXp: 4000,
  title: "Rising Star",
  xpToday: 350,
}

const chartConfig = {
  earned: {
    label: "XP Earned",
    color: "var(--primary)",
  },
  remaining: {
    label: "Remaining",
    color: "var(--muted)",
  },
} satisfies ChartConfig

interface LevelProgressCardProps {
  data?: LevelProgressData
  className?: string
}

export function LevelProgressCard({
  data = MOCK_LEVEL_PROGRESS,
  className,
}: LevelProgressCardProps) {
  const { currentLevel, currentXp, nextLevelXp, xpToday } = data

  const xpRemaining = Math.max(0, nextLevelXp - currentXp)

  // Both values together define the full arc — earned fills proportionally
  const chartData = [{ earned: currentXp, remaining: xpRemaining }]

  return (
    <Card size="sm" className={className ?? "aspect-video overflow-hidden"}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-0">
        <div className="flex items-center gap-1.5">
          <SparkleIcon weight="fill" className="size-4 text-primary" />
          <CardTitle className="text-sm font-semibold tracking-tight">
            XP Progress
          </CardTitle>
        </div>
        {xpToday ? (
          <span className="text-[11px] font-medium text-emerald-600 dark:text-emerald-400">
            +{xpToday} XP today
          </span>
        ) : null}
      </CardHeader>

      <CardContent className="flex flex-col justify-center pt-0 pb-4">
        <ChartContainer config={chartConfig} className="w-full">
          <RadialBarChart
            data={chartData}
            endAngle={180}
            innerRadius="55%"
            outerRadius="95%"
            cy="85%"
          >
            <ChartTooltip
              cursor={false}
              content={<ChartTooltipContent hideLabel />}
            />
            <PolarRadiusAxis tick={false} tickLine={false} axisLine={false}>
              <Label
                content={({ viewBox }) => {
                  if (viewBox && "cx" in viewBox && "cy" in viewBox) {
                    const cx = viewBox.cx ?? 0
                    const cy = viewBox.cy ?? 0
                    return (
                      <text textAnchor="middle">
                        <tspan
                          x={cx}
                          y={cy - 52}
                          fontSize={32}
                          fontWeight="bold"
                          className="fill-foreground"
                        >
                          {currentLevel}
                        </tspan>
                        <tspan
                          x={cx}
                          y={cy - 28}
                          fontSize={9}
                          letterSpacing="0.12em"
                          className="fill-muted-foreground"
                        >
                          LEVEL
                        </tspan>
                        <tspan
                          x={cx}
                          y={cy - 10}
                          fontSize={11}
                          fontWeight="600"
                          className="fill-foreground"
                        >
                          {currentXp.toLocaleString()} /{" "}
                          {nextLevelXp.toLocaleString()}
                        </tspan>
                        <tspan
                          x={cx}
                          y={cy + 6}
                          fontSize={10}
                          className="fill-muted-foreground"
                        >
                          {xpRemaining.toLocaleString()} XP to go
                        </tspan>
                      </text>
                    )
                  }
                }}
              />
            </PolarRadiusAxis>
            {/* remaining renders first (bottom of stack = background arc) */}
            <RadialBar
              dataKey="remaining"
              stackId="a"
              cornerRadius={0}
              fill="var(--color-remaining)"
              className="stroke-transparent stroke-2"
            />
            {/* earned renders on top, its leading edge gets the rounded cap */}
            <RadialBar
              dataKey="earned"
              stackId="a"
              cornerRadius={5}
              fill="var(--color-earned)"
              className="stroke-transparent stroke-2"
            />
          </RadialBarChart>
        </ChartContainer>
      </CardContent>
    </Card>
  )
}
